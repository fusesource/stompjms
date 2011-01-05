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
package org.fusesource.stompjms;


import org.fusesource.stompjms.jndi.JNDIStorable;
import org.fusesource.stompjms.util.PropertyUtil;

import javax.jms.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Jms ConnectionFactory implementation
 * 
 */
public class StompJmsConnectionFactory extends JNDIStorable implements ConnectionFactory, QueueConnectionFactory,
TopicConnectionFactory {
    private URI brokerURI;
    private URI localURI;
    private String username;
    private String password;

    /**
     * Constructor
     */
    public StompJmsConnectionFactory() {
    }

   
    
    /**
     * Set properties
     * @param props
     */
    public void setProperties(Properties props) {
        Map<String,String> map = new HashMap<String, String>();
        for (Map.Entry<Object,Object> entry: props.entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        setProperties(map);
    }

    @Override
    public void setProperties(Map<String, String> map) {
        populateProperties(map);
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
        try {
            TopicConnection result = new StompJmsConnection(this.brokerURI,this.localURI,getUsername(),getPassword());
            Map <String,String>map = PropertyUtil.getProperties(this);
            PropertyUtil.setProperties(result, map);
            return result;
        } catch (Exception e) {
            throw StompJmsExceptionSupport.create(e);
        }
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
            TopicConnection result = new StompJmsConnection(this.brokerURI,this.localURI,userName,password);
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
        try {
            Connection result = new StompJmsConnection(this.brokerURI,this.localURI,getUsername(),getPassword());
            PropertyUtil.setProperties(result, PropertyUtil.getProperties(this));
            return result;
        } catch (Exception e) {
            throw StompJmsExceptionSupport.create(e);
        }
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
            Connection result = new StompJmsConnection(this.brokerURI,this.localURI,getUsername(),getPassword());
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
        try {
            QueueConnection result = new StompJmsConnection(this.brokerURI,this.localURI,getUsername(),getPassword());
            PropertyUtil.setProperties(result, PropertyUtil.getProperties(this));
            return result;
        } catch (Exception e) {
            throw StompJmsExceptionSupport.create(e);
        }
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
            QueueConnection result = new StompJmsConnection(this.brokerURI,this.localURI,userName,password);
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
        if (name != null && name.trim().isEmpty()==false) {
        try {
            return new URI(name);
        } catch (URISyntaxException e) {
            throw (IllegalArgumentException)new IllegalArgumentException("Invalid broker URI: " + name).initCause(e);
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

}
