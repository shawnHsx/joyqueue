/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.journalq.broker.producer;

import com.google.common.collect.Lists;
import com.jd.journalq.broker.buffer.Serializer;
import com.jd.journalq.broker.cluster.ClusterManager;
import com.jd.journalq.broker.monitor.BrokerMonitor;
import com.jd.journalq.domain.PartitionGroup;
import com.jd.journalq.domain.QosLevel;
import com.jd.journalq.domain.TopicName;
import com.jd.journalq.exception.JournalqCode;
import com.jd.journalq.exception.JournalqException;
import com.jd.journalq.message.BrokerCommit;
import com.jd.journalq.message.BrokerPrepare;
import com.jd.journalq.message.BrokerRollback;
import com.jd.journalq.network.session.Producer;
import com.jd.journalq.network.session.TransactionId;
import com.jd.journalq.store.PartitionGroupStore;
import com.jd.journalq.store.StoreService;
import com.jd.journalq.store.WriteRequest;
import com.jd.journalq.store.WriteResult;
import com.jd.journalq.store.message.MessageParser;
import com.jd.journalq.store.transaction.TransactionStore;
import com.google.common.base.Strings;
import com.jd.journalq.toolkit.service.Service;
import com.jd.journalq.toolkit.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事务管理器
 *
 * @author lining11
 * Date: 2018/8/17
 */
public class TransactionManager extends Service {

    private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);

    private final AtomicLong sequence = new AtomicLong();

    private ProduceConfig config;
    private StoreService store;
    private ClusterManager clusterManager;
    private BrokerMonitor brokerMonitor;

    private UnCompleteTransactionManager unCompleteTransactionManager;
    private TransactionRecover transactionRecover;
    private TransactionCleaner transactionCleaner;

    public TransactionManager(ProduceConfig config, StoreService store, ClusterManager clusterManager, BrokerMonitor brokerMonitor) {
        this.config = config;
        this.store = store;
        this.clusterManager = clusterManager;
        this.brokerMonitor = brokerMonitor;
    }

    public TransactionId prepare(Producer producer, BrokerPrepare prepare) throws JournalqException {
        if (unCompleteTransactionManager.getTransactionCount(producer.getTopic(), producer.getApp()) > config.getTransactionMaxUncomplete()) {
            logger.warn("too many transactions, topic: {}, app: {}. txId: {}", prepare.getTopic(), prepare.getApp(), prepare.getTxId());
            throw new JournalqException(JournalqCode.FW_TRANSACTION_LIMIT);
        }

        TransactionStore transactionStore = store.getTransactionStore(producer.getTopic());
        if (transactionStore == null) {
            logger.error("transaction store not exist, topic: {}", producer.getTopic());
            throw new JournalqException(JournalqCode.CN_TRANSACTION_NOT_EXISTS);
        }

        int storeId = transactionStore.next();
        TransactionId transactionId = generateTransactionId(prepare, storeId);

        try {
            unCompleteTransactionManager.putTransaction(transactionId);

            ByteBuffer buffer = ByteBuffer.allocate(Serializer.sizeOfBrokerPrepare(prepare));
            Serializer.writeBrokerPrepare(prepare, buffer);
            buffer.flip();

            Future<WriteResult> prepareFuture = transactionStore.asyncWrite(storeId, buffer.slice());
            waitFuture(producer, prepareFuture);
        } catch (Exception e) {
            try {
                transactionStore.remove(storeId);
                unCompleteTransactionManager.removeTransaction(transactionId);
            } catch (Exception ex) {
                logger.error("clear prepare exception, topic: {}, app: {}. txId: {}", prepare.getTopic(), prepare.getApp(), prepare.getTxId(), ex);
            }

            logger.error("write prepare exception, topic: {}, app: {}. txId: {}", prepare.getTopic(), prepare.getApp(), prepare.getTxId(), e);
            throw new JournalqException(JournalqCode.CN_TRANSACTION_PREPARE_ERROR);
        }

        return transactionId;
    }

    protected TransactionId generateTransactionId(BrokerPrepare prepare, int storeId) {
        long now = SystemClock.now();
        String txId = String.format("transactionId_%s_%s_%s_%s", prepare.getTopic(), prepare.getApp(), sequence.getAndIncrement(), now);
        return new TransactionId(prepare.getTopic(), prepare.getApp(), txId, prepare.getQueryId(), storeId, prepare.getTimeout(), now);
    }

    public TransactionId commit(final Producer producer, final BrokerCommit commit) throws JournalqException {
        TransactionStore transactionStore = store.getTransactionStore(producer.getTopic());
        if (transactionStore == null) {
            logger.error("transaction store not exist, topic: {}", producer.getTopic());
            throw new JournalqException(JournalqCode.CN_TRANSACTION_NOT_EXISTS);
        }

        TransactionId transactionId = unCompleteTransactionManager.getTransaction(commit.getTopic(), commit.getApp(), commit.getTxId());
        if (transactionId == null) {
            logger.error("The current tx is not in txManager, topic: {}, app: {}, txId: {}", commit.getTxId(), commit.getApp(), commit.getTxId());
            throw new JournalqException(JournalqCode.CN_TRANSACTION_NOT_EXISTS);
        }

        BrokerPrepare brokerPrepare = null;
        PartitionGroup partitionGroup = null;
        List<WriteRequest> writeRequests = Lists.newLinkedList();
        int messageSize = 0;

        try {
            int index = 0;
            Iterator<ByteBuffer> readIterator = transactionStore.readIterator(transactionId.getStoreId());
            while (readIterator.hasNext()) {
                ByteBuffer byteBuffer = readIterator.next();
                if (index == 0) {
                    brokerPrepare = Serializer.readBrokerPrepare(byteBuffer);
                } else {
                    short currentPartition = dispatchPartition(byteBuffer, (short) 0);
                    PartitionGroup currentPartitionGroup = clusterManager.getPartitionGroup(TopicName.parse(producer.getTopic()), currentPartition);
                    if (partitionGroup == null) {
                        partitionGroup = currentPartitionGroup;
                    }
                    if (currentPartitionGroup == null || currentPartitionGroup.getGroup() != partitionGroup.getGroup()) {
                        throw new JournalqException(JournalqCode.SE_WRITE_FAILED);
                    }
                    writeRequests.add(new WriteRequest(currentPartition, byteBuffer));
                    messageSize += byteBuffer.limit();
                }
                index++;
            }

            try {
                PartitionGroupStore partitionStoreService = store.getStore(commit.getTopic(), partitionGroup.getGroup(), QosLevel.REPLICATION);
                long startTime = SystemClock.now();
                Future<WriteResult> future = partitionStoreService.asyncWrite(writeRequests.toArray(new WriteRequest[0]));
                waitFuture(producer, future);
                long endTime = SystemClock.now();

                for (WriteRequest writeRequest : writeRequests) {
                    brokerMonitor.onPutMessage(producer.getTopic(), producer.getApp(), partitionGroup.getGroup(), writeRequest.getPartition(), 1, messageSize, endTime - startTime);
                }
            } catch (Exception e) {
                logger.warn("write transaction message exception, topic: {}, app: {}, txId: {}", brokerPrepare.getTopic(), brokerPrepare.getApp(), brokerPrepare.getTxId(), e);
                throw new JournalqException(JournalqCode.SE_IO_ERROR);
            }

            unCompleteTransactionManager.removeTransaction(transactionId);
        } catch (Exception e) {
            logger.error("write transaction message exception, topic: {}, app: {}, txId: {}", commit.getTopic(), commit.getApp(), commit.getTxId(), e);
            if (e instanceof JournalqException) {
                throw (JournalqException) e;
            } else {
                throw new JournalqException(JournalqCode.SE_IO_ERROR);
            }
        }
        return transactionId;
    }

    protected short dispatchPartition(ByteBuffer msg, short defaultPartition) {
        short partition = MessageParser.getShort(msg, MessageParser.PARTITION);
        return partition < 0 ? defaultPartition : partition;
    }

    protected void waitFuture(Producer producer, Future<WriteResult> future) throws JournalqException {
        try {
            com.jd.journalq.domain.Producer.ProducerPolicy producerPolicy = clusterManager.tryGetProducerPolicy(TopicName.parse(producer.getTopic()), producer.getApp());
            int configTimeOut = (producerPolicy == null ? 0 : producerPolicy.getTimeOut());
            if (configTimeOut == 0) {
                future.get();
            } else {
                future.get(configTimeOut, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            throw new JournalqException(JournalqCode.SE_DISK_FLUSH_SLOW);
        } catch (ExecutionException | TimeoutException e) {
            throw new JournalqException(JournalqCode.SE_WRITE_TIMEOUT);
        } catch (Exception e) {
            throw new JournalqException(JournalqCode.CN_CONNECTION_TIMEOUT);
        }
    }

    public TransactionId rollback(final Producer producer, final BrokerRollback rollback) throws JournalqException {
        TransactionStore transactionStore = store.getTransactionStore(producer.getTopic());
        if (transactionStore == null) {
            logger.error("transaction store not exist, topic: {}", producer.getTopic());
            throw new JournalqException(JournalqCode.CN_TRANSACTION_NOT_EXISTS);
        }

        TransactionId transactionId = unCompleteTransactionManager.getTransaction(rollback.getTopic(), rollback.getApp(), rollback.getTxId());
        if (transactionId == null) {
            logger.warn("transaction not exist, topic: {}, id: {}", producer.getTopic(), rollback.getTxId());
            throw new JournalqException(JournalqCode.CN_TRANSACTION_NOT_EXISTS);
        }

        transactionStore.remove(transactionId.getStoreId());
        unCompleteTransactionManager.removeTransaction(transactionId);
        return transactionId;
    }

    public Future<WriteResult> putMessage(Producer producer, String txId, ByteBuffer... byteBuffers) throws JournalqException {
        TransactionStore transactionStore = store.getTransactionStore(producer.getTopic());
        if (Strings.isNullOrEmpty(txId)) {
            logger.error("The current message is not a tx message!");
            throw new JournalqException(JournalqCode.CN_UNKNOWN_ERROR);
        }

        TransactionId transactionId = unCompleteTransactionManager.getTransaction(producer.getTopic(), producer.getApp(), txId);
        if (transactionId == null) {
            logger.error("The current tx is not in txManager! txId:{}...", txId);
            throw new JournalqException(JournalqCode.CN_TRANSACTION_NOT_EXISTS);
        }

        return transactionStore.asyncWrite(transactionId.getStoreId(), byteBuffers);
    }

    public List<TransactionId> getFeedback(Producer producer, int count) {
        return unCompleteTransactionManager.getFeedback(producer, count);
    }

    @Override
    protected void validate() throws Exception {
        unCompleteTransactionManager = new UnCompleteTransactionManager(config);
        transactionRecover = new TransactionRecover(config, unCompleteTransactionManager, store);
        transactionCleaner = new TransactionCleaner(config, unCompleteTransactionManager, store);
    }

    @Override
    protected void doStart() throws Exception {
        transactionRecover.recover();
        transactionCleaner.start();
    }

    @Override
    protected void doStop() {
        if (transactionCleaner != null) {
            transactionCleaner.stop();
        }
    }
}
