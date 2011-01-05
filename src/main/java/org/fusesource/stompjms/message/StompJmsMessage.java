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
package org.fusesource.stompjms.message;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.stompjms.StompJmsDestination;
import org.fusesource.stompjms.StompJmsExceptionSupport;
import org.fusesource.stompjms.channel.StompFrame;
import org.fusesource.stompjms.channel.StompTranslator;
import org.fusesource.stompjms.channel.TypeConversionSupport;
import org.fusesource.stompjms.util.Callback;
import org.fusesource.stompjms.util.PropertyExpression;

import javax.jms.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class StompJmsMessage implements javax.jms.Message {
  
    private static final Map<String, PropertySetter> JMS_PROPERTY_SETERS = new HashMap<String, PropertySetter>();

    public static enum JmsMsgType{
        MESSAGE,BYTES,MAP,OBJECT,STREAM,TEXT
    }
    protected transient Callback<StompJmsMessage> acknowledgeCallback;
    protected StompJmsDestination destination;
    protected StompJmsDestination replyTo;
    protected String messageId;
    protected String correlationId;
    protected long timestamp;
    protected long expiration;
    protected int redeliveryCounter;
    protected String consumerId;
    protected String type;
    protected int priority;
    protected boolean persistent;
    protected boolean readOnlyBody;
    protected boolean readOnlyProperties;
    protected Map<String, Object> properties;
    protected String transactionId;
    protected StompFrame frame = new StompFrame();
    private Buffer content;
    
    public StompJmsMessage copy() throws JMSException {
        StompJmsMessage copy = new StompJmsMessage();
        copy(copy);
        return copy;
    }
    
    public JmsMsgType getMsgType() {
        return JmsMsgType.MESSAGE;
    }

    protected void copy(StompJmsMessage copy) {
        copy.destination=this.destination;
        copy.replyTo=this.replyTo;
        copy.messageId=this.messageId;
        copy.correlationId=this.correlationId;
        copy.timestamp=this.timestamp;
        copy.expiration=this.expiration;
        copy.type=this.type;
        copy.priority=this.priority;
        copy.persistent=this.persistent;
        copy.readOnlyBody=this.readOnlyBody;
        copy.readOnlyProperties=this.readOnlyBody;
        if (this.properties != null) {
            copy.properties=new HashMap<String,Object>(this.properties);
        }
        copy.frame=this.frame;
        copy.content=this.content;
        copy.acknowledgeCallback = this.acknowledgeCallback;
        copy.transactionId=this.transactionId;
        //don't copy redilveryCounter
    }

    @Override
    public int hashCode() {
        String id = getJMSMessageID();
        if (id != null) {
            return id.hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        StompJmsMessage msg = (StompJmsMessage) o;
        String oMsg = msg.getJMSMessageID();
        String thisMsg = this.getJMSMessageID();
        return thisMsg != null && oMsg != null && oMsg.equals(thisMsg);
    }

    public void acknowledge() throws JMSException {
        if (acknowledgeCallback != null) {
            try {
                acknowledgeCallback.execute(this);
            } catch (Throwable e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }
    
    public Buffer getContent() {
        return this.content;
    }
    
    public void setContent(Buffer content) {
        this.content=content;
    }

    public void clearBody() throws JMSException {
        if (this.frame != null) {
            this.frame.clearContent();
        }
        setContent(null);
        readOnlyBody = false;
    }
    
    public void setReadOnlyBody(boolean readOnlyBody) {
        this.readOnlyBody = readOnlyBody;
    }
    
    public void setReadOnlyProperties(boolean readOnlyProperties) {
        this.readOnlyProperties = readOnlyProperties;
    }
    
    
    public String getJMSMessageID() {
        return this.messageId;
    }

    /**
     * Seems to be invalid because the parameter doesn't initialize MessageId
     * instance variables ProducerId and ProducerSequenceId
     *
     * @param value
     * @throws JMSException
     */
    public void setJMSMessageID(String value){
        this.messageId=value;
    }

    
    public long getJMSTimestamp() {
        return this.timestamp;
    }

    public void setJMSTimestamp(long timestamp) {
        this.timestamp=timestamp;
    }

    public String getJMSCorrelationID() {
        return this.correlationId;
    }

    public void setJMSCorrelationID(String correlationId) {
        this.correlationId=correlationId;
    }

    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        return encodeString(getJMSCorrelationID());
    }

    public void setJMSCorrelationIDAsBytes(byte[] correlationId) throws JMSException {
        setJMSCorrelationID(decodeString(correlationId));
    }

    public String getJMSXMimeType() {
        return "jms/message";
    }
    
    public boolean isPersistent() {
        return persistent;
    }
    
    public void setPersistent(boolean value) {
        this.persistent = value;
    }

    protected static String decodeString(byte[] data) throws JMSException {
        try {
            if (data == null) {
                return null;
            }
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new JMSException("Invalid UTF-8 encoding: " + e.getMessage());
        }
    }

    protected static byte[] encodeString(String data) throws JMSException {
        try {
            if (data == null) {
                return null;
            }
            return data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new JMSException("Invalid UTF-8 encoding: " + e.getMessage());
        }
    }

    public Destination getJMSReplyTo() {
        return this.replyTo;
    }

    public void setJMSReplyTo(Destination destination) throws JMSException {
        this.replyTo=StompJmsMessageTransformation.transformDestination(destination);
    }
    
    public void setJMSReplyTo(StompJmsDestination destination) {
        this.replyTo=destination;
    }
    
    public StompJmsDestination getStompJmsReplyTo() {
        return this.replyTo;
    }

    public Destination getJMSDestination() {
        return this.destination;
    }
    
    public StompJmsDestination getStompJmsDestination() {
        return this.destination;
    }

    public void setJMSDestination(Destination destination) throws JMSException {
        this.destination=StompJmsMessageTransformation.transformDestination(destination);
    }
    
    public void setJMSDestination(StompJmsDestination destination)  {
        this.destination=destination;
    }

    public int getJMSDeliveryMode() {
        return this.isPersistent() ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
    }

    public void setJMSDeliveryMode(int mode) {
        this.setPersistent(mode == DeliveryMode.PERSISTENT);
    }
    
    public boolean isRedelivered() {
        return redeliveryCounter > 0;
    }

    public void setRedelivered(boolean redelivered) {
        if (redelivered) {
            if (!isRedelivered()) {
                setRedeliveryCounter(1);
            }
        } else {
            if (isRedelivered()) {
                setRedeliveryCounter(0);
            }
        }
    }

    public void incrementRedeliveryCounter() {
        redeliveryCounter++;
    }
    
    public int getRedeliveryCounter() {
        return redeliveryCounter;
    }

    public void setRedeliveryCounter(int deliveryCounter) {
        this.redeliveryCounter = deliveryCounter;
    }

    public boolean getJMSRedelivered() {
        return this.isRedelivered();
    }

    public void setJMSRedelivered(boolean redelivered) {
        this.setRedelivered(redelivered);
    }

    public String getJMSType() {
        return this.type;
    }

    public void setJMSType(String type) {
        this.type=type;
    }

    public long getJMSExpiration() {
        return this.expiration;
    }

    public void setJMSExpiration(long expiration) {
        this.expiration=expiration;
    }

    public int getJMSPriority() {
        return this.priority;
    }

    public void setJMSPriority(int priority) {
        this.priority=priority;
    }

    public Map<String, Object> getProperties() throws IOException {
        if (properties == null) {
            if (this.frame== null) {
                return Collections.emptyMap();
            }
            properties = StompTranslator.getJmsHeaders(this.frame);
        }
        return Collections.unmodifiableMap(properties);
    }

    public void clearProperties() {
        if (this.frame != null) {
            this.frame.getHeaders().clear();
        }
        properties=null;
    }

    public void setProperty(String name, Object value) throws IOException {
        lazyCreateProperties();
        properties.put(name, value);
    }
    
    public void removeProperty(String name) throws IOException {
        lazyCreateProperties();
        properties.remove(name);
    }

    protected void lazyCreateProperties() throws IOException {
        if (properties == null) {
            if (this.frame == null) {
                properties = new HashMap<String, Object>();
            } else {
                properties = StompTranslator.getJmsHeaders(this.frame);
            }
        }
    }
    public boolean propertyExists(String name) throws JMSException {
        try {
            return (this.getProperties().containsKey(name) || getObjectProperty(name)!= null);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    public Enumeration getPropertyNames() throws JMSException {
        try {
            Vector<String> result = new Vector<String>(this.getProperties().keySet());
            return result.elements();
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    /**
     * return all property names, including standard JMS properties and JMSX properties
     * @return  Enumeration of all property names on this message
     * @throws JMSException
     */
    public Enumeration getAllPropertyNames() throws JMSException {
        try {
            Vector<String> result = new Vector<String>(this.getProperties().keySet());
            result.addAll(JMS_PROPERTY_SETERS.keySet());
            return result.elements();
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
    }

    interface PropertySetter {

        void set(StompJmsMessage message, Object value) throws MessageFormatException;
    }

    static {
        JMS_PROPERTY_SETERS.put("JMSXDeliveryCount", new PropertySetter() {
            public void set(StompJmsMessage message, Object value) throws MessageFormatException {
                Integer rc = (Integer) TypeConversionSupport.convert(value, Integer.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSXDeliveryCount cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setRedeliveryCounter(rc.intValue() - 1);
            }
        });
       
        JMS_PROPERTY_SETERS.put("JMSCorrelationID", new PropertySetter() {
            public void set(StompJmsMessage message, Object value) throws MessageFormatException {
                String rc = (String) TypeConversionSupport.convert(value, String.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSCorrelationID cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSCorrelationID(rc);
            }
        });
        JMS_PROPERTY_SETERS.put("JMSDeliveryMode", new PropertySetter() {
            public void set(StompJmsMessage message, Object value) throws MessageFormatException {
                Integer rc = (Integer) TypeConversionSupport.convert(value, Integer.class);
                if (rc == null) {
                    Boolean bool = (Boolean) TypeConversionSupport.convert(value, Boolean.class);
                    if (bool == null) {
                        throw new MessageFormatException("Property JMSDeliveryMode cannot be set from a " + value.getClass().getName() + ".");
                    }
                    else {
                        rc = bool.booleanValue() ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
                    }
                }
                ((StompJmsMessage) message).setJMSDeliveryMode(rc);
            }
        });
        JMS_PROPERTY_SETERS.put("JMSExpiration", new PropertySetter() {
            public void set(StompJmsMessage message, Object value) throws MessageFormatException {
                Long rc = (Long) TypeConversionSupport.convert(value, Long.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSExpiration cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSExpiration(rc.longValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSPriority", new PropertySetter() {
            public void set(StompJmsMessage message, Object value) throws MessageFormatException {
                Integer rc = (Integer) TypeConversionSupport.convert(value, Integer.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSPriority cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSPriority(rc.intValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSRedelivered", new PropertySetter() {
            public void set(StompJmsMessage message, Object value) throws MessageFormatException {
                Boolean rc = (Boolean) TypeConversionSupport.convert(value, Boolean.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSRedelivered cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSRedelivered(rc.booleanValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSReplyTo", new PropertySetter() {
            public void set(StompJmsMessage message, Object value) throws MessageFormatException {
                StompJmsDestination rc = (StompJmsDestination) TypeConversionSupport.convert(value, StompJmsDestination.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSReplyTo cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setJMSReplyTo(rc);
            }
        });
        JMS_PROPERTY_SETERS.put("JMSTimestamp", new PropertySetter() {
            public void set(StompJmsMessage message, Object value) throws MessageFormatException {
                Long rc = (Long) TypeConversionSupport.convert(value, Long.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSTimestamp cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSTimestamp(rc.longValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSType", new PropertySetter() {
            public void set(StompJmsMessage message, Object value) throws MessageFormatException {
                String rc = (String) TypeConversionSupport.convert(value, String.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSType cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSType(rc);
            }
        });
    }

    public void setObjectProperty(String name, Object value) throws JMSException {
        setObjectProperty(name, value, true);
    }

    public void setObjectProperty(String name, Object value, boolean checkReadOnly) throws JMSException {

        if (checkReadOnly) {
            checkReadOnlyProperties();
        }
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Property name cannot be empty or null");
        }

        checkValidObject(value);
        PropertySetter setter = JMS_PROPERTY_SETERS.get(name);

        if (setter != null && value != null) {
            setter.set(this, value);
        } else {
            try {
                this.setProperty(name, value);
            } catch (IOException e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    public void setProperties(Map<String,Object> properties) throws JMSException  {
        for (Iterator<Map.Entry<String,Object>> iter = properties.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String,Object> entry = iter.next();
            setObjectProperty((String) entry.getKey(), entry.getValue());
        }
    }

    protected void checkValidObject(Object value) throws MessageFormatException {

        boolean valid = value instanceof Boolean || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long;
        valid = valid || value instanceof Float || value instanceof Double || value instanceof Character
                || value instanceof String || value == null;

        if (!valid) {

            throw new MessageFormatException(
                    "Only objectified primitive objects and String types are allowed but was: " + value + " type: "
                            + value.getClass());

        }
    }
    
    
    
    

    public Object getObjectProperty(String name) throws JMSException {
        if (name == null) {
            throw new NullPointerException("Property name cannot be null");
        }

        // PropertyExpression handles converting message headers to properties.
        PropertyExpression expression = new PropertyExpression(name);
        return expression.evaluate(this);
    }

    public boolean getBooleanProperty(String name) throws JMSException {
        Object value = getObjectProperty(name);
        if (value == null) {
            return false;
        }
        Boolean rc = (Boolean) TypeConversionSupport.convert(value, Boolean.class);
        if (rc == null) {
            throw new MessageFormatException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a boolean");
        }
        return rc.booleanValue();
    }

    public byte getByteProperty(String name) throws JMSException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NumberFormatException("property " + name + " was null");
        }
        Byte rc = (Byte) TypeConversionSupport.convert(value, Byte.class);
        if (rc == null) {
            throw new MessageFormatException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a byte");
        }
        return rc.byteValue();
    }

    public short getShortProperty(String name) throws JMSException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NumberFormatException("property " + name + " was null");
        }
        Short rc = (Short) TypeConversionSupport.convert(value, Short.class);
        if (rc == null) {
            throw new MessageFormatException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a short");
        }
        return rc.shortValue();
    }

    public int getIntProperty(String name) throws JMSException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NumberFormatException("property " + name + " was null");
        }
        Integer rc = (Integer) TypeConversionSupport.convert(value, Integer.class);
        if (rc == null) {
            throw new MessageFormatException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as an integer");
        }
        return rc.intValue();
    }

    public long getLongProperty(String name) throws JMSException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NumberFormatException("property " + name + " was null");
        }
        Long rc = (Long) TypeConversionSupport.convert(value, Long.class);
        if (rc == null) {
            throw new MessageFormatException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a long");
        }
        return rc.longValue();
    }

    public float getFloatProperty(String name) throws JMSException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NullPointerException("property " + name + " was null");
        }
        Float rc = (Float) TypeConversionSupport.convert(value, Float.class);
        if (rc == null) {
            throw new MessageFormatException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a float");
        }
        return rc.floatValue();
    }

    public double getDoubleProperty(String name) throws JMSException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NullPointerException("property " + name + " was null");
        }
        Double rc = (Double) TypeConversionSupport.convert(value, Double.class);
        if (rc == null) {
            throw new MessageFormatException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a double");
        }
        return rc.doubleValue();
    }

    public String getStringProperty(String name) throws JMSException {
        Object value = getObjectProperty(name);
        if (value == null) {
            return null;
        }
        String rc = (String) TypeConversionSupport.convert(value, String.class);
        if (rc == null) {
            throw new MessageFormatException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a String");
        }
        return rc;
    }

    public void setBooleanProperty(String name, boolean value) throws JMSException {
        setBooleanProperty(name, value, true);
    }

    public void setBooleanProperty(String name, boolean value, boolean checkReadOnly) throws JMSException {
        setObjectProperty(name, Boolean.valueOf(value), checkReadOnly);
    }

    public void setByteProperty(String name, byte value) throws JMSException {
        setObjectProperty(name, Byte.valueOf(value));
    }

    public void setShortProperty(String name, short value) throws JMSException {
        setObjectProperty(name, Short.valueOf(value));
    }

    public void setIntProperty(String name, int value) throws JMSException {
        setObjectProperty(name, Integer.valueOf(value));
    }

    public void setLongProperty(String name, long value) throws JMSException {
        setObjectProperty(name, Long.valueOf(value));
    }

    public void setFloatProperty(String name, float value) throws JMSException {
        setObjectProperty(name, new Float(value));
    }

    public void setDoubleProperty(String name, double value) throws JMSException {
        setObjectProperty(name, new Double(value));
    }

    public void setStringProperty(String name, String value) throws JMSException {
        setObjectProperty(name, value);
    }

    private void checkReadOnlyProperties() throws MessageNotWriteableException {
        if (readOnlyProperties) {
            throw new MessageNotWriteableException("Message properties are read-only");
        }
    }

    protected void checkReadOnlyBody() throws MessageNotWriteableException {
        if (readOnlyBody) {
            throw new MessageNotWriteableException("Message body is read-only");
        }
    }

    public Callback<StompJmsMessage> getAcknowledgeCallback() {
        return acknowledgeCallback;
    }

    public void setAcknowledgeCallback(Callback<StompJmsMessage> acknowledgeCallback) {
        this.acknowledgeCallback = acknowledgeCallback;
    }

    /**
     * Send operation event listener. Used to get the message ready to be sent.
     * @throws JMSException 
     */
    public void onSend() throws JMSException {
        setReadOnlyBody(true);
        setReadOnlyProperties(true);
        storeContent();
    }
    
    /**
     * serialize the payload
     * @throws JMSException
     */
    public void storeContent() throws JMSException{
    }

    /**
     * @return the consumerId
     */
    public String getConsumerId() {
        return this.consumerId;
    }

    /**
     * @param consumerId the consumerId to set
     */
    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    /**
     * @return the transactionId
     */
    public String getTransactionId() {
        return this.transactionId;
    }

    /**
     * @param transactionId the transactionId to set
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
