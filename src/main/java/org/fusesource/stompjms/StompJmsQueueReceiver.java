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

import javax.jms.IllegalStateException;
import javax.jms.Queue;
import javax.jms.QueueReceiver;

/**
 * Implementation of a Jms QueueReceiver
 *
 */
public class StompJmsQueueReceiver extends StompJmsMessageConsumer implements QueueReceiver {

    /**
     * Constructor
     * @param s
     */
    protected StompJmsQueueReceiver(String id,StompJmsSession s,StompJmsDestination d,String selector) {
        super(id,s,d,selector);
    }

    /**
     * @return the Queue
     * @throws IllegalStateException 
     * @see javax.jms.QueueReceiver#getQueue()
     */
    public Queue getQueue() throws IllegalStateException {
        checkClosed();
       return (Queue) this.destination;
    }
}
