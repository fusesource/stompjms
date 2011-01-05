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
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueSender;

/**
 * Implementation of a Queue Sender
 * 
 */
public class StompJmsQueueSender extends StompJmsMessageProducer implements QueueSender {
    /**
     * Constructor
     * 
     * @param s
     * @param dest
     */
    protected StompJmsQueueSender(StompJmsSession s, StompJmsDestination dest) {
        super(s, dest);
    }

    /**
     * @return the Queue
     * @throws IllegalStateException 
     * @see javax.jms.QueueSender#getQueue()
     */
    public Queue getQueue() throws IllegalStateException {
        checkClosed();
        return (Queue) this.destination;
    }

    /**
     * @param queue
     * @param message
     * @throws JMSException
     * @see javax.jms.QueueSender#send(javax.jms.Queue, javax.jms.Message)
     */
    public void send(Queue queue, Message message) throws JMSException {
        super.send(queue, message);
    }

    /**
     * @param queue
     * @param message
     * @param deliveryMode
     * @param priority
     * @param timeToLive
     * @throws JMSException
     * @see javax.jms.QueueSender#send(javax.jms.Queue, javax.jms.Message, int, int, long)
     */
    public void send(Queue queue, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        super.send(message, deliveryMode, priority, timeToLive);
    }
}
