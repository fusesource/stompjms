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

package org.fusesource.stompjms.channel;

import org.fusesource.stompjms.StompJmsDestination;
import org.fusesource.stompjms.StompJmsExceptionSupport;
import org.fusesource.stompjms.StompJmsMessageListener;
import org.fusesource.stompjms.message.StompJmsMessage;
import org.fusesource.stompjms.util.LRUCache;
import org.fusesource.stompjms.util.SendRequest;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.net.SocketFactory;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


public class StompChannel implements StompFrameListener {
    private static final long TIMEOUT = 15000;
    private String channelId;
    private String userName;
    private String password;
    private String ackMode;
    private URI brokerURI;
    private URI localURI;
    private StompSocket socket;
    private StompJmsMessageListener listener;
    private ExceptionListener exceptionListener;
    private AtomicBoolean started = new AtomicBoolean();
    private AtomicBoolean initialized = new AtomicBoolean();
    private AtomicBoolean connected = new AtomicBoolean();
    private Map<String, SendRequest> requests = new LRUCache<String, SendRequest>();
    private String currentTransactionId = null;

    public StompChannel copy() {
        StompChannel copy = new StompChannel();
        copy.brokerURI = this.brokerURI;
        copy.localURI = this.localURI;
        copy.userName = this.userName;
        copy.password = this.password;
        copy.ackMode = this.ackMode;
        return copy;
    }

    public void initialize() throws JMSException {
        if (this.initialized.compareAndSet(false, true)) {
            try {
                this.socket = new StompSocket(SocketFactory.getDefault(), localURI, brokerURI);
                this.socket.setStompFrameListener(this);
                this.socket.initialize();
            } catch (IOException e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }


    public void connect() throws JMSException {
        if (this.connected.compareAndSet(false, true)) {
            initialize();
            try {
                this.socket.connect(getUserName(), getPassword(), getChannelId());
            } catch (IOException e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    public void start() throws JMSException {
        if (started.compareAndSet(false, true)) {
            connect();
            try {
                this.socket.setStompFrameListener(this);
                this.socket.start();
            } catch (Throwable e) {
                this.started.set(false);
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    public boolean isStarted() {
        return started.get();
    }

    public void stop() throws JMSException {
        if (started.compareAndSet(true, false)) {
            if (this.socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw StompJmsExceptionSupport.create(e);
                }
            }
        }
    }

    public void sendMessage(StompJmsMessage message) throws JMSException {
        StompJmsMessage copy = message.copy();
        copy.onSend();
        StompFrame frame = StompTranslator.convert(copy);
        frame.setAction(Stomp.Commands.SEND);
        addTransaction(frame);
        try {
            sendFrame(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void sendMessageRequest(StompJmsMessage message) throws JMSException {
        StompFrame frame = StompTranslator.convert(message);
        frame.setAction(Stomp.Commands.SEND);
        addTransaction(frame);
        try {
            sendRequest(UUID.randomUUID().toString(), frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void ackMessage(StompJmsDestination destination, String consumerId, String messageId) throws JMSException {
        StompFrame frame = new StompFrame();
        frame.setAction(Stomp.Commands.ACK);
        frame.getHeaders().put(Stomp.Headers.Ack.SUBSCRIPTION, consumerId);
        frame.getHeaders().put(Stomp.Headers.Ack.MESSAGE_ID, messageId);
        addTransaction(frame);
        try {
            sendFrame(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void subscribe(StompJmsDestination destination, String consumerId, String selector, boolean clientAck) throws JMSException {
        StompFrame frame = new StompFrame();
        frame.setAction(Stomp.Commands.SUBSCRIBE);
        frame.getHeaders().put(Stomp.Headers.Subscribe.DESTINATION, destination.toString());
        frame.getHeaders().put(Stomp.Headers.Subscribe.ID, consumerId);
        if (selector != null && selector.trim().isEmpty() == false) {
            frame.getHeaders().put(Stomp.Headers.Subscribe.SELECTOR, selector);
        }
        if (clientAck) {
            frame.getHeaders().put(Stomp.Headers.Subscribe.ACK_MODE, Stomp.Headers.Subscribe.AckModeValues.CLIENT);
        } else {
            frame.getHeaders().put(Stomp.Headers.Subscribe.ACK_MODE, Stomp.Headers.Subscribe.AckModeValues.AUTO);
        }
        try {
            sendRequest(consumerId, frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void unsubscribe(StompJmsDestination destination, String consumerId) throws JMSException {
        StompFrame frame = new StompFrame();
        frame.setAction(Stomp.Commands.UNSUBSCRIBE);
        frame.getHeaders().put(Stomp.Headers.Unsubscribe.DESTINATION, destination.toString());
        frame.getHeaders().put(Stomp.Headers.Unsubscribe.ID, consumerId);
        try {
            sendFrame(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }


    public synchronized String startTransaction() throws JMSException {
        if (this.currentTransactionId != null) {
            throw new JMSException("Transaction " + this.currentTransactionId + " already in progress");
        }
        this.currentTransactionId = "TX:" + UUID.randomUUID().toString();
        StompFrame frame = new StompFrame();
        frame.setAction(Stomp.Commands.BEGIN);
        addTransaction(frame);
        try {
            sendFrame(frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
        return currentTransactionId;
    }

    public synchronized void commitTransaction() throws JMSException {
        String id = this.currentTransactionId;
        StompFrame frame = new StompFrame();
        frame.setAction(Stomp.Commands.COMMIT);
        addTransaction(frame);
        this.currentTransactionId = null;
        try {
            sendRequest(id, frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void rollbackTransaction() throws JMSException {
        String id = this.currentTransactionId;
        StompFrame frame = new StompFrame();
        frame.setAction(Stomp.Commands.ABORT);
        addTransaction(frame);
        this.currentTransactionId = null;
        try {
            sendRequest(id, frame);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public void sendFrame(StompFrame frame) throws IOException {
        this.socket.sendFrame(frame);
    }

    public void sendRequest(String id, StompFrame frame) throws IOException {
        SendRequest sr = new SendRequest();
        synchronized (this.requests) {
            this.requests.put(id, sr);
        }
        frame.getHeaders().put(Stomp.Headers.RECEIPT_REQUESTED, id);
        this.socket.sendFrame(frame);
        StompFrame response = sr.get(TIMEOUT);
        if (response == null) {
            throw new IOException("SendRequest timed out for " + frame);
        }
    }


    public void onFrame(StompFrame frame) {
        String action = frame.getAction();
        if (frame.getClass() == StompFrameError.class) {
            handleException(((StompFrameError) frame).getException());
        }
        if (action.startsWith(Stomp.Commands.MESSAGE)) {
            try {
                StompJmsMessage msg = StompTranslator.convert(frame);
                addTransaction(msg);
                msg.setReadOnlyBody(true);
                msg.setReadOnlyProperties(true);
                StompJmsMessageListener l = this.listener;
                if (l != null) {
                    l.onMessage(msg);
                }
            } catch (JMSException e) {
                handleException(e);
            }
        } else if (action.startsWith(Stomp.Responses.RECEIPT)) {
            String id = frame.getHeaders().get(Stomp.Headers.Response.RECEIPT_ID);
            if (id != null) {
                synchronized (this.requests) {
                    SendRequest request = this.requests.remove(id);
                    if (request != null) {
                        request.put(id, frame);
                    } else {
                        handleException(new ProtocolException("Stomp Response without a valid receipt id: " + id + " for Frame " + frame));
                    }
                }
            } else {
                handleException(new ProtocolException("Stomp Response with no receipt id: " + frame));
            }
        } else if (action.startsWith(Stomp.Responses.ERROR)) {
            handleException(new ProtocolException("Received an error: " + frame.toString()));
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

    private void handleException(Exception e) {
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
            frame.getHeaders().put(Stomp.Headers.TRANSACTION, this.currentTransactionId);
        }
    }

}
