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

import org.fusesource.stompjms.message.StompJmsMessageTransformation;

import javax.jms.*;
import javax.jms.IllegalStateException;

/**
 * Implementation of a Jms MessageProducer
 */
public class StompJmsMessageProducer implements MessageProducer {
    protected final StompJmsSession session;
    protected StompJmsDestination destination;
    protected final boolean flexibleDestination;
    protected int deliveryMode = DeliveryMode.PERSISTENT;
    protected int priority = Message.DEFAULT_PRIORITY;
    protected long timeToLive = Message.DEFAULT_TIME_TO_LIVE;
    protected boolean closed;
    protected boolean disableMessageId;
    protected boolean disableTimestamp;

    protected StompJmsMessageProducer(StompJmsSession s, StompJmsDestination dest) {
        this.session = s;
        this.destination = dest;
        this.flexibleDestination = dest == null;
    }

    /**
     * Close the producer
     *
     * @see javax.jms.MessageProducer#close()
     */
    public void close() {
        this.closed = true;
        this.session.remove(this);
    }

    /**
     * @return the delivery mode
     * @throws JMSException
     * @see javax.jms.MessageProducer#getDeliveryMode()
     */
    public int getDeliveryMode() throws JMSException {
        checkClosed();
        return this.deliveryMode;
    }

    /**
     * @return the destination
     * @throws JMSException
     * @see javax.jms.MessageProducer#getDestination()
     */
    public Destination getDestination() throws JMSException {
        checkClosed();
        return this.destination;
    }

    /**
     * @return true if disableIds is set
     * @throws JMSException
     * @see javax.jms.MessageProducer#getDisableMessageID()
     */
    public boolean getDisableMessageID() throws JMSException {
        checkClosed();
        return this.disableMessageId;
    }

    /**
     * @return true if disable timestamp is set
     * @throws JMSException
     * @see javax.jms.MessageProducer#getDisableMessageTimestamp()
     */
    public boolean getDisableMessageTimestamp() throws JMSException {
        checkClosed();
        return this.disableTimestamp;
    }

    /**
     * @return the priority
     * @throws JMSException
     * @see javax.jms.MessageProducer#getPriority()
     */
    public int getPriority() throws JMSException {
        checkClosed();
        return this.priority;
    }

    /**
     * @return timeToLive
     * @throws JMSException
     * @see javax.jms.MessageProducer#getTimeToLive()
     */
    public long getTimeToLive() throws JMSException {
        checkClosed();
        return this.timeToLive;
    }

    /**
     * @param message
     * @throws JMSException
     * @see javax.jms.MessageProducer#send(javax.jms.Message)
     */
    public void send(Message message) throws JMSException {
        send(this.destination, message, this.deliveryMode, this.priority, this.timeToLive);
    }

    /**
     * @param destination
     * @param message
     * @throws JMSException
     * @see javax.jms.MessageProducer#send(javax.jms.Destination, javax.jms.Message)
     */
    public void send(Destination destination, Message message) throws JMSException {
        send(destination, message, this.deliveryMode, this.priority, this.timeToLive);
    }

    /**
     * @param message
     * @param deliveryMode
     * @param priority
     * @param timeToLive
     * @throws JMSException
     * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
     */
    public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        send(this.destination, message, deliveryMode, priority, timeToLive);
    }

    /**
     * @param destination
     * @param message
     * @param deliveryMode
     * @param priority
     * @param timeToLive
     * @throws JMSException
     * @see javax.jms.MessageProducer#send(javax.jms.Destination, javax.jms.Message, int, int, long)
     */
    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive)
            throws JMSException {
        setDestination(destination);
        this.session.send(destination, message, deliveryMode, priority, timeToLive);
    }

    /**
     * @param deliveryMode
     * @throws JMSException
     * @see javax.jms.MessageProducer#setDeliveryMode(int)
     */
    public void setDeliveryMode(int deliveryMode) throws JMSException {
        checkClosed();
        this.deliveryMode = deliveryMode;
    }

    /**
     * @param value
     * @throws JMSException
     * @see javax.jms.MessageProducer#setDisableMessageID(boolean)
     */
    public void setDisableMessageID(boolean value) throws JMSException {
        checkClosed();
        this.disableMessageId = value;
    }

    /**
     * @param value
     * @throws JMSException
     * @see javax.jms.MessageProducer#setDisableMessageTimestamp(boolean)
     */
    public void setDisableMessageTimestamp(boolean value) throws JMSException {
        checkClosed();
        this.disableTimestamp = value;
    }

    /**
     * @param defaultPriority
     * @throws JMSException
     * @see javax.jms.MessageProducer#setPriority(int)
     */
    public void setPriority(int defaultPriority) throws JMSException {
        checkClosed();
        this.priority = defaultPriority;
    }

    /**
     * @param timeToLive
     * @throws JMSException
     * @see javax.jms.MessageProducer#setTimeToLive(long)
     */
    public void setTimeToLive(long timeToLive) throws JMSException {
        checkClosed();
        this.timeToLive = timeToLive;
    }

    /**
     * @param destination the destination to set
     * @throws JMSException
     * @throws InvalidDestinationException
     */
    public void setDestination(Destination destination) throws JMSException {
        if (destination == null) {
            throw new InvalidDestinationException("Don't understand null destinations");
        }
        if (!this.flexibleDestination && !destination.equals(this.destination)) {
            throw new UnsupportedOperationException("This producer can only send messages to: "
                    + this.destination.getPhysicalName());
        }
        this.destination = StompJmsMessageTransformation.transformDestination(session.connection, destination);
    }

    protected void checkClosed() throws IllegalStateException {
        if (this.closed) {
            throw new IllegalStateException("The MessageProducer is closed");
        }
    }
}
