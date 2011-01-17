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
import javax.jms.*;

/**
 * Implementation of a TopicPublisher
 */
public class StompJmsTopicPublisher extends StompJmsMessageProducer implements TopicPublisher {

    /**
     * Constructor
     *
     * @param s
     * @param destination
     */
    protected StompJmsTopicPublisher(StompJmsSession s, StompJmsDestination destination) {
        super(s, destination);
    }

    /**
     * @return the Topic
     * @throws IllegalStateException
     * @see javax.jms.TopicPublisher#getTopic()
     */
    public Topic getTopic() throws IllegalStateException {
        checkClosed();
        return (Topic) this.destination;
    }

    /**
     * @param message
     * @throws JMSException
     * @see javax.jms.TopicPublisher#publish(javax.jms.Message)
     */
    public void publish(Message message) throws JMSException {
        super.send(message);

    }

    /**
     * @param topic
     * @param message
     * @throws JMSException
     * @see javax.jms.TopicPublisher#publish(javax.jms.Topic, javax.jms.Message)
     */
    public void publish(Topic topic, Message message) throws JMSException {
        super.send(topic, message);

    }

    /**
     * @param message
     * @param deliveryMode
     * @param priority
     * @param timeToLive
     * @throws JMSException
     * @see javax.jms.TopicPublisher#publish(javax.jms.Message, int, int, long)
     */
    public void publish(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        super.send(message, deliveryMode, priority, timeToLive);

    }

    /**
     * @param topic
     * @param message
     * @param deliveryMode
     * @param priority
     * @param timeToLive
     * @throws JMSException
     * @see javax.jms.TopicPublisher#publish(javax.jms.Topic, javax.jms.Message, int, int, long)
     */
    public void publish(Topic topic, Message message, int deliveryMode, int priority, long timeToLive)
            throws JMSException {
        super.send(topic, message, deliveryMode, priority, timeToLive);

    }


}
