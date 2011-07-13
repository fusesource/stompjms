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

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.ByteArrayOutputStream;
import org.fusesource.stompjms.channel.StompChannel;
import org.fusesource.stompjms.message.*;

import javax.jms.*;
import javax.jms.IllegalStateException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.fusesource.hawtbuf.Buffer.ascii;

/**
 * JMS Session implementation
 */
public class StompJmsSession implements Session, QueueSession, TopicSession, StompJmsMessageListener {
    private long nextMessageSwquence = 0;
    private final StompJmsConnection connection;
    private final StompChannel channel;
    private final int acknowledgementMode;
    private final List<MessageProducer> producers = new CopyOnWriteArrayList<MessageProducer>();
    private final Map<AsciiBuffer, StompJmsMessageConsumer> consumers = new ConcurrentHashMap<AsciiBuffer, StompJmsMessageConsumer>();
    private MessageListener messageListener;
    private AtomicBoolean closed = new AtomicBoolean();
    private AtomicBoolean started = new AtomicBoolean();
    private AsciiBuffer currentTransactionId;
    private LinkedBlockingQueue<StompJmsMessage> stoppedMessages = new LinkedBlockingQueue<StompJmsMessage>(10000);

    /**
     * Constructor
     *
     * @param connection
     * @param acknowledgementMode
     */
    protected StompJmsSession(StompJmsConnection connection, StompChannel channel, int acknowledgementMode) {
        this.connection = connection;
        this.channel = channel;
        this.acknowledgementMode = acknowledgementMode;
        this.channel.setListener(this);
    }

    /**
     * @throws JMSException
     * @see javax.jms.Session#close()
     */
    public void close() throws JMSException {
        if (closed.compareAndSet(false, true)) {

            this.connection.removeSession(this);
            for (StompJmsMessageConsumer c : this.consumers.values()) {
                if (this.channel.isStarted()) {
                    this.channel.unsubscribe(c.getDestination(), c.getId(), false, false);
                }
                c.close();
            }
        }
        this.consumers.clear();
        this.connection.removeSession(this);
    }

    /**
     * @throws JMSException
     * @see javax.jms.Session#commit()
     */
    public void commit() throws JMSException {
        checkClosed();
        if (!getTransacted()) {
            throw new javax.jms.IllegalStateException("Not a transacted session");
        }
        this.channel.commitTransaction();
        this.currentTransactionId = this.channel.startTransaction();
    }

    /**
     * @param destination
     * @return QueueBrowser
     * @throws JMSException
     * @see javax.jms.Session#createBrowser(javax.jms.Queue)
     */
    public QueueBrowser createBrowser(Queue destination) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(destination);
        StompJmsQueueBrowser result = new StompJmsQueueBrowser(this, channel.nextId(), dest, "");
        return result;
    }

    /**
     * @param queue
     * @param messageSelector
     * @return QueueBrowser
     * @throws JMSException
     * @see javax.jms.Session#createBrowser(javax.jms.Queue, java.lang.String)
     */
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        checkClosed();
        throw new JMSException("Not supported by STOMP protocol");
    }

    /**
     * @return BytesMessage
     * @throws IllegalStateException
     * @see javax.jms.Session#createBytesMessage()
     */
    public BytesMessage createBytesMessage() throws IllegalStateException {
        checkClosed();
        return new StompJmsBytesMessage();
    }

    /**
     * @param destination
     * @return a MessageConsumer
     * @throws JMSException
     * @see javax.jms.Session#createConsumer(javax.jms.Destination)
     */
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(destination);
        StompJmsMessageConsumer result = new StompJmsMessageConsumer(channel.nextId(), this, dest, "");
        result.init();
        return result;
    }

    /**
     * @param destination
     * @param messageSelector
     * @return MessageConsumer
     * @throws JMSException
     * @see javax.jms.Session#createConsumer(javax.jms.Destination,
     *      java.lang.String)
     */
    public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(destination);
        StompJmsMessageConsumer result = new StompJmsMessageConsumer(channel.nextId(), this, dest,
                messageSelector);
        result.init();
        return result;
    }

    /**
     * @param destination
     * @param messageSelector
     * @param NoLocal
     * @return the MessageConsumer
     * @throws JMSException
     * @see javax.jms.Session#createConsumer(javax.jms.Destination,
     *      java.lang.String, boolean)
     */
    public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean NoLocal)
            throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(destination);
        StompJmsTopicSubscriber result = new StompJmsTopicSubscriber(channel.nextId(), this, dest, NoLocal,
                messageSelector);
        result.init();
        return result;
    }

    /**
     * @param topic
     * @param name
     * @return a TopicSubscriber
     * @throws JMSException
     * @see javax.jms.Session#createDurableSubscriber(javax.jms.Topic,
     *      java.lang.String)
     */
    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        checkClosed();
        AsciiBuffer id = StompChannel.encodeHeader(this.connection.getClientID() + ":" + name);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(topic);
        StompJmsTopicSubscriber result = new StompJmsDurableTopicSubscriber(id, this, dest, false, "");
        result.init();
        return result;
    }

    /**
     * @param topic
     * @param name
     * @param messageSelector
     * @param noLocal
     * @return TopicSubscriber
     * @throws JMSException
     * @see javax.jms.Session#createDurableSubscriber(javax.jms.Topic,
     *      java.lang.String, java.lang.String, boolean)
     */
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal)
            throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(topic);
        StompJmsTopicSubscriber result = new StompJmsDurableTopicSubscriber(channel.nextId(), this, dest, noLocal,messageSelector);
        result.init();
        return result;
    }

    /**
     * @return MapMessage
     * @throws IllegalStateException
     * @see javax.jms.Session#createMapMessage()
     */
    public MapMessage createMapMessage() throws IllegalStateException {
        checkClosed();
        return new StompJmsMapMessage();
    }

    /**
     * @return Message
     * @throws IllegalStateException
     * @see javax.jms.Session#createMessage()
     */
    public Message createMessage() throws IllegalStateException {
        checkClosed();
        return new StompJmsMessage();
    }

    /**
     * @return ObjectMessage
     * @throws IllegalStateException
     * @see javax.jms.Session#createObjectMessage()
     */
    public ObjectMessage createObjectMessage() throws IllegalStateException {
        checkClosed();
        return new StompJmsObjectMessage();
    }

    /**
     * @param object
     * @return ObjectMessage
     * @throws JMSException
     * @see javax.jms.Session#createObjectMessage(java.io.Serializable)
     */
    public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
        checkClosed();
        ObjectMessage result = createObjectMessage();
        result.setObject(object);
        return result;
    }

    /**
     * @param destination
     * @return MessageProducer
     * @throws JMSException
     * @see javax.jms.Session#createProducer(javax.jms.Destination)
     */
    public MessageProducer createProducer(Destination destination) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(destination);
        StompJmsMessageProducer result = new StompJmsMessageProducer(this, dest);
        add(result);
        return result;
    }

    /**
     * @param queueName
     * @return Queue
     * @throws JMSException
     * @see javax.jms.Session#createQueue(java.lang.String)
     */
    public Queue createQueue(String queueName) throws JMSException {
        checkClosed();
        return new StompJmsQueue(queueName);
    }

    /**
     * @return StreamMessage
     * @throws JMSException
     * @see javax.jms.Session#createStreamMessage()
     */
    public StreamMessage createStreamMessage() throws JMSException {
        checkClosed();
        return new StompJmsStreamMessage();
    }

    /**
     * @return TemporaryQueue
     * @throws JMSException
     * @see javax.jms.Session#createTemporaryQueue()
     */
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        checkClosed();
        return new StompJmsTempQueue(UUID.randomUUID().toString());
    }

    /**
     * @return TemporaryTopic
     * @throws JMSException
     * @see javax.jms.Session#createTemporaryTopic()
     */
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        checkClosed();
        return new StompJmsTempTopic(UUID.randomUUID().toString());
    }

    /**
     * @return TextMessage
     * @throws JMSException
     * @see javax.jms.Session#createTextMessage()
     */
    public TextMessage createTextMessage() throws JMSException {
        checkClosed();
        return new StompJmsTextMessage();
    }

    /**
     * @param text
     * @return TextMessage
     * @throws JMSException
     * @see javax.jms.Session#createTextMessage(java.lang.String)
     */
    public TextMessage createTextMessage(String text) throws JMSException {
        checkClosed();
        StompJmsTextMessage result = new StompJmsTextMessage();
        result.setText(text);
        return result;
    }

    /**
     * @param topicName
     * @return Topic
     * @throws JMSException
     * @see javax.jms.Session#createTopic(java.lang.String)
     */
    public Topic createTopic(String topicName) throws JMSException {
        checkClosed();
        return new StompJmsTopic(topicName);
    }

    /**
     * @return acknowledgeMode
     * @throws JMSException
     * @see javax.jms.Session#getAcknowledgeMode()
     */
    public int getAcknowledgeMode() throws JMSException {
        checkClosed();
        return this.acknowledgementMode;
    }

    /**
     * @return the Sesion messageListener
     * @throws JMSException
     * @see javax.jms.Session#getMessageListener()
     */
    public MessageListener getMessageListener() throws JMSException {
        checkClosed();
        return this.messageListener;
    }

    /**
     * @return true if transacted
     * @throws JMSException
     * @see javax.jms.Session#getTransacted()
     */
    public boolean getTransacted() throws JMSException {
        checkClosed();
        return this.acknowledgementMode == Session.SESSION_TRANSACTED;
    }

    /**
     * @throws JMSException
     * @see javax.jms.Session#recover()
     */
    public void recover() throws JMSException {
        checkClosed();
    }

    /**
     * @throws JMSException
     * @see javax.jms.Session#rollback()
     */
    public void rollback() throws JMSException {
        checkClosed();
        if (!getTransacted()) {
            throw new javax.jms.IllegalStateException("Not a transacted session");
        }
        for (StompJmsMessageConsumer c : consumers.values()) {
            c.rollback(this.currentTransactionId);
        }
        this.channel.rollbackTransaction();
        this.currentTransactionId = this.channel.startTransaction();
    }

    /**
     * @see javax.jms.Session#run()
     */
    public void run() {
        // TODO Auto-generated method stub
    }

    /**
     * @param listener
     * @throws JMSException
     * @see javax.jms.Session#setMessageListener(javax.jms.MessageListener)
     */
    public void setMessageListener(MessageListener listener) throws JMSException {
        checkClosed();
        this.messageListener = listener;
    }

    /**
     * @param name
     * @throws JMSException
     * @see javax.jms.Session#unsubscribe(java.lang.String)
     */
    public void unsubscribe(String name) throws JMSException {
        checkClosed();
        AsciiBuffer id = StompChannel.encodeHeader(this.connection.getClientID() + ":" + name);
        StompJmsMessageConsumer consumer = this.consumers.remove(id);
        if (consumer != null) {
            consumer.close();
        }
        this.channel.unsubscribe(null, id, true, false);

    }

    /**
     * @param queue
     * @return QueueRecevier
     * @throws JMSException
     * @see javax.jms.QueueSession#createReceiver(javax.jms.Queue)
     */
    public QueueReceiver createReceiver(Queue queue) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(queue);
        StompJmsQueueReceiver result = new StompJmsQueueReceiver(channel.nextId(), this, dest, "");
        result.init();
        return result;
    }

    /**
     * @param queue
     * @param messageSelector
     * @return QueueReceiver
     * @throws JMSException
     * @see javax.jms.QueueSession#createReceiver(javax.jms.Queue,
     *      java.lang.String)
     */
    public QueueReceiver createReceiver(Queue queue, String messageSelector) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(queue);
        StompJmsQueueReceiver result = new StompJmsQueueReceiver(channel.nextId(), this, dest, messageSelector);
        result.init();
        return result;
    }

    /**
     * @param queue
     * @return QueueSender
     * @throws JMSException
     * @see javax.jms.QueueSession#createSender(javax.jms.Queue)
     */
    public QueueSender createSender(Queue queue) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(queue);
        StompJmsQueueSender result = new StompJmsQueueSender(this, dest);
        return result;
    }

    /**
     * @param topic
     * @return TopicPublisher
     * @throws JMSException
     * @see javax.jms.TopicSession#createPublisher(javax.jms.Topic)
     */
    public TopicPublisher createPublisher(Topic topic) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(topic);
        StompJmsTopicPublisher result = new StompJmsTopicPublisher(this, dest);
        add(result);
        return result;
    }

    /**
     * @param topic
     * @return TopicSubscriber
     * @throws JMSException
     * @see javax.jms.TopicSession#createSubscriber(javax.jms.Topic)
     */
    public TopicSubscriber createSubscriber(Topic topic) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(topic);
        StompJmsTopicSubscriber result = new StompJmsTopicSubscriber(channel.nextId(), this, dest, false, "");
        result.init();
        return result;
    }

    /**
     * @param topic
     * @param messageSelector
     * @param noLocal
     * @return TopicSubscriber
     * @throws JMSException
     * @see javax.jms.TopicSession#createSubscriber(javax.jms.Topic,
     *      java.lang.String, boolean)
     */
    public TopicSubscriber createSubscriber(Topic topic, String messageSelector, boolean noLocal) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(topic);
        StompJmsTopicSubscriber result = new StompJmsTopicSubscriber(channel.nextId(), this, dest, noLocal, messageSelector);
        return result;
    }

    protected void add(StompJmsMessageConsumer consumer, boolean persistent, boolean browser) throws JMSException {
        this.consumers.put(consumer.getId(), consumer);
        this.channel.subscribe(consumer.getDestination(), consumer.getId(), StompChannel.encodeHeader(consumer.getMessageSelector()),
                this.acknowledgementMode == Session.CLIENT_ACKNOWLEDGE, persistent, browser);
        if (started.get()) {
            consumer.start();
        }
    }

    protected void remove(StompJmsMessageConsumer consumer) throws JMSException {
        this.consumers.remove(consumer.getId());
    }

    protected void add(MessageProducer producer) {
        this.producers.add(producer);
    }

    protected void remove(MessageProducer producer) {
        this.producers.remove(producer);
    }

    protected void onException(Exception ex) {
        this.connection.onException(ex);
    }


    protected void onException(JMSException ex) {
        this.connection.onException(ex);
    }

    protected void send(Destination dest, Message msg, int deliveryMode, int priority, long timeToLive)
            throws JMSException {
        StompJmsDestination destination = StompJmsMessageTransformation.transformDestination(dest);
        StompJmsMessage message = StompJmsMessageTransformation.transformMessage(msg);
        send(destination, message, deliveryMode, priority, timeToLive);
    }

    private void send(StompJmsDestination destination, StompJmsMessage message, int deliveryMode, int priority,
                      long timeToLive) throws JMSException {
        message.setMessageID(getNextMessageId());
        message.setJMSDestination(destination);
        message.setJMSDeliveryMode(deliveryMode);
        message.setJMSPriority(priority);
        if (timeToLive > 0) {
            long timeStamp = System.currentTimeMillis();
            message.setJMSTimestamp(timeStamp);
            message.setJMSExpiration(System.currentTimeMillis() + timeToLive);
        }
        if (message.isPersistent() && !getTransacted()) {
            this.channel.sendMessageRequest(message);
        } else {
            this.channel.sendMessage(message);
        }
    }

    protected void checkClosed() throws IllegalStateException {
        if (this.closed.get()) {
            throw new IllegalStateException("The MessageProducer is closed");
        }
    }

    public void onMessage(StompJmsMessage message) {
        if (started.get()) {
            dispatch(message);
        } else {
            this.stoppedMessages.add(message);
        }

    }

    protected void start() throws JMSException {
        if (started.compareAndSet(false, true)) {
            StompJmsMessage message = null;
            while ((message = this.stoppedMessages.poll()) != null) {
                dispatch(message);
            }
            if (getTransacted()) {
                this.currentTransactionId = this.channel.startTransaction();
            }
            for (StompJmsMessageConsumer consumer : consumers.values()) {
                consumer.start();
            }
        }
    }

    protected void stop() throws JMSException {
        started.set(false);
        for (StompJmsMessageConsumer consumer : consumers.values()) {
            consumer.stop();
        }
    }

    protected boolean isStarted() {
        return this.started.get();
    }

    protected StompChannel getChannel() {
        return this.channel;
    }

    protected StompJmsConnection getConnection() {
        return this.connection;
    }

    private void dispatch(StompJmsMessage message) {
        AsciiBuffer id = message.getConsumerId();
        if (id == null || id.isEmpty()) {
            this.connection.onException(new JMSException("No ConsumerId set for " + message));
        }
        if (this.messageListener != null) {
            this.messageListener.onMessage(message);
        } else {
            StompJmsMessageConsumer consumer = this.consumers.get(id);
            if (consumer != null) {
                consumer.onMessage(message);
            }
        }
    }

    private AsciiBuffer getNextMessageId() {
        AsciiBuffer session = channel.getSession();
        AsciiBuffer id = ascii(Long.toString(nextMessageSwquence++));
        ByteArrayOutputStream out = new ByteArrayOutputStream(session.length() + 1 + id.length());
        out.write(session);
        out.write('.');
        out.write(id);
        return out.toBuffer().ascii();
    }
}
