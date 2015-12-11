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

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.stomp.client.CallbackConnection;
import org.fusesource.stomp.client.ProtocolException;
import org.fusesource.stomp.client.Stomp;
import org.fusesource.stomp.codec.StompFrame;
import org.fusesource.stomp.client.Callback;
import org.fusesource.stomp.client.Promise;
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.fusesource.stomp.jms.util.StompTranslator;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fusesource.stomp.client.Constants.*;
import static org.fusesource.hawtdispatch.Dispatch.*;

public class StompChannel {

    private static final StompServerAdaptor STOMP_SERVER_ADAPTORS[] = new StompServerAdaptor[]{
        new ApolloServerAdaptor(),
        new ActiveMQServerAdaptor(),
        new StompServerAdaptor()
    };

    static final long TIMEOUT = -1;
    String channelId;
    String userName;
    String password;
    String ackMode;
    boolean omitHost;
    URI brokerURI;
    URI localURI;
    CallbackConnection connection;
    StompJmsMessageListener listener;
    ExceptionListener exceptionListener;
    AtomicBoolean started = new AtomicBoolean();
    AtomicBoolean connected = new AtomicBoolean();
    AsciiBuffer sessionId;
    AtomicInteger writeBufferRemaining = new AtomicInteger();
    AtomicInteger serverAckSubs = new AtomicInteger();
    StompServerAdaptor serverAdaptor;
    String clientId;
    private long disconnectTimeout = 10000;

    public AsciiBuffer sessionId() {
        return sessionId;
    }

    public CallbackConnection connection() {
        return connection;
    }

    public StompChannel copy() {
        StompChannel copy = new StompChannel();
        copy.brokerURI = this.brokerURI;
        copy.localURI = this.localURI;
        copy.userName = this.userName;
        copy.password = this.password;
        copy.ackMode = this.ackMode;
        copy.omitHost = this.omitHost;
        return copy;
    }

    CountDownLatch connectedLatch = new CountDownLatch(1);

    public void connect(long timeoutMs) throws JMSException {
        if (this.connected.compareAndSet(false, true)) {
            try {
                final Promise<CallbackConnection> future = new Promise<CallbackConnection>();
                Stomp stomp = new Stomp(brokerURI);
                stomp.setLogin(userName);
                stomp.setPasscode(password);
                stomp.setLocalURI(localURI);
                stomp.setClientId(clientId);
                stomp.connectCallback(future);
                if( omitHost ) {
                    stomp.setHost(null);
                }

                connection = future.await();
                writeBufferRemaining.set(connection.transport().getProtocolCodec().getWriteBufferSize());
                connection.getDispatchQueue().execute(new Task() {
                    public void run() {
                        connection.receive(new Callback<StompFrame>() {
                            public void onFailure(Throwable value) {
                                handleException(value);
                            }
                            public void onSuccess(StompFrame value) {
                                onFrame(value);
                            }
                        });
                        connection.resume();
                    }
                });

                sessionId = connection.connectedFrame().headerMap().get(SESSION);
                if ( sessionId ==null ) {
                    sessionId = new AsciiBuffer("id-"+UUID.randomUUID().toString());
                }
                started.set(true);

                String sv = getServerAndVersion();
                for( StompServerAdaptor adaptor : STOMP_SERVER_ADAPTORS) {
                    if( adaptor.matchesServerAndVersion(sv) ) {
                        serverAdaptor = adaptor;
                        break;
                    }
                }
                assert serverAdaptor!=null;

            } catch (Exception e) {
                connected.set(false);
                throw StompJmsExceptionSupport.create(e);
            } finally {
                connectedLatch.countDown();
            }
        }
        try {
            connectedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw StompJmsExceptionSupport.create(e);
        }
        if (!started.get()) {
            throw StompJmsExceptionSupport.create(new TimeoutException("connection timeout"));
        }
    }

    public boolean isStarted() {
        return started.get();
    }

    public void close() throws JMSException {
        if (connected.compareAndSet(true, false)) {
            final CountDownLatch cd = new CountDownLatch(1);
            started.set(false);

            // Request a DISCONNECT so that we can try to flush the socket out.
            connection.getDispatchQueue().execute(new Task(){
                public void run() {
                    StompFrame frame = new StompFrame(DISCONNECT);
                    connection.request(frame, new Callback<StompFrame>(){
                        public void onFailure(Throwable value) {
                            onSuccess(null);
                        }
                        public void onSuccess(StompFrame value) {
                            cd.countDown();
                        }
                    });
                }
            });

            // Wait for the disconnect to finish..
            try {
                cd.await(getDisconnectTimeout(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new JMSException("Interrupted");
            } finally {
                connection.close(NOOP);
            }
        }
    }

    public void sendMessage(StompJmsMessage copy, AsciiBuffer txid, boolean sync) throws JMSException {
        if( sync && serverAckSubs.get() >0 ) {
            throw new JMSException("Sync message sends not allowed when a subscription is using 'ack:auto'.  Causes deadlocks.");
        }
        copy.onSend();
        StompFrame frame = copy.getFrame();
        frame.action(SEND);
        frame.headerMap().put(CONTENT_LENGTH, new AsciiBuffer(Integer.toString(frame.content().length)));
        if (txid != null) {
            frame.headerMap().put(TRANSACTION, txid);
        }
        try {
            if( sync ) {
                sendRequest(frame);
            } else {
                sendFrame(frame);
            }
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void ackMessage(AsciiBuffer consumerId, AsciiBuffer messageId, AsciiBuffer txid, Promise<StompFrame> callback) throws JMSException {
        if( callback!=null && serverAckSubs.get() >0 ) {
            throw new JMSException("Sync acks not allowed when a subscription is using 'ack:auto'.  Causes deadlocks.");
        }
        connection.getDispatchQueue().assertExecuting();
        StompFrame frame = new StompFrame();
        frame.action(ACK);
        frame.headerMap().put(SUBSCRIPTION, consumerId);
        frame.headerMap().put(MESSAGE_ID, messageId);
        if (txid != null) {
            frame.headerMap().put(TRANSACTION, txid);
        }
        try {
            if(callback!=null) {
                sendRequest(frame, callback);
            } else {
                sendFrame(frame);
            }
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void subscribe(StompJmsDestination destination, AsciiBuffer consumerId, AsciiBuffer selector, AsciiBuffer ackMode, boolean noLocal, boolean persistent, boolean browser, StompJmsPrefetch prefetch, Map<AsciiBuffer, AsciiBuffer> headers) throws JMSException {
        StompFrame frame = new StompFrame();
        frame.action(SUBSCRIBE);
        final Map<AsciiBuffer, AsciiBuffer> headerMap = frame.headerMap();
        headerMap.put(DESTINATION, destination.toBuffer());
        headerMap.put(ID, consumerId);
        if (selector != null && selector.trim().isEmpty() == false) {
            headerMap.put(SELECTOR, selector);
        }
        headerMap.put(ACK_MODE, ackMode);
        if ( prefetch!=null ) {
            serverAdaptor.addSubscribeHeaders(headerMap, persistent, browser, noLocal, prefetch);
        }
        if(headers!=null) {
            headerMap.putAll(headers);
        }
        try {
            if( !destination.isTopic() && serverAckSubs.get() > 0 ) {
                // have to do async to avoid deadlocks.
                sendFrame(frame);
            } else {
                // do it sync so that if a producer send to a topic we just
                // subscribed on, we can ensure the subscription will receive
                // the message.
                sendRequest(frame);
            }
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void unsubscribe(AsciiBuffer consumerId, boolean persistent) throws JMSException {
        StompFrame frame = serverAdaptor.createUnsubscribeFrame(consumerId, persistent);
        try {
            sendFrame(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }


    public AsciiBuffer startTransaction() throws JMSException {
        AsciiBuffer txid = connection.nextId("TX-");
        StompFrame frame = new StompFrame();
        frame.action(BEGIN);
        if (txid != null) {
            frame.headerMap().put(TRANSACTION, txid);
        }
        try {
            sendFrame(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
        return txid;
    }

    public void commitTransaction(AsciiBuffer txid) throws JMSException {
        if( serverAckSubs.get() >0 ) {
            throw new JMSException("transactions not allowed when a subscription is using 'ack:auto'.  Causes deadlocks.");
        }
        StompFrame frame = new StompFrame();
        frame.action(COMMIT);
        if (txid != null) {
            frame.headerMap().put(TRANSACTION, txid);
        }
        try {
            sendRequest(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void rollbackTransaction(AsciiBuffer txid) throws JMSException {
        if( serverAckSubs.get() >0 ) {
            throw new JMSException("transactions not allowed when a subscription is using 'ack:auto'.  Causes deadlocks.");
        }
        StompFrame frame = new StompFrame();
        frame.action(ABORT);
        if (txid != null) {
            frame.headerMap().put(TRANSACTION, txid);
        }
        try {
            sendRequest(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }


    public void sendFrame(final StompFrame frame) throws IOException {
        try {
            final int size = frame.size();
            if( writeBufferRemaining.getAndAdd(-size) > 0 ) {
                // just send it without blocking...
                connection.getDispatchQueue().execute(new Task() {
                    public void run() {
                        connection.send(frame, new Callback<Void>(){
                            public void onFailure(Throwable value) {
                                handleException(value);
                            }
                            @Override
                            public void onSuccess(Void value) {
                                writeBufferRemaining.getAndAdd(size);
                            }
                        });
                    }
                });
            } else {
                // ran out of buffer space.. wait for the write to complete
                // so that we don't blow out our memory buffers.
                final Promise<Void> future = new Promise<Void>() {
                    @Override
                    public void onSuccess(Void value) {
                        writeBufferRemaining.getAndAdd(size);
                        super.onSuccess(value);
                    }
                };
                connection.getDispatchQueue().execute(new Task() {
                    public void run() {
                        connection.send(frame, future);
                    }
                });
                future.await();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public void sendRequest(final StompFrame frame, final Promise<StompFrame> future) {
        connection.getDispatchQueue().execute(new Task() {
            public void run() {
                connection.request(frame, future);
            }
        });
    }

    public void sendRequest(final StompFrame frame) throws IOException {
        try {
            final Promise<StompFrame> future = new Promise<StompFrame>();
            sendRequest(frame, future);
            // Wait on the future so that we don't cause flow control
            // problems.
            future.await();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public void onFrame(StompFrame frame) {
        AsciiBuffer action = frame.action();
        if (action.startsWith(MESSAGE)) {
            try {
                StompJmsMessage msg = StompTranslator.convert(frame);
                msg.setFrame(frame);
                msg.setReadOnlyBody(true);
                msg.setReadOnlyProperties(true);
                StompJmsMessageListener l = this.listener;
                if (l != null) {
                    l.onMessage(msg);
                }
            } catch (JMSException e) {
                handleException(e);
            }
        } else {
            handleException(new ProtocolException("Unknown STOMP action: " + action));
        }
    }

    /**
     * @return the channelId
     */
    public String getChannelId() {
        return this.channelId;
    }

    /**
     * @param channelId the channelId to set
     */
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the ackMode
     */
    public String getAckMode() {
        return this.ackMode;
    }

    /**
     * @param ackMode the ackMode to set
     */
    public void setAckMode(String ackMode) {
        this.ackMode = ackMode;
    }

    /**
     * @return the brokerURI
     */
    public URI getBrokerURI() {
        return this.brokerURI;
    }

    /**
     * @param brokerURI the brokerURI to set
     */
    public void setBrokerURI(URI brokerURI) {
        this.brokerURI = brokerURI;
    }

    /**
     * @return the localURI
     */
    public URI getLocalURI() {
        return this.localURI;
    }

    /**
     * @param localURI the localURI to set
     */
    public void setLocalURI(URI localURI) {
        this.localURI = localURI;
    }
    
    public boolean isOmitHost() {
        return omitHost;
    }

    public void setOmitHost(boolean omitHost) {
        this.omitHost = omitHost;
    }
    /**
     * @return the listener
     */
    public StompJmsMessageListener getListener() {
        return this.listener;
    }

    /**
     * @param listener the listener to set
     */
    public void setListener(StompJmsMessageListener listener) {
        this.listener = listener;
    }

    public void setExceptionListener(ExceptionListener listener) {
        this.exceptionListener = listener;
    }

    private void handleException(Throwable e) {
        ExceptionListener l = this.exceptionListener;
        if (l != null) {
            l.onException(StompJmsExceptionSupport.create(e));
        } else {
            if( started.get() ) {
                e.printStackTrace();
            }
        }
    }

    public AsciiBuffer nextId() {
        return connection.nextId();
    }

    public String getConnectedHostId() {
        return getConnectedHeader(HOST_ID);
    }

    public String getServerAndVersion() {
        return getConnectedHeader(SERVER);
    }

    public String getConnectedSessionId() {
        return getConnectedHeader(SESSION);
    }

    private String getConnectedHeader(AsciiBuffer header) {
        final StompFrame frame = connection.connectedFrame();
        String rc = null;
        if( frame != null ) {
            final AsciiBuffer host = frame.getHeader(header);
            if( host !=null ) {
                rc = host.toString();
            }
        }
        return rc;
    }

    public StompServerAdaptor getServerAdaptor() {
        return serverAdaptor;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getDisconnectTimeout() {
        return disconnectTimeout;
    }

    public void setDisconnectTimeout(long disconnectTimeout) {
        this.disconnectTimeout = disconnectTimeout;
    }
}
