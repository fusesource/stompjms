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

import javax.jms.IllegalStateException;
import javax.jms.*;

/**
 * Implementation of a Queue Sender
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
