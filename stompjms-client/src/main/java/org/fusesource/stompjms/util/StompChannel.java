/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms.util;

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.stompjms.StompJmsDestination;
import org.fusesource.stompjms.StompJmsExceptionSupport;
import org.fusesource.stompjms.StompJmsMessageListener;
import org.fusesource.stompjms.client.ProtocolException;
import org.fusesource.stompjms.client.Stomp;
import org.fusesource.stompjms.client.StompFrame;
import org.fusesource.stompjms.client.callback.Callback;
import org.fusesource.stompjms.client.callback.Connection;
import org.fusesource.stompjms.client.future.CallbackFuture;
import org.fusesource.stompjms.message.StompJmsMessage;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.fusesource.stompjms.client.Constants.*;

public class StompChannel {
    static final long TIMEOUT = -1;
    String channelId;
    String userName;
    String password;
    String ackMode;
    URI brokerURI;
    URI localURI;
    Connection connection;
    StompJmsMessageListener listener;
    ExceptionListener exceptionListener;
    AtomicBoolean started = new AtomicBoolean();
    AtomicBoolean connected = new AtomicBoolean();
    AsciiBuffer currentTransactionId = null;
    AsciiBuffer session;
    
    private AtomicLong requestCounter = new AtomicLong();
    
    public AsciiBuffer getSession() {
        return session;
    }
    public AsciiBuffer nextId() {
        return nextId("");
    }

    public Connection getConnection() {
        return connection;
    }

    public AsciiBuffer nextId(String prefix) {
        return new AsciiBuffer(prefix+Long.toString(requestCounter.incrementAndGet()));
    }

    public StompChannel copy() {
        StompChannel copy = new StompChannel();
        copy.brokerURI = this.brokerURI;
        copy.localURI = this.localURI;
        copy.userName = this.userName;
        copy.password = this.password;
        copy.ackMode = this.ackMode;
        return copy;
    }

    public void connect() throws JMSException {
        if (this.connected.compareAndSet(false, true)) {

            try {

                final CallbackFuture<Connection> future = new CallbackFuture<Connection>();
                Stomp.callback(brokerURI)
                    .login(userName).passcode(password)
                    .localURI(localURI)
                    .connect(future);
                connection = future.await();
                connection.getDispatchQueue().execute(new Runnable() {
                    public void run() {
                        connection.receive(new Callback<StompFrame>() {
                            public void failure(Throwable value) {
                                handleException(value);
                            }
                            public void success(StompFrame value) {
                                onFrame(value);
                            }
                        });
                        connection.resume();
                    }
                });

                session = connection.connectedFrame().headerMap().get(SESSION);
                if ( session==null ) {
                    session = new AsciiBuffer("id-"+UUID.randomUUID().toString());
                }
                started.set(true);

            } catch (Exception e) {
                connected.set(false);
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    public boolean isStarted() {
        return started.get();
    }

    public void close() throws JMSException {
        if (connected.compareAndSet(true, false)) {
            started.set(false);
            this.connection.close(new Runnable() {
                public void run() {
                    connection = null;
                }
            });
        }
    }

    public void sendMessage(StompJmsMessage message, boolean sync) throws JMSException {
        StompJmsMessage copy = message.copy();
        copy.onSend();
        StompFrame frame = copy.getFrame();
        frame.action(SEND);
        frame.headerMap().put(CONTENT_LENGTH, new AsciiBuffer(Integer.toString(frame.content().length)));
        addTransaction(frame);
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

    public void ackMessage(AsciiBuffer consumerId, AsciiBuffer messageId, Boolean sync) throws JMSException {
//        System.out.println(""+socket.getLocalAddress() +" ack "+ messageId);
        StompFrame frame = new StompFrame();
        frame.action(ACK);
        frame.headerMap().put(SUBSCRIPTION, consumerId);
        frame.headerMap().put(MESSAGE_ID, messageId);
        addTransaction(frame);
        try {
            if(sync) {
                sendRequest(frame);
            } else {
                sendFrame(frame);
            }
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void subscribe(StompJmsDestination destination, AsciiBuffer consumerId, AsciiBuffer selector, boolean clientAck, boolean persistent, boolean browser) throws JMSException {
        StompFrame frame = new StompFrame();
        frame.action(SUBSCRIBE);
        frame.headerMap().put(DESTINATION, destination.toBuffer());
        frame.headerMap().put(ID, consumerId);
        if (selector != null && selector.trim().isEmpty() == false) {
            frame.headerMap().put(SELECTOR, selector);
        }
//        if (clientAck) {
//            frame.headerMap().put(ACK_MODE, CLIENT);
//        } else {
//            frame.headerMap().put(ACK_MODE, AUTO);
//        }
        frame.headerMap().put(ACK_MODE, CLIENT);
        if (persistent) {
            frame.headerMap().put(PERSISTENT, TRUE);
        }
        if (browser) {
            frame.headerMap().put(BROWSER, TRUE);
        }
        try {
            sendRequest(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void unsubscribe(StompJmsDestination destination, AsciiBuffer consumerId, boolean persistent, boolean browser) throws JMSException {
        StompFrame frame = new StompFrame();
        frame.action(UNSUBSCRIBE);
        if (destination != null) {
            frame.headerMap().put(DESTINATION, destination.toBuffer());
        }
        frame.headerMap().put(ID, consumerId);
        if (persistent) {
            frame.headerMap().put(PERSISTENT, TRUE);
        }
        try {
            sendFrame(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }


    public synchronized AsciiBuffer startTransaction() throws JMSException {
        if (this.currentTransactionId != null) {
            throw new JMSException("Transaction " + this.currentTransactionId + " already in progress");
        }
        this.currentTransactionId = nextId("TX-");
        StompFrame frame = new StompFrame();
        frame.action(BEGIN);
        addTransaction(frame);
        try {
            sendFrame(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
        return currentTransactionId;
    }

    public synchronized void commitTransaction() throws JMSException {
        AsciiBuffer id = this.currentTransactionId;
        StompFrame frame = new StompFrame();
        frame.action(COMMIT);
        addTransaction(frame);
        this.currentTransactionId = null;
        try {
            sendRequest(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void rollbackTransaction() throws JMSException {
        AsciiBuffer id = this.currentTransactionId;
        StompFrame frame = new StompFrame();
        frame.action(ABORT);
        addTransaction(frame);
        this.currentTransactionId = null;
        try {
            sendRequest(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void sendFrame(final StompFrame frame) throws IOException {
        try {
            final CallbackFuture<Void> future = new CallbackFuture<Void>();
            connection.getDispatchQueue().execute(new Runnable() {
                public void run() {
                    connection.send(frame, future);
                }
            });
            // Wait on the future so that we don't cause flow control
            // problems.
            future.await();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public void sendRequest(final StompFrame frame) throws IOException {
        try {
            final CallbackFuture<StompFrame> future = new CallbackFuture<StompFrame>();
            connection.getDispatchQueue().execute(new Runnable() {
                public void run() {
                    connection.request(frame, future);
                }
            });
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
            e.printStackTrace();
        }
    }

    private synchronized void addTransaction(StompJmsMessage message) {
        if (this.currentTransactionId != null) {
            message.setTransactionId(this.currentTransactionId);
        }
    }

    private synchronized void addTransaction(StompFrame frame) {
        if (this.currentTransactionId != null) {
            frame.headerMap().put(TRANSACTION, this.currentTransactionId);
        }
    }

}
