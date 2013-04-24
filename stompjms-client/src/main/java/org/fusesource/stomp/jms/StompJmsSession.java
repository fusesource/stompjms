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

import org.apache.activemq.apollo.filter.FilterException;
import org.apache.activemq.apollo.selector.SelectorParser;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.ByteArrayOutputStream;
import org.fusesource.stomp.codec.StompFrame;
import org.fusesource.stomp.jms.message.*;

import javax.jms.*;
import javax.jms.IllegalStateException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.fusesource.hawtbuf.Buffer.ascii;
import static org.fusesource.stomp.client.Constants.AUTO;
import static org.fusesource.stomp.client.Constants.CLIENT;

/**
 * JMS Session implementation
 */
public class StompJmsSession implements Session, QueueSession, TopicSession, StompJmsMessageListener {

    static final int SERVER_AUTO_ACKNOWLEDGE = -1;

    long nextMessageSwquence = 0;
    final StompJmsConnection connection;
    final int acknowledgementMode;
    final List<MessageProducer> producers = new CopyOnWriteArrayList<MessageProducer>();
    final Map<AsciiBuffer, StompJmsMessageConsumer> consumers = new ConcurrentHashMap<AsciiBuffer, StompJmsMessageConsumer>();
    MessageListener messageListener;
    AtomicBoolean closed = new AtomicBoolean();
    AtomicBoolean started = new AtomicBoolean();
    volatile AsciiBuffer currentTransactionId;
    boolean forceAsyncSend;
    long consumerMessageBufferSize = 1024*64;
    LinkedBlockingQueue<StompJmsMessage> stoppedMessages = new LinkedBlockingQueue<StompJmsMessage>(10000);
    StompChannel channel;
    StompJmsPrefetch prefetch;

    /**
     * Constructor
     *
     * @param connection
     * @param acknowledgementMode
     */
    protected StompJmsSession(StompJmsConnection connection, int acknowledgementMode, boolean forceAsyncSend) {
        this.connection = connection;
        this.acknowledgementMode = acknowledgementMode;
        this.forceAsyncSend = forceAsyncSend;
        this.prefetch = new StompJmsPrefetch(connection.prefetch);
    }

    /////////////////////////////////////////////////////////////////////////
    //
    // Session methods
    //
    /////////////////////////////////////////////////////////////////////////

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
     * @return true if transacted
     * @throws JMSException
     * @see javax.jms.Session#getTransacted()
     */
    public boolean getTransacted() throws JMSException {
        checkClosed();
        return this.acknowledgementMode == Session.SESSION_TRANSACTED;
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
     * @param listener
     * @throws JMSException
     * @see javax.jms.Session#setMessageListener(javax.jms.MessageListener)
     */
    public void setMessageListener(MessageListener listener) throws JMSException {
        checkClosed();
        this.messageListener = listener;
    }

    /**
     * @throws JMSException
     * @see javax.jms.Session#recover()
     */
    public void recover() throws JMSException {
        checkClosed();
        if (getTransacted()) {
            throw new javax.jms.IllegalStateException("Cannot call recover() on a transacted session");
        }
        // TODO: re-deliver all un-acked client-ack messages.
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
        for (StompJmsMessageConsumer c : consumers.values()) {
            c.commit();
        }
        getChannel().commitTransaction(currentTransactionId);
        this.currentTransactionId = getChannel().startTransaction();
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
            c.rollback();
        }
        getChannel().rollbackTransaction(currentTransactionId);
        this.currentTransactionId = getChannel().startTransaction();
        getExecutor().execute(new Runnable() {
            public void run() {
                for (StompJmsMessageConsumer c : consumers.values()) {
                    c.drainMessageQueueToListener();
                }
            }
        });
    }

    /**
     * @see javax.jms.Session#run()
     */
    public void run() {
        // TODO Auto-generated method stub
    }

    /**
     * @throws JMSException
     * @see javax.jms.Session#close()
     */
    public void close() throws JMSException {
        if (closed.compareAndSet(false, true)) {
            stop();
            this.connection.removeSession(this, channel);
            for (StompJmsMessageConsumer c : new ArrayList<StompJmsMessageConsumer>(this.consumers.values())) {
                c.close();
            }
            this.connection.removeSession(this, channel);
            channel = null;
        }
    }

    /////////////////////////////////////////////////////////////////////////
    //
    // Consumer creation
    //
    /////////////////////////////////////////////////////////////////////////

    /**
     * @param destination
     * @return a MessageConsumer
     * @throws JMSException
     * @see javax.jms.Session#createConsumer(javax.jms.Destination)
     */
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        checkClosed();
        checkDestination(destination);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, destination);
        StompJmsMessageConsumer result = new StompJmsMessageConsumer(getChannel().nextId(), this, dest, "");
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
        checkDestination(destination);
        messageSelector = checkSelector(messageSelector);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, destination);
        StompJmsMessageConsumer result = new StompJmsMessageConsumer(getChannel().nextId(), this, dest,
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
        checkDestination(destination);
        messageSelector = checkSelector(messageSelector);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, destination);
        StompJmsTopicSubscriber result = new StompJmsTopicSubscriber(getChannel().nextId(), this, dest, NoLocal,
                messageSelector);
        result.init();
        return result;
    }

    /**
     * @param queue
     * @return QueueRecevier
     * @throws JMSException
     * @see javax.jms.QueueSession#createReceiver(javax.jms.Queue)
     */
    public QueueReceiver createReceiver(Queue queue) throws JMSException {
        checkClosed();
        checkDestination(queue);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, queue);
        StompJmsQueueReceiver result = new StompJmsQueueReceiver(getChannel().nextId(), this, dest, "");
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
        checkDestination(queue);
        messageSelector = checkSelector(messageSelector);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, queue);
        StompJmsQueueReceiver result = new StompJmsQueueReceiver(getChannel().nextId(), this, dest, messageSelector);
        result.init();
        return result;
    }

    /**
     * @param destination
     * @return QueueBrowser
     * @throws JMSException
     * @see javax.jms.Session#createBrowser(javax.jms.Queue)
     */
    public QueueBrowser createBrowser(Queue destination) throws JMSException {
        checkClosed();
        checkDestination(destination);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, destination);
        StompJmsQueueBrowser result = new StompJmsQueueBrowser(this, getChannel().nextId(), dest, "");
        return result;
    }

    /**
     * @param destination
     * @param messageSelector
     * @return QueueBrowser
     * @throws JMSException
     * @see javax.jms.Session#createBrowser(javax.jms.Queue, java.lang.String)
     */
    public QueueBrowser createBrowser(Queue destination, String messageSelector) throws JMSException {
        checkClosed();
        checkDestination(destination);
        messageSelector = checkSelector(messageSelector);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, destination);
        StompJmsQueueBrowser result = new StompJmsQueueBrowser(this, getChannel().nextId(), dest, messageSelector);
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
        checkDestination(topic);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, topic);
        StompJmsTopicSubscriber result = new StompJmsTopicSubscriber(getChannel().nextId(), this, dest, false, "");
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
        checkDestination(topic);
        messageSelector = checkSelector(messageSelector);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, topic);
        StompJmsTopicSubscriber result = new StompJmsTopicSubscriber(getChannel().nextId(), this, dest, noLocal, messageSelector);
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
        checkDestination(topic);
        AsciiBuffer id = StompFrame.encodeHeader(this.connection.getClientID() + ":" + name);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, topic);
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
        checkDestination(topic);
        messageSelector = checkSelector(messageSelector);
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, topic);
        AsciiBuffer id;
        if( name !=null ) {
            id = StompFrame.encodeHeader(name);
        } else {
            id = getChannel().nextId();
        }
        StompJmsTopicSubscriber result = new StompJmsDurableTopicSubscriber(id, this, dest, noLocal,messageSelector);
        result.init();
        return result;
    }

    /**
     * @param name
     * @throws JMSException
     * @see javax.jms.Session#unsubscribe(java.lang.String)
     */
    public void unsubscribe(String name) throws JMSException {
        checkClosed();
        AsciiBuffer id = StompFrame.encodeHeader(name);
        StompJmsMessageConsumer consumer = this.consumers.remove(id);
        if (consumer != null) {
            consumer.close();
        }
        getChannel().unsubscribe(id, true);
    }

    /////////////////////////////////////////////////////////////////////////
    //
    // Producer creation
    //
    /////////////////////////////////////////////////////////////////////////

    /**
     * @param destination
     * @return MessageProducer
     * @throws JMSException
     * @see javax.jms.Session#createProducer(javax.jms.Destination)
     */
    public MessageProducer createProducer(Destination destination) throws JMSException {
        checkClosed();
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, destination);
        StompJmsMessageProducer result = new StompJmsMessageProducer(this, dest);
        add(result);
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
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, queue);
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
        StompJmsDestination dest = StompJmsMessageTransformation.transformDestination(connection, topic);
        StompJmsTopicPublisher result = new StompJmsTopicPublisher(this, dest);
        add(result);
        return result;
    }

    /////////////////////////////////////////////////////////////////////////
    //
    // Message creation
    //
    /////////////////////////////////////////////////////////////////////////

    /**
     * @return BytesMessage
     * @throws IllegalStateException
     * @see javax.jms.Session#createBytesMessage()
     */
    public BytesMessage createBytesMessage() throws IllegalStateException {
        checkClosed();
        return init(new StompJmsBytesMessage());
    }

    /**
     * @return MapMessage
     * @throws IllegalStateException
     * @see javax.jms.Session#createMapMessage()
     */
    public MapMessage createMapMessage() throws IllegalStateException {
        checkClosed();
        return init(new StompJmsMapMessage());
    }

    /**
     * @return Message
     * @throws IllegalStateException
     * @see javax.jms.Session#createMessage()
     */
    public Message createMessage() throws IllegalStateException {
        checkClosed();
        return init(new StompJmsMessage());
    }

    /**
     * @return ObjectMessage
     * @throws IllegalStateException
     * @see javax.jms.Session#createObjectMessage()
     */
    public ObjectMessage createObjectMessage() throws IllegalStateException {
        checkClosed();
        return init(new StompJmsObjectMessage());
    }

    /**
     * @param object
     * @return ObjectMessage
     * @throws JMSException
     * @see javax.jms.Session#createObjectMessage(java.io.Serializable)
     */
    public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
        ObjectMessage result = createObjectMessage();
        result.setObject(object);
        return result;
    }

    /**
     * @return StreamMessage
     * @throws JMSException
     * @see javax.jms.Session#createStreamMessage()
     */
    public StreamMessage createStreamMessage() throws JMSException {
        checkClosed();
        return init(new StompJmsStreamMessage());
    }

    /**
     * @return TextMessage
     * @throws JMSException
     * @see javax.jms.Session#createTextMessage()
     */
    public TextMessage createTextMessage() throws JMSException {
        checkClosed();
        return init(new StompJmsTextMessage());
    }

    /**
     * @param text
     * @return TextMessage
     * @throws JMSException
     * @see javax.jms.Session#createTextMessage(java.lang.String)
     */
    public TextMessage createTextMessage(String text) throws JMSException {
        TextMessage result = createTextMessage();
        result.setText(text);
        return result;
    }

    /////////////////////////////////////////////////////////////////////////
    //
    // Destination creation
    //
    /////////////////////////////////////////////////////////////////////////

    /**
     * @param queueName
     * @return Queue
     * @throws JMSException
     * @see javax.jms.Session#createQueue(java.lang.String)
     */
    public Queue createQueue(String queueName) throws JMSException {
        checkClosed();
        return new StompJmsQueue(connection, queueName);
    }

    /**
     * @return TemporaryQueue
     * @throws JMSException
     * @see javax.jms.Session#createTemporaryQueue()
     */
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        checkClosed();
        return getChannel().getServerAdaptor().createTemporaryQueue(this);
    }

    /**
     * @return TemporaryTopic
     * @throws JMSException
     * @see javax.jms.Session#createTemporaryTopic()
     */
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        checkClosed();
        return getChannel().getServerAdaptor().createTemporaryTopic(this);
    }

    /**
     * @param topicName
     * @return Topic
     * @throws JMSException
     * @see javax.jms.Session#createTopic(java.lang.String)
     */
    public Topic createTopic(String topicName) throws JMSException {
        checkClosed();
        return new StompJmsTopic(connection, topicName);
    }

    /////////////////////////////////////////////////////////////////////////
    //
    // Impl methods
    //
    /////////////////////////////////////////////////////////////////////////

    protected void add(StompJmsMessageConsumer consumer) throws JMSException {
        this.consumers.put(consumer.getId(), consumer);
        if(consumer.ackSource == null) {
            getChannel().serverAckSubs.incrementAndGet();
        }

        AsciiBuffer mode;
        if (this.acknowledgementMode == StompJmsSession.SERVER_AUTO_ACKNOWLEDGE) {
            mode = AUTO;
        } else {
            mode = CLIENT;
        }

        getChannel().subscribe(
                consumer.getDestination(),
                consumer.getId(),
                StompFrame.encodeHeader(consumer.getMessageSelector()),
                mode,
                consumer.getNoLocal(),
                consumer.isDurableSubscription(),
                consumer.isBrowser(),
                prefetch,
                StompFrame.encodeHeaders(consumer.getDestination().getSubscribeHeaders())
        );
        if (started.get()) {
            consumer.start();
        }
    }

    protected void remove(StompJmsMessageConsumer consumer) throws JMSException {
        if (getChannel().isStarted()) {
            getChannel().unsubscribe(consumer.getId(), false);
        }
        this.consumers.remove(consumer.getId());
        if(consumer.ackSource == null) {
            getChannel().serverAckSubs.decrementAndGet();
        }
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
        StompJmsDestination destination = StompJmsMessageTransformation.transformDestination(connection, dest);
        send(destination, msg, deliveryMode, priority, timeToLive);
    }

    private void send(StompJmsDestination destination, Message original, int deliveryMode, int priority,
                      long timeToLive) throws JMSException {

        original.setJMSDeliveryMode(deliveryMode);
        original.setJMSPriority(priority);
        if (timeToLive > 0) {
            long timeStamp = System.currentTimeMillis();
            original.setJMSTimestamp(timeStamp);
            original.setJMSExpiration(System.currentTimeMillis() + timeToLive);
        }
        final AsciiBuffer msgId = getNextMessageId();
        if( original instanceof StompJmsMessage ) {
            ((StompJmsMessage)original).setConnection(connection);
            ((StompJmsMessage)original).setMessageID(msgId);
        } else {
            original.setJMSMessageID(msgId.toString());
        }

        StompJmsMessage copy = StompJmsMessageTransformation.transformMessage(connection, original);
        copy.setJMSDestination(destination);
        boolean sync = !forceAsyncSend && deliveryMode==DeliveryMode.PERSISTENT && !getTransacted();

        // If we are doing transactions we HAVE to use the
        // session's channel since that's how the UOWs are being
        // delimited.  And if there are no consumers, then 
        // we know the TCP connection will not be getting flow controlled
        // by slow consumers, so it's safe to us it too. 
        if( consumers.isEmpty() || getTransacted()) {
            StompChannel channel = getChannel();
            channel.sendMessage(copy, currentTransactionId, sync);
        } else {
            // Non transacted session, with consumers.. they might end up
            // flow controlling the channel so lets publish the message
            // over the connection's main channel.
            copy.setMessageID(msgId);
            this.connection.getChannel().sendMessage(copy, currentTransactionId, sync);
        }
        
    }

    protected void checkClosed() throws IllegalStateException {
        if (this.closed.get()) {
            throw new IllegalStateException("The MessageProducer is closed");
        }
    }

    // This extra wrapping class around SelectorParser is used to avoid ClassNotFoundException
    // if SelectorParser is not in the class path.
    static class OptionalSectorParser {
        public static void check(String selector) throws InvalidSelectorException {
            try {
                SelectorParser.parse(selector);
            } catch (FilterException e) {
                throw new InvalidSelectorException(e.getMessage());
            }
        }
    }
    static final OptionalSectorParser SELECTOR_PARSER;
    static {
        OptionalSectorParser parser;
        try {
            // lets verify it's working..
            parser = new OptionalSectorParser();
            parser.check("x=1");
        } catch (Throwable e) {
            parser = null;
        }
        SELECTOR_PARSER = parser;
    }

    public static String checkSelector(String selector) throws InvalidSelectorException {
        if( selector!=null ) {
            if( selector.trim().length() ==0 ) {
                return null;
            }
            if( SELECTOR_PARSER !=null ) {
                SELECTOR_PARSER.check(selector);
            }
        }
        return selector;
    }
    public static void checkDestination(Destination dest) throws InvalidDestinationException {
        if( dest==null ) {
            throw new InvalidDestinationException("Destination cannot be null");
        }
    }

    public void onMessage(StompJmsMessage message) {
        message.setConnection(connection);
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
            if (getTransacted() && this.currentTransactionId == null) {
                this.currentTransactionId = getChannel().startTransaction();
            }
            for (StompJmsMessageConsumer consumer : consumers.values()) {
                consumer.start();
            }
        }
    }


    protected StompChannel getChannel() throws JMSException {
        if(this.channel == null) {
            this.channel = this.connection.createChannel(this);
        }
        return this.channel;
    }

    public boolean isForceAsyncSend() {
        return forceAsyncSend;
    }

    public void setForceAsyncSend(boolean forceAsyncSend) {
        this.forceAsyncSend = forceAsyncSend;
    }

    protected void stop() throws JMSException {
        started.set(false);
        if(executor!=null) {
            executor.shutdown();
            executor = null;
        }
        for (StompJmsMessageConsumer consumer : consumers.values()) {
            consumer.stop();
        }
    }

    protected boolean isStarted() {
        return this.started.get();
    }

    public StompJmsConnection getConnection() {
        return this.connection;
    }

    ExecutorService executor;

    Executor getExecutor() {
        if( executor ==null ) {
            executor = Executors.newSingleThreadExecutor();
        }
        return executor;
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

    private AsciiBuffer getNextMessageId() throws JMSException {
        AsciiBuffer session = null;
        if(channel!=null) {
            session = channel.sessionId();
        } else {
            session = connection.getChannel().sessionId();
        }
        AsciiBuffer id = ascii(Long.toString(nextMessageSwquence++));
        ByteArrayOutputStream out = new ByteArrayOutputStream(3+session.length() + 1 + id.length());
        out.write('I');
        out.write('D');
        out.write(':');
        out.write(session);
        out.write('-');
        out.write(id);
        return out.toBuffer().ascii();
    }

    private <T extends StompJmsMessage> T init(T message) {
        message.setConnection(connection);
        return message;
    }

    public StompJmsPrefetch getPrefetch() {
        return prefetch;
    }

    public void setPrefetch(StompJmsPrefetch prefetch) {
        this.prefetch = prefetch;
    }
}
