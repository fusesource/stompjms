/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.stompjms;

import javax.jms.TemporaryQueue;


/**
 * TemporaryQueue
 */
public class StompJmsTempQueue extends StompJmsDestination implements TemporaryQueue {

    /**
     * Constructor
     *
     * @param name
     */
    public StompJmsTempQueue(String name) {
        super(name);
        this.topic = false;
    }

    protected String getType() {
        return StompJmsDestination.TEMP_QUEUE_QUALIFED_PREFIX;
    }

    /**
     * @see javax.jms.TemporaryQueue#delete()
     */
    public void delete() {
        // TODO Auto-generated method stub

    }

    /**
     * @return name
     * @see javax.jms.Queue#getQueueName()
     */
    public String getQueueName() {
        return getPhysicalName();
    }
}
