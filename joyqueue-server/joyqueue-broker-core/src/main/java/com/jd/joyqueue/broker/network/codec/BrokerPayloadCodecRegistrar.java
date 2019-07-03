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
package com.jd.joyqueue.broker.network.codec;

import com.jd.joyqueue.broker.election.network.codec.AppendEntriesRequestDecoder;
import com.jd.joyqueue.broker.election.network.codec.AppendEntriesRequestEncoder;
import com.jd.joyqueue.broker.election.network.codec.AppendEntriesResponseDecoder;
import com.jd.joyqueue.broker.election.network.codec.AppendEntriesResponseEncoder;
import com.jd.joyqueue.broker.election.network.codec.ReplicateConsumePosRequestDecoder;
import com.jd.joyqueue.broker.election.network.codec.ReplicateConsumePosRequestEncoder;
import com.jd.joyqueue.broker.election.network.codec.ReplicateConsumePosResponseDecoder;
import com.jd.joyqueue.broker.election.network.codec.ReplicateConsumePosResponseEncoder;
import com.jd.joyqueue.broker.election.network.codec.TimeoutNowRequestDecoder;
import com.jd.joyqueue.broker.election.network.codec.TimeoutNowRequestEncoder;
import com.jd.joyqueue.broker.election.network.codec.TimeoutNowResponseDecoder;
import com.jd.joyqueue.broker.election.network.codec.TimeoutNowResponseEncoder;
import com.jd.joyqueue.broker.election.network.codec.VoteRequestDecoder;
import com.jd.joyqueue.broker.election.network.codec.VoteRequestEncoder;
import com.jd.joyqueue.broker.election.network.codec.VoteResponseDecoder;
import com.jd.joyqueue.broker.election.network.codec.VoteResponseEncoder;
import com.jd.joyqueue.broker.index.network.codec.IndexQueryRequestDecoder;
import com.jd.joyqueue.broker.index.network.codec.IndexQueryRequestEncoder;
import com.jd.joyqueue.broker.index.network.codec.IndexQueryResponseDecoder;
import com.jd.joyqueue.broker.index.network.codec.IndexQueryResponseEncoder;
import com.jd.joyqueue.broker.index.network.codec.IndexStoreRequestDecoder;
import com.jd.joyqueue.broker.index.network.codec.IndexStoreRequestEncoder;
import com.jd.joyqueue.broker.index.network.codec.IndexStoreResponseDecoder;
import com.jd.joyqueue.broker.index.network.codec.IndexStoreResponseEncoder;
import com.jd.joyqueue.broker.producer.transaction.codec.TransactionCommitRequestCodec;
import com.jd.joyqueue.broker.producer.transaction.codec.TransactionRollbackRequestCodec;
import com.jd.joyqueue.network.codec.BooleanAckCodec;
import com.jd.joyqueue.network.transport.codec.PayloadCodecFactory;
import com.jd.joyqueue.nsr.network.codec.OperatePartitionGroupCodec;
import com.jd.joyqueue.server.retry.remote.command.codec.GetRetryAckCodec;
import com.jd.joyqueue.server.retry.remote.command.codec.GetRetryCodec;
import com.jd.joyqueue.server.retry.remote.command.codec.GetRetryCountAckCodec;
import com.jd.joyqueue.server.retry.remote.command.codec.GetRetryCountCodec;
import com.jd.joyqueue.server.retry.remote.command.codec.PutRetryCodec;
import com.jd.joyqueue.server.retry.remote.command.codec.UpdateRetryCodec;

/**
 * BrokerPayloadCodecRegistrar
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/21
 */
// 使用BrokerPayloadCodec接口通过spi方式注册
@Deprecated
public class BrokerPayloadCodecRegistrar {

    public static PayloadCodecFactory register(PayloadCodecFactory payloadCodecFactory) {
        // boolean ack command codec
        payloadCodecFactory.register(new BooleanAckCodec());

        // retry message command codec
        payloadCodecFactory.register(new GetRetryCodec());
        payloadCodecFactory.register(new GetRetryAckCodec());
        payloadCodecFactory.register(new GetRetryCountCodec());
        payloadCodecFactory.register(new GetRetryCountAckCodec());
        payloadCodecFactory.register(new PutRetryCodec());
        payloadCodecFactory.register(new UpdateRetryCodec());

        // raft election command codec
        payloadCodecFactory.register(new VoteRequestDecoder());
        payloadCodecFactory.register(new VoteRequestEncoder());
        payloadCodecFactory.register(new VoteResponseDecoder());
        payloadCodecFactory.register(new VoteResponseEncoder());
        payloadCodecFactory.register(new TimeoutNowRequestDecoder());
        payloadCodecFactory.register(new TimeoutNowRequestEncoder());
        payloadCodecFactory.register(new TimeoutNowResponseDecoder());
        payloadCodecFactory.register(new TimeoutNowResponseEncoder());
        payloadCodecFactory.register(new AppendEntriesRequestDecoder());
        payloadCodecFactory.register(new AppendEntriesRequestEncoder());
        payloadCodecFactory.register(new AppendEntriesResponseDecoder());
        payloadCodecFactory.register(new AppendEntriesResponseEncoder());

        // index manage command codec
        payloadCodecFactory.register(new IndexQueryRequestDecoder());
        payloadCodecFactory.register(new IndexQueryRequestEncoder());
        payloadCodecFactory.register(new IndexQueryResponseDecoder());
        payloadCodecFactory.register(new IndexQueryResponseEncoder());
        payloadCodecFactory.register(new IndexStoreRequestDecoder());
        payloadCodecFactory.register(new IndexStoreRequestEncoder());
        payloadCodecFactory.register(new IndexStoreResponseDecoder());
        payloadCodecFactory.register(new IndexStoreResponseEncoder());

        // replication related command
        payloadCodecFactory.register(new ReplicateConsumePosRequestDecoder());
        payloadCodecFactory.register(new ReplicateConsumePosRequestEncoder());
        payloadCodecFactory.register(new ReplicateConsumePosResponseDecoder());
        payloadCodecFactory.register(new ReplicateConsumePosResponseEncoder());
        //nsr
        payloadCodecFactory.register(new OperatePartitionGroupCodec());

        // transaction
        payloadCodecFactory.register(new TransactionCommitRequestCodec());
        payloadCodecFactory.register(new TransactionRollbackRequestCodec());

        return payloadCodecFactory;
    }
}