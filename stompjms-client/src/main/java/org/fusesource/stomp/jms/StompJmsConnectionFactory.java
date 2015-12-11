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


import org.fusesource.stomp.jms.jndi.JNDIStorable;
import org.fusesource.stomp.jms.util.PropertyUtil;

import javax.jms.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Jms ConnectionFactory implementation
 */
public class StompJmsConnectionFactory extends JNDIStorable implements ConnectionFactory, QueueConnectionFactory,
        TopicConnectionFactory {
    private URI brokerURI;
    private URI localURI;
    private String username;
    private String password;
    boolean forceAsyncSend;
    boolean omitHost;
    String queuePrefix = "/queue/";
    String topicPrefix = "/topic/";
    String tempQueuePrefix = "/temp-queue/";
    String tempTopicPrefix = "/temp-topic/";
    long disconnectTimeout = 10000;
    StompJmsPrefetch prefetch = new StompJmsPrefetch();
    private int connectTimeoutMs;

    /**
     * Constructor
     */
    public StompJmsConnectionFactory() {
    }


    /**
     * Set properties
     *
     * @param props
     */
    public void setProperties(Properties props) {
        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        setProperties(map);
    }

    @Override
    public void setProperties(Map<String, String> map) {
        buildFromProperties(map);
    }

    /**
     * @param map
     */
    @Override
    protected void buildFromProperties(Map<String, String> map) {
        PropertyUtil.setProperties(this, map);
    }

    /**
     * @param map
     */
    @Override
    protected void populateProperties(Map<String, String> map) {
        try {
            Map<String, String> result = PropertyUtil.getProperties(this);
            map.putAll(result);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * @return a TopicConnection
     * @throws JMSException
     * @see javax.jms.TopicConnectionFactory#createTopicConnection()
     */
    public TopicConnection createTopicConnection() throws JMSException {
        return createTopicConnection( getUsername(), getPassword());
    }

    /**
     * @param userName
     * @param password
     * @return a TopicConnection
     * @throws JMSException
     * @see javax.jms.TopicConnectionFactory#createTopicConnection(java.lang.String, java.lang.String)
     */
    public TopicConnection createTopicConnection(String userName, String password) throws JMSException {
        try {
            StompJmsTopicConnection result = new StompJmsTopicConnection(this.brokerURI, this.localURI, userName,
                    password, connectTimeoutMs);
            PropertyUtil.setProperties(result, PropertyUtil.getProperties(this));
            return result;
        } catch (Exception e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    /**
     * @return a Connection
     * @throws JMSException
     * @see javax.jms.ConnectionFactory#createConnection()
     */
    public Connection createConnection() throws JMSException {
        return createConnection(getUsername(), getPassword());
    }

    /**
     * @param userName
     * @param password
     * @return Connection
     * @throws JMSException
     * @see javax.jms.ConnectionFactory#createConnection(java.lang.String, java.lang.String)
     */
    public Connection createConnection(String userName, String password) throws JMSException {
        try {
            StompJmsConnection result = new StompJmsConnection(this.brokerURI, this.localURI, userName, password,
                    connectTimeoutMs);
            PropertyUtil.setProperties(result, PropertyUtil.getProperties(this));
            return result;
        } catch (Exception e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    /**
     * @return a QueueConnection
     * @throws JMSException
     * @see javax.jms.QueueConnectionFactory#createQueueConnection()
     */
    public QueueConnection createQueueConnection() throws JMSException {
        return createQueueConnection(getUsername(), getPassword());
    }

    /**
     * @param userName
     * @param password
     * @return a QueueConnection
     * @throws JMSException
     * @see javax.jms.QueueConnectionFactory#createQueueConnection(java.lang.String, java.lang.String)
     */
    public QueueConnection createQueueConnection(String userName, String password) throws JMSException {
        try {
            StompJmsQueueConnection result = new StompJmsQueueConnection(this.brokerURI, this.localURI, userName,
                    password, connectTimeoutMs);
            PropertyUtil.setProperties(result, PropertyUtil.getProperties(this));
            return result;
        } catch (Exception e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }


    /**
     * @return the brokerURI
     */
    public String getBrokerURI() {
        return this.brokerURI != null ? this.brokerURI.toString() : "";
    }


    /**
     * @param brokerURI the brokerURI to set
     */
    public void setBrokerURI(String brokerURI) {
        if( brokerURI == null ) {
            throw new IllegalArgumentException("brokerURI cannot be null");
        }
        this.brokerURI = createURI(brokerURI);
    }


    /**
     * @return the localURI
     */
    public String getLocalURI() {
        return this.localURI != null ? this.localURI.toString() : "";
    }


    /**
     * @param localURI the localURI to set
     */
    public void setLocalURI(String localURI) {
        this.localURI = createURI(localURI);
    }

    private URI createURI(String name) {
        if (name != null && name.trim().isEmpty() == false) {
            try {
                return new URI(name);
            } catch (URISyntaxException e) {
                throw (IllegalArgumentException) new IllegalArgumentException("Invalid broker URI: " + name).initCause(e);
            }
        }
        return null;
    }


    /**
     * @return the username
     */
    public String getUsername() {
        return this.username;
    }


    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
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

    public boolean isForceAsyncSend() {
        return forceAsyncSend;
    }

    public void setForceAsyncSend(boolean forceAsyncSend) {
        this.forceAsyncSend = forceAsyncSend;
    }

    public boolean isOmitHost() {
        return omitHost;
    }

    public void setOmitHost(boolean omitHost) {
        this.omitHost = omitHost;
    }
    
    public String getQueuePrefix() {
        return queuePrefix;
    }

    public void setQueuePrefix(String queuePrefix) {
        this.queuePrefix = queuePrefix;
    }

    public String getTempQueuePrefix() {
        return tempQueuePrefix;
    }

    public void setTempQueuePrefix(String tempQueuePrefix) {
        this.tempQueuePrefix = tempQueuePrefix;
    }

    public String getTempTopicPrefix() {
        return tempTopicPrefix;
    }

    public void setTempTopicPrefix(String tempTopicPrefix) {
        this.tempTopicPrefix = tempTopicPrefix;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public long getDisconnectTimeout() {
        return disconnectTimeout;
    }

    public void setDisconnectTimeout(long disconnectTimeout) {
        this.disconnectTimeout = disconnectTimeout;
    }

    public StompJmsPrefetch getPrefetch() {
        return prefetch;
    }

    public void setPrefetch(StompJmsPrefetch prefetch) {
        this.prefetch = prefetch;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }
}
