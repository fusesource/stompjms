/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
