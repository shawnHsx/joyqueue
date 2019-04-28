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
package com.jd.journalq.network.command;

import com.jd.journalq.network.transport.command.JMQPayload;

import java.util.List;
import java.util.Map;

/**
 * FetchIndex
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/12/13
 */
public class FetchIndex extends JMQPayload {

    private Map<String, List<Short>> partitions;
    private String app;

    @Override
    public int type() {
        return JournalqCommandType.FETCH_INDEX.getCode();
    }

    public void setPartitions(Map<String, List<Short>> partitions) {
        this.partitions = partitions;
    }

    public Map<String, List<Short>> getPartitions() {
        return partitions;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getApp() {
        return app;
    }
}