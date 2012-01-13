/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stomp.jms;

import javax.jms.*;
import javax.jms.IllegalStateException;


/**
 * Jms QueueSession implementation
 */
public class StompJmsQueueSession extends StompJmsTopicSession {
    /**
     * Constructor
     *
     * @param connection
     * @param acknowledgementMode
     */
    protected StompJmsQueueSession(StompJmsConnection connection, int acknowledgementMode, boolean forceAsyncSend) {
        super(connection, acknowledgementMode, forceAsyncSend);
    }

    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        if (destination instanceof Topic) {
            throw new IllegalStateException("Operation not supported by a QueueSession");
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
        if (destination instanceof Topic) {
            throw new IllegalStateException("Operation not supported by a QueueSession");
        }
        return super.createConsumer(destination, messageSelector);
    }

    /**
     * @param destination
     * @param messageSelector
     * @param NoLocal
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createConsumer(javax.jms.Destination, java.lang.String, boolean)
     */
    public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean NoLocal)
            throws JMSException {
        throw new IllegalStateException("Operation not supported by a QueueSession");
    }

    /**
     * @param topic
     * @param name
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createDurableSubscriber(javax.jms.Topic, java.lang.String)
     */
    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        throw new IllegalStateException("Operation not supported by a QueueSession");
    }

    /**
     * @param topic
     * @param name
     * @param messageSelector
     * @param noLocal
     * @return
     * @throws IllegalStateException
     * @throws JMSException
     * @see javax.jms.Session#createDurableSubscriber(javax.jms.Topic, java.lang.String, java.lang.String, boolean)
     */
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal)
            throws IllegalStateException {
        throw new IllegalStateException("Operation not supported by a QueueSession");
    }

    /**
     * @param destination
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createProducer(javax.jms.Destination)
     */
    public MessageProducer createProducer(Destination destination) throws JMSException {
        if (destination instanceof Topic) {
            throw new IllegalStateException("Operation not supported by a QueueSession");
        }
        return super.createProducer(destination);
    }

    /**
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createTemporaryTopic()
     */
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        throw new IllegalStateException("Operation not supported by a QueueSession");
    }

    /**
     * @param topicName
     * @return
     * @throws JMSException
     * @see javax.jms.Session#createTopic(java.lang.String)
     */
    public Topic createTopic(String topicName) throws JMSException {
        throw new IllegalStateException("Operation not supported by a QueueSession");
    }

    /**
     * @param name
     * @throws JMSException
     * @see javax.jms.Session#unsubscribe(java.lang.String)
     */
    public void unsubscribe(String name) throws JMSException {
        throw new IllegalStateException("Operation not supported by a QueueSession");
    }

    /**
     * @param topic
     * @return
     * @throws JMSException
     * @see javax.jms.TopicSession#createPublisher(javax.jms.Topic)
     */
    public TopicPublisher createPublisher(Topic topic) throws JMSException {
        throw new IllegalStateException("Operation not supported by a QueueSession");
    }

    /**
     * @param topic
     * @return
     * @throws JMSException
     * @see javax.jms.TopicSession#createSubscriber(javax.jms.Topic)
     */
    public TopicSubscriber createSubscriber(Topic topic) throws JMSException {
        throw new IllegalStateException("Operation not supported by a QueueSession");
    }

    /**
     * @param topic
     * @param messageSelector
     * @param noLocal
     * @return
     * @throws JMSException
     * @see javax.jms.TopicSession#createSubscriber(javax.jms.Topic, java.lang.String, boolean)
     */
    public TopicSubscriber createSubscriber(Topic topic, String messageSelector, boolean noLocal) throws JMSException {
        throw new IllegalStateException("Operation not supported by a QueueSession");
    }
}
