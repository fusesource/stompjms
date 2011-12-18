/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stomp.jms.message;

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.stomp.jms.StompJmsConnection;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.StompJmsExceptionSupport;
import org.fusesource.stomp.codec.StompFrame;
import org.fusesource.stomp.jms.util.TypeConversionSupport;
import org.fusesource.stomp.jms.util.PropertyExpression;

import javax.jms.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.fusesource.hawtbuf.Buffer.ascii;
import static org.fusesource.stomp.client.Constants.*;
import static org.fusesource.stomp.codec.StompFrame.*;

public class StompJmsMessage implements javax.jms.Message {

    private static final Map<String, PropertySetter> JMS_PROPERTY_SETERS = new HashMap<String, PropertySetter>();

    private static HashSet<AsciiBuffer> RESERVED_HEADER_NAMES = new HashSet<AsciiBuffer>();
    static{
        RESERVED_HEADER_NAMES.add(DESTINATION);
        RESERVED_HEADER_NAMES.add(REPLY_TO);
        RESERVED_HEADER_NAMES.add(MESSAGE_ID);
        RESERVED_HEADER_NAMES.add(CORRELATION_ID);
        RESERVED_HEADER_NAMES.add(EXPIRATION_TIME);
        RESERVED_HEADER_NAMES.add(TIMESTAMP);
        RESERVED_HEADER_NAMES.add(PRIORITY);
        RESERVED_HEADER_NAMES.add(REDELIVERED);
        RESERVED_HEADER_NAMES.add(TYPE);
        RESERVED_HEADER_NAMES.add(PERSISTENT);
        RESERVED_HEADER_NAMES.add(RECEIPT_REQUESTED);
        RESERVED_HEADER_NAMES.add(TRANSFORMATION);
        RESERVED_HEADER_NAMES.add(SUBSCRIPTION);
        RESERVED_HEADER_NAMES.add(CONTENT_LENGTH);
    }

    public static enum JmsMsgType {
        MESSAGE("jms/message"),
        BYTES("jms/bytes-message"),
        MAP("jms/map-message"),
        OBJECT("jms/object-message"),
        STREAM("jms/stream-message"),
        TEXT("jms/text-message");

        public final AsciiBuffer buffer = new AsciiBuffer(this.name());
        public final AsciiBuffer mime;

        JmsMsgType(String mime){
            this.mime = (ascii(mime));
        }
    }

    protected transient Runnable acknowledgeCallback;
    protected transient StompJmsConnection connection;

    protected boolean readOnlyBody;
    protected boolean readOnlyProperties;
    protected Map<String, Object> properties;
    protected AsciiBuffer transactionId;
    protected StompFrame frame = new StompFrame(MESSAGE);
    protected int redeliveryCounter=0;

    public StompJmsMessage() {
        frame.headerMap().put(TRANSFORMATION, getMsgType().buffer);
    }

    public StompJmsMessage copy() throws JMSException {
        StompJmsMessage copy = new StompJmsMessage();
        copy(copy);
        return copy;
    }

    public JmsMsgType getMsgType() {
        return JmsMsgType.MESSAGE;
    }

    public StompFrame getFrame() {
        return frame;
    }

    public void setFrame(StompFrame frame) {
        this.frame = frame;
    }

    protected void copy(StompJmsMessage copy) {
        copy.readOnlyBody = this.readOnlyBody;
        copy.readOnlyProperties = this.readOnlyBody;
        if (this.properties != null) {
            copy.properties = new HashMap<String, Object>(this.properties);
        }
        copy.frame = frame.clone();
        copy.acknowledgeCallback = this.acknowledgeCallback;
        copy.transactionId = this.transactionId;
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
                acknowledgeCallback.run();
            } catch (Throwable e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    public Buffer getContent() {
        Buffer content = this.frame.content();
        if( content.isEmpty() ) {
            return null;
        } else {
            return content;
        }
    }

    public void setContent(Buffer content) {
        if( content == null ) {
            this.frame.content(NO_DATA);
        } else {
            this.frame.content(content);
        }
    }

    public void clearBody() throws JMSException {
        if (this.frame != null) {
            this.frame.content(StompFrame.NO_DATA);
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

    private String getStringHeader(AsciiBuffer key) {
        AsciiBuffer buffer = frame.headerMap().get(key);
        if( buffer == null ) {
            return null;
        } else {
            return buffer.toString();
        }
    }
    private void setStringHeader(AsciiBuffer key, String value) {
        if(value==null) {
            frame.headerMap().remove(key);
        } else {
            frame.headerMap().put(key, ascii(value));
        }
    }

    private byte[] getBytesHeader(AsciiBuffer key) {
        AsciiBuffer buffer = frame.headerMap().get(key);
        if( buffer == null ) {
            return null;
        } else {
            return buffer.deepCopy().data;
        }
    }
    private void setBytesHeader(AsciiBuffer key, byte[]  value) {
        if(value==null) {
            frame.headerMap().remove(key);
        } else {
            frame.headerMap().put(key, new Buffer(value).deepCopy().ascii());
        }
    }

    private Integer getIntegerHeader(AsciiBuffer key) {
        AsciiBuffer buffer = frame.headerMap().get(key);
        if( buffer == null ) {
            return null;
        } else {
            return Integer.parseInt(buffer.toString());
        }
    }
    private void setIntegerHeader(AsciiBuffer key, Integer value) {
        if(value==null) {
            frame.headerMap().remove(key);
        } else {
            frame.headerMap().put(key, ascii(value.toString()));
        }
    }

    private Long getLongHeader(AsciiBuffer key) {
        AsciiBuffer buffer = frame.headerMap().get(key);
        if( buffer == null ) {
            return null;
        } else {
            return Long.parseLong(buffer.toString());
        }
    }
    private void setLongHeader(AsciiBuffer key, Long value) {
        if(value==null) {
            frame.headerMap().remove(key);
        } else {
            frame.headerMap().put(key, ascii(value.toString()));
        }
    }

    private Boolean getBooleanHeader(AsciiBuffer key) {
        AsciiBuffer buffer = frame.headerMap().get(key);
        if( buffer == null ) {
            return null;
        } else {
            return Boolean.parseBoolean(buffer.toString());
        }
    }
    private void setBooleanHeader(AsciiBuffer key, Boolean value) {
        if(value==null) {
            frame.headerMap().remove(key);
        } else {
            frame.headerMap().put(key, value.booleanValue() ? TRUE : FALSE);
        }
    }

    private StompJmsDestination getDestinationHeader(AsciiBuffer key) throws InvalidDestinationException {
        AsciiBuffer buffer = frame.headerMap().get(key);
        if( buffer == null ) {
            return null;
        } else {
            return StompJmsDestination.createDestination(connection, buffer.toString());
        }
    }
    private void setDestinationHeader(AsciiBuffer key, StompJmsDestination value) {
        if(value==null) {
            frame.headerMap().remove(key);
        } else {
            frame.headerMap().put(key, ascii(value.toString()));
        }
    }

    public AsciiBuffer getMessageID() {
        return frame.headerMap().get(MESSAGE_ID);
    }

    public String getJMSMessageID() {
        return getStringHeader(MESSAGE_ID);
    }

    /**
     * Seems to be invalid because the parameter doesn't initialize MessageId
     * instance variables ProducerId and ProducerSequenceId
     *
     * @param value
     * @throws JMSException
     */
    public void setJMSMessageID(String value) {
        setStringHeader(MESSAGE_ID, value);
    }
    public void setMessageID(AsciiBuffer value) {
        frame.headerMap().put(MESSAGE_ID, value);
    }


    private <T> T or(T value, T other) {
        if(value!=null) {
            return value;
        } else {
            return other;
        }
    }

    public long getJMSTimestamp() {
        return or(getLongHeader(TIMESTAMP), 0L);
    }

    public void setJMSTimestamp(long timestamp) {
        setLongHeader(TIMESTAMP, timestamp==0? null : timestamp);
    }

    public String getJMSCorrelationID() {
        return getStringHeader(CORRELATION_ID);
    }

    public void setJMSCorrelationID(String correlationId) {
        setStringHeader(CORRELATION_ID, correlationId);
    }

    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        return getBytesHeader(CORRELATION_ID);
    }

    public void setJMSCorrelationIDAsBytes(byte[] correlationId) throws JMSException {
        setBytesHeader(CORRELATION_ID, correlationId);
    }


    public boolean isPersistent() {
        return or(getBooleanHeader(PERSISTENT), false);
    }

    public void setPersistent(boolean value) {
        setBooleanHeader(PERSISTENT, value ? true : null);
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

    public Destination getJMSReplyTo() throws InvalidDestinationException {
        return getStompJmsReplyTo();
    }

    public void setJMSReplyTo(Destination destination) throws JMSException {
        setJMSReplyTo(StompJmsMessageTransformation.transformDestination(connection, destination));
    }

    public void setJMSReplyTo(StompJmsDestination destination) {
        setDestinationHeader(REPLY_TO, destination);
    }

    public StompJmsDestination getStompJmsReplyTo() throws InvalidDestinationException {
        return getDestinationHeader(REPLY_TO);
    }

    public Destination getJMSDestination() throws InvalidDestinationException {
        return getStompJmsDestination();
    }

    public StompJmsDestination getStompJmsDestination() throws InvalidDestinationException {
        return getDestinationHeader(DESTINATION);
    }

    public void setJMSDestination(Destination destination) throws JMSException {
        setJMSDestination(StompJmsMessageTransformation.transformDestination(connection, destination));
    }

    public void setJMSDestination(StompJmsDestination destination) {
        setDestinationHeader(DESTINATION, destination);
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
        return getStringHeader(TYPE);
    }

    public void setJMSType(String type) {
        setStringHeader(TYPE, type);
    }

    public long getJMSExpiration() {
        return or(getLongHeader(EXPIRATION_TIME), 0L);
    }

    public void setJMSExpiration(long expiration) {
        setLongHeader(EXPIRATION_TIME, expiration == 0 ? null : expiration);
    }

    public int getJMSPriority() {
        return or(getIntegerHeader(PRIORITY), 4);
    }

    public void setJMSPriority(int priority) {
        setIntegerHeader(PRIORITY, priority == 4 ? null : priority);
    }

    public Map<String, Object> getProperties() throws IOException {
        lazyCreateProperties();
        return Collections.unmodifiableMap(properties);
    }

    public void clearProperties() {
        if (this.frame != null) {
            this.frame.headerMap().clear();
        }
        properties = null;
    }

    public void setProperty(String name, Object value) throws IOException {
        lazyCreateProperties();
        properties.put(name, value);
        this.frame.headerMap().put(encodeHeader(name), encodeHeader(value.toString()));
    }

    public void removeProperty(String name) throws IOException {
        lazyCreateProperties();
        properties.remove(name);
    }

    protected void lazyCreateProperties() throws IOException {
        if (properties == null) {
            if (this.frame != null) {
                properties = new HashMap<String, Object>(this.frame.headerMap().size());
                for (Map.Entry<AsciiBuffer, AsciiBuffer> entry: this.frame.headerMap().entrySet()){
                    if( !RESERVED_HEADER_NAMES.contains(entry.getKey()) ) {
                        properties.put(decodeHeader(entry.getKey()), decodeHeader(entry.getValue()));
                    }
                }
            } else {
                properties = new HashMap<String, Object>();
            }
        }
    }

    public boolean propertyExists(String name) throws JMSException {
        try {
            return (this.getProperties().containsKey(name) || getObjectProperty(name) != null);
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
     *
     * @return Enumeration of all property names on this message
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

        void set(StompJmsConnection connection, StompJmsMessage message, Object value) throws MessageFormatException;
    }

    static {
        JMS_PROPERTY_SETERS.put("JMSXDeliveryCount", new PropertySetter() {
            public void set(StompJmsConnection connection, StompJmsMessage message, Object value) throws MessageFormatException {
                Integer rc = (Integer) TypeConversionSupport.convert(connection, value, Integer.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSXDeliveryCount cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setRedeliveryCounter(rc.intValue() - 1);
            }
        });

        JMS_PROPERTY_SETERS.put("JMSCorrelationID", new PropertySetter() {
            public void set(StompJmsConnection connection, StompJmsMessage message, Object value) throws MessageFormatException {
                String rc = (String) TypeConversionSupport.convert(connection, value, String.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSCorrelationID cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSCorrelationID(rc);
            }
        });
        JMS_PROPERTY_SETERS.put("JMSDeliveryMode", new PropertySetter() {
            public void set(StompJmsConnection connection, StompJmsMessage message, Object value) throws MessageFormatException {
                Integer rc = (Integer) TypeConversionSupport.convert(connection, value, Integer.class);
                if (rc == null) {
                    Boolean bool = (Boolean) TypeConversionSupport.convert(connection, value, Boolean.class);
                    if (bool == null) {
                        throw new MessageFormatException("Property JMSDeliveryMode cannot be set from a " + value.getClass().getName() + ".");
                    } else {
                        rc = bool.booleanValue() ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
                    }
                }
                ((StompJmsMessage) message).setJMSDeliveryMode(rc);
            }
        });
        JMS_PROPERTY_SETERS.put("JMSExpiration", new PropertySetter() {
            public void set(StompJmsConnection connection, StompJmsMessage message, Object value) throws MessageFormatException {
                Long rc = (Long) TypeConversionSupport.convert(connection, value, Long.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSExpiration cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSExpiration(rc.longValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSPriority", new PropertySetter() {
            public void set(StompJmsConnection connection, StompJmsMessage message, Object value) throws MessageFormatException {
                Integer rc = (Integer) TypeConversionSupport.convert(connection, value, Integer.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSPriority cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSPriority(rc.intValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSRedelivered", new PropertySetter() {
            public void set(StompJmsConnection connection, StompJmsMessage message, Object value) throws MessageFormatException {
                Boolean rc = (Boolean) TypeConversionSupport.convert(connection, value, Boolean.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSRedelivered cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSRedelivered(rc.booleanValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSReplyTo", new PropertySetter() {
            public void set(StompJmsConnection connection, StompJmsMessage message, Object value) throws MessageFormatException {
                StompJmsDestination rc = (StompJmsDestination) TypeConversionSupport.convert(connection, value, StompJmsDestination.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSReplyTo cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setJMSReplyTo(rc);
            }
        });
        JMS_PROPERTY_SETERS.put("JMSTimestamp", new PropertySetter() {
            public void set(StompJmsConnection connection, StompJmsMessage message, Object value) throws MessageFormatException {
                Long rc = (Long) TypeConversionSupport.convert(connection, value, Long.class);
                if (rc == null) {
                    throw new MessageFormatException("Property JMSTimestamp cannot be set from a " + value.getClass().getName() + ".");
                }
                ((StompJmsMessage) message).setJMSTimestamp(rc.longValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSType", new PropertySetter() {
            public void set(StompJmsConnection connection, StompJmsMessage message, Object value) throws MessageFormatException {
                String rc = (String) TypeConversionSupport.convert(connection, value, String.class);
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
            setter.set(connection, this, value);
        } else {
            try {
                this.setProperty(name, value);
            } catch (IOException e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    public void setProperties(Map<String, Object> properties) throws JMSException {
        for (Iterator<Map.Entry<String, Object>> iter = properties.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, Object> entry = iter.next();
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
        Boolean rc = (Boolean) TypeConversionSupport.convert(connection, value, Boolean.class);
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
        Byte rc = (Byte) TypeConversionSupport.convert(connection, value, Byte.class);
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
        Short rc = (Short) TypeConversionSupport.convert(connection, value, Short.class);
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
        Integer rc = (Integer) TypeConversionSupport.convert(connection, value, Integer.class);
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
        Long rc = (Long) TypeConversionSupport.convert(connection, value, Long.class);
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
        Float rc = (Float) TypeConversionSupport.convert(connection, value, Float.class);
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
        Double rc = (Double) TypeConversionSupport.convert(connection, value, Double.class);
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
        String rc = (String) TypeConversionSupport.convert(connection, value, String.class);
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

    public Runnable getAcknowledgeCallback() {
        return acknowledgeCallback;
    }

    public void setAcknowledgeCallback(Runnable acknowledgeCallback) {
        this.acknowledgeCallback = acknowledgeCallback;
    }

    /**
     * Send operation event listener. Used to get the message ready to be sent.
     *
     * @throws JMSException
     */
    public void onSend() throws JMSException {
        setReadOnlyBody(true);
        setReadOnlyProperties(true);
        storeContent();
    }

    /**
     * serialize the payload
     *
     * @throws JMSException
     */
    public void storeContent() throws JMSException {
    }

    /**
     * @return the consumerId
     */
    public AsciiBuffer getConsumerId() {
        return this.frame.headerMap().get(SUBSCRIPTION);
    }

    /**
     * @return the transactionId
     */
    public AsciiBuffer getTransactionId() {
        return this.transactionId;
    }

    /**
     * @param transactionId the transactionId to set
     */
    public void setTransactionId(AsciiBuffer transactionId) {
        this.transactionId = transactionId;
    }
}
