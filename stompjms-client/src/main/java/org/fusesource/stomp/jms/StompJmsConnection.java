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
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of a JMS Connection
 */
public class StompJmsConnection implements Connection, TopicConnection, QueueConnection {

    private String clientId;
    private int clientNumber = 0;
    private boolean clientIdSet;
    private ExceptionListener exceptionListener;
    private List<StompJmsSession> sessions = new CopyOnWriteArrayList<StompJmsSession>();
    private AtomicBoolean connected = new AtomicBoolean();
    private AtomicBoolean closed = new AtomicBoolean();
    private AtomicBoolean started = new AtomicBoolean();
    String queuePrefix = "/queue/";
    String topicPrefix = "/topic/";
    String tempQueuePrefix = "/temp-queue/";
    String tempTopicPrefix = "/temp-topic/";

    boolean forceAsyncSend;
    boolean omitHost;

    final URI brokerURI;
    final URI localURI;
    final String userName;
    final String password;
    StompChannel channel;
    boolean isConnnectedToApolloServer;

    /**
     * @param brokerURI
     * @param localURI
     * @param userName
     * @param password
     * @throws JMSException
     */
    protected StompJmsConnection(URI brokerURI, URI localURI, String userName, String password) throws JMSException {
        this.brokerURI = brokerURI;
        this.localURI = localURI;
        this.userName = userName;
        this.password = password;
    }

    /**
     * @throws JMSException
     * @see javax.jms.Connection#close()
     */
    public synchronized void close() throws JMSException {
        if (closed.compareAndSet(false, true)) {
            try {
                for (Session s : this.sessions) {
                    s.close();
                }
                this.sessions.clear();
                if (channel != null) {
                    channel.close();
                    channel = null;
                }
            } catch (Exception e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    /**
     * @param destination
     * @param messageSelector
     * @param sessionPool
     * @param maxMessages
     * @return ConnectionConsumer
     * @throws JMSException
     * @see javax.jms.Connection#createConnectionConsumer(javax.jms.Destination,
     *      java.lang.String, javax.jms.ServerSessionPool, int)
     */
    public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector,
                                                       ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        checkClosed();
        connect();
        throw new JMSException("Not supported");
    }

    /**
     * @param topic
     * @param subscriptionName
     * @param messageSelector
     * @param sessionPool
     * @param maxMessages
     * @return ConnectionConsumer
     * @throws JMSException
     * @see javax.jms.Connection#createDurableConnectionConsumer(javax.jms.Topic,
     *      java.lang.String, java.lang.String, javax.jms.ServerSessionPool,
     *      int)
     */
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
                                                              String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        checkClosed();
        connect();
        throw new JMSException("Not supported");
    }

    /**
     * @param transacted
     * @param acknowledgeMode
     * @return Session
     * @throws JMSException
     * @see javax.jms.Connection#createSession(boolean, int)
     */
    public synchronized Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        checkClosed();
        connect();
        int ackMode = getSessionAcknowledgeMode(transacted, acknowledgeMode);
        StompJmsSession result = new StompJmsSession(this, ackMode, forceAsyncSend);
        addSession(result);
        if (started.get()) {
            result.start();
        }
        return result;
    }

    /**
     * @return clientId
     * @see javax.jms.Connection#getClientID()
     */
    public String getClientID() {
        return this.clientId;
    }

    /**
     * @return ExceptionListener
     * @see javax.jms.Connection#getExceptionListener()
     */
    public ExceptionListener getExceptionListener() {
        return this.exceptionListener;
    }

    /**
     * @return ConnectionMetaData
     * @see javax.jms.Connection#getMetaData()
     */
    public ConnectionMetaData getMetaData() {
        return StompJmsConnectionMetaData.INSTANCE;
    }

    /**
     * @param clientID
     * @throws JMSException
     * @see javax.jms.Connection#setClientID(java.lang.String)
     */
    public synchronized void setClientID(String clientID) throws JMSException {
        if (this.clientIdSet) {
            throw new IllegalStateException("The clientID has already been set");
        }
        if (clientID == null) {
            throw new IllegalStateException("Cannot have a null clientID");
        }
        if( connected.get() ) {
            throw new IllegalStateException("Cannot set the client id once connected.");
        }
        this.clientId = clientID;
        this.clientIdSet = true;
    }

    /**
     * @param listener
     * @see javax.jms.Connection#setExceptionListener(javax.jms.ExceptionListener)
     */
    public void setExceptionListener(ExceptionListener listener) {
        this.exceptionListener = listener;
    }

    /**
     * @throws JMSException
     * @see javax.jms.Connection#start()
     */
    public void start() throws JMSException {
        checkClosed();
        connect();
        if (this.started.compareAndSet(false, true)) {
            try {
                for (StompJmsSession s : this.sessions) {
                    s.start();
                }
            } catch (Exception e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    /**
     * @throws JMSException
     * @see javax.jms.Connection#stop()
     */
    public void stop() throws JMSException {
        checkClosed();
        connect();
        if (this.started.compareAndSet(true, false)) {
            try {
                for (StompJmsSession s : this.sessions) {
                    s.stop();
                }
            } catch (Exception e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    /**
     * @param topic
     * @param messageSelector
     * @param sessionPool
     * @param maxMessages
     * @return ConnectionConsumer
     * @throws JMSException
     * @see javax.jms.TopicConnection#createConnectionConsumer(javax.jms.Topic,
     *      java.lang.String, javax.jms.ServerSessionPool, int)
     */
    public ConnectionConsumer createConnectionConsumer(Topic topic, String messageSelector,
                                                       ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        checkClosed();
        connect();
        return null;
    }

    /**
     * @param transacted
     * @param acknowledgeMode
     * @return TopicSession
     * @throws JMSException
     * @see javax.jms.TopicConnection#createTopicSession(boolean, int)
     */
    public TopicSession createTopicSession(boolean transacted, int acknowledgeMode) throws JMSException {
        checkClosed();
        connect();
        int ackMode = getSessionAcknowledgeMode(transacted, acknowledgeMode);
        StompJmsTopicSession result = new StompJmsTopicSession(this, ackMode, forceAsyncSend);
        addSession(result);
        if (started.get()) {
            result.start();
        }
        return result;
    }

    /**
     * @param queue
     * @param messageSelector
     * @param sessionPool
     * @param maxMessages
     * @return ConnectionConsumer
     * @throws JMSException
     * @see javax.jms.QueueConnection#createConnectionConsumer(javax.jms.Queue,
     *      java.lang.String, javax.jms.ServerSessionPool, int)
     */
    public ConnectionConsumer createConnectionConsumer(Queue queue, String messageSelector,
                                                       ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        checkClosed();
        connect();
        return null;
    }

    /**
     * @param transacted
     * @param acknowledgeMode
     * @return QueueSession
     * @throws JMSException
     * @see javax.jms.QueueConnection#createQueueSession(boolean, int)
     */
    public QueueSession createQueueSession(boolean transacted, int acknowledgeMode) throws JMSException {
        checkClosed();
        connect();
        int ackMode = getSessionAcknowledgeMode(transacted, acknowledgeMode);
        StompJmsQueueSession result = new StompJmsQueueSession(this, ackMode, forceAsyncSend);
        addSession(result);
        if (started.get()) {
            result.start();
        }
        return result;
    }

    /**
     * @param ex
     */
    public void onException(Exception ex) {
        onException(StompJmsExceptionSupport.create(ex));
    }

    /**
     * @param ex
     */
    public void onException(JMSException ex) {
        ExceptionListener l = this.exceptionListener;
        if (l != null) {
            l.onException(StompJmsExceptionSupport.create(ex));
        }
    }

    protected int getSessionAcknowledgeMode(boolean transacted, int acknowledgeMode) throws JMSException {
        int result = acknowledgeMode;
        if (!transacted && acknowledgeMode == Session.SESSION_TRANSACTED) {
            throw new JMSException("acknowledgeMode SESSION_TRANSACTED cannot be used for an non-transacted Session");
        }
        if (transacted) {
            result = Session.SESSION_TRANSACTED;
        }
        return result;
    }

    protected synchronized StompChannel createChannel() throws JMSException {
        StompChannel rc = new StompChannel();
        rc.setBrokerURI(brokerURI);
        rc.setLocalURI(localURI);
        rc.setUserName(userName);
        rc.setPassword(password);
        rc.setOmitHost(omitHost);
        rc.setExceptionListener(this.exceptionListener);
        rc.setChannelId(clientId + "-" + clientNumber++);
        return rc;
    }

    protected StompChannel getChannel() throws JMSException {
        StompChannel rc;
        synchronized (this) {
            if(channel == null) {
                channel = createChannel();
            }
            rc = channel;
        }
        rc.connect();
        String sv = rc.getServerAndVersion();
        isConnnectedToApolloServer = sv!=null && sv.startsWith("apache-apollo/");
        return rc;
    }

    protected StompChannel createChannel(StompJmsSession s) throws JMSException {
        StompChannel rc;
        synchronized (this) {
            if(channel != null) {
                rc = channel;
                channel = null;
            } else {
                rc = createChannel();
            }
        }
        rc.connect();
        rc.setListener(s);
        return rc;
    }

    protected void removeSession(StompJmsSession s, StompChannel channel) throws JMSException {
        synchronized (this) {
            this.sessions.remove(s);
            if( channel!=null && this.channel==null ) {
                // just in case some one is in a loop creating/closing sessions.
                this.channel = channel;
                channel = null;
            }
        }
        if(channel!=null) {
            channel.setListener(null);
            channel.close();
        }
    }

    protected void addSession(StompJmsSession s) {
        this.sessions.add(s);
    }

    protected void checkClosed() throws IllegalStateException {
        if (this.closed.get()) {
            throw new IllegalStateException("The MessageProducer is closed");
        }
    }

    private void connect() throws JMSException {
        if (connected.compareAndSet(false, true)) {
            getChannel();
        }
    }

    public boolean isForceAsyncSend() {
        return forceAsyncSend;
    }

    /**
     * If set to true then all mesage sends are done async.
     * @param forceAsyncSend
     */
    public void setForceAsyncSend(boolean forceAsyncSend) {
        this.forceAsyncSend = forceAsyncSend;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public String getTempTopicPrefix() {
        return tempTopicPrefix;
    }

    public void setTempTopicPrefix(String tempTopicPrefix) {
        this.tempTopicPrefix = tempTopicPrefix;
    }

    public String getTempQueuePrefix() {
        return tempQueuePrefix;
    }

    public void setTempQueuePrefix(String tempQueuePrefix) {
        this.tempQueuePrefix = tempQueuePrefix;
    }

    public String getQueuePrefix() {
        return queuePrefix;
    }

    public void setQueuePrefix(String queuePrefix) {
        this.queuePrefix = queuePrefix;
    }
    
    public boolean isOmitHost() {
        return omitHost;
    }

    public void setOmitHost(boolean omitHost) {
        this.omitHost = omitHost;
    }

    StompJmsTempQueue isTempQueue(String value) throws JMSException {
        connect();
        if( isConnnectedToApolloServer && value.startsWith(queuePrefix+"temp.")) {
            return new StompJmsTempQueue(queuePrefix, value.substring(queuePrefix.length()));
        }
        if( tempQueuePrefix!=null && value.startsWith(tempQueuePrefix) ) {
            return new StompJmsTempQueue(tempQueuePrefix, value.substring(tempQueuePrefix.length()));
        }
        return null;
    }

    StompJmsTempTopic isTempTopic(String value) throws JMSException {
        connect();
        if( isConnnectedToApolloServer && value.startsWith(topicPrefix+"temp.")) {
            return new StompJmsTempTopic(topicPrefix, value.substring(topicPrefix.length()));
        }
        if( tempTopicPrefix!=null && value.startsWith(tempTopicPrefix) ) {
            return new StompJmsTempTopic(tempTopicPrefix, value.substring(tempTopicPrefix.length()));
        }
        return null;
    }

}
