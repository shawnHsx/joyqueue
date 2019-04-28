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
package com.jd.journalq.network.transport.command;

import com.jd.journalq.network.transport.codec.JMQHeader;

/**
 * journalq消息体
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/21
 */
public abstract class JMQPayload implements Payload, Type, HeaderAware {

    private JMQHeader header;

    /**
     * 校验
     */
    public void validate() {
        //Do nothing
    }

    @Override
    public void setHeader(Header header) {
        if (header instanceof JMQHeader) {
            this.header = (JMQHeader) header;
        }
    }

    public JMQHeader getHeader() {
        return header;
    }
}