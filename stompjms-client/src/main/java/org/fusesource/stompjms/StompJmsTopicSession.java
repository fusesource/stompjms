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

import org.fusesource.stompjms.channel.StompChannel;

import javax.jms.*;
import javax.jms.IllegalStateException;

/**
 * Implementation of a TopicSession
 */
public class StompJmsTopicSession extends StompJmsSession {
    /**
     * Constructor
     *
     * @param connection
     * @param acknowledgementMode
     */
    protected StompJmsTopicSession(StompJmsConnection connection, StompChannel channel, int acknowledgementMode) {
        super(connection, channel, acknowledgementMode);
    }

    /**
     * @param queue
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createBrowser(javax.jms.Queue)
     */
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        throw new IllegalStateException("Operation not supported by a TopicSession");
    }

    /**
     * @param queue
     * @param messageSelector
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createBrowser(javax.jms.Queue, java.lang.String)
     */
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        throw new IllegalStateException("Operation not supported by a TopicSession");
    }

    /**
     * @param destination
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createConsumer(javax.jms.Destination)
     */
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        if (destination instanceof Queue) {
            throw new IllegalStateException("Operation not supported by a TopicSession");
        }
        return super.createConsumer(destination);
    }

    /**
     * @param destination
     * @param messageSelector
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createConsumer(javax.jms.Destination, java.lang.String)
     */
    public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
        if (destination instanceof Queue) {
            throw new IllegalStateException("Operation not supported by a TopicSession");
        }
        return super.createConsumer(destination, messageSelector);
    }

    /**
     * @param destination
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createProducer(javax.jms.Destination)
     */
    public MessageProducer createProducer(Destination destination) throws JMSException {
        if (destination instanceof Queue) {
            throw new IllegalStateException("Operation not supported by a TopicSession");
        }
        return super.createProducer(destination);
    }

    /**
     * @param queueName
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createQueue(java.lang.String)
     */
    public Queue createQueue(String queueName) throws JMSException {
        throw new IllegalStateException("Operation not supported by a TopicSession");
    }

    /**
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createTemporaryQueue()
     */
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        throw new IllegalStateException("Operation not supported by a TopicSession");
    }

    /**
     * @param queue
     * @return
     * @throws JMSException
     * @see javax.jms.QueueSession#createReceiver(javax.jms.Queue)
     */
    public QueueReceiver createReceiver(Queue queue) throws JMSException {
        throw new IllegalStateException("Operation not supported by a TopicSession");
    }

    /**
     * @param queue
     * @param messageSelector
     * @return
     * @throws JMSException
     * @see javax.jms.QueueSession#createReceiver(javax.jms.Queue, java.lang.String)
     */
    public QueueReceiver createReceiver(Queue queue, String messageSelector) throws JMSException {
        throw new IllegalStateException("Operation not supported by a TopicSession");
    }

    /**
     * @param queue
     * @return
     * @throws JMSException
     * @see javax.jms.QueueSession#createSender(javax.jms.Queue)
     */
    public QueueSender createSender(Queue queue) throws JMSException {
        throw new IllegalStateException("Operation not supported by a TopicSession");
    }
}
