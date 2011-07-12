/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms.channel;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.BufferInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.stompjms.StompJmsDestination;
import org.fusesource.stompjms.StompJmsExceptionSupport;
import org.fusesource.stompjms.message.*;

import javax.jms.JMSException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import static org.fusesource.stompjms.channel.Stomp.*;
import static org.fusesource.hawtbuf.Buffer.*;


/**
 * Frame translator implementation that uses XStream to convert messages to and
 * from XML and JSON
 */
public class StompTranslator {


//    public static StompJmsDestination getDestination(StompFrame frame) throws JMSException {
//        final AsciiBuffer destination = frame.headers.remove(DESTINATION);
//        return StompJmsDestination.createDestination(destination.toString());
//    }
//
//    public static StompJmsDestination getReplyTo(StompFrame frame) throws JMSException {
//        final AsciiBuffer destination = frame.headers.remove(REPLY_TO);
//        return StompJmsDestination.createDestination(destination.toString());
//    }

    public static Object readObjectFromString(String text) {
        HierarchicalStreamReader in = new JettisonMappedXmlDriver().createReader(new StringReader(text));
        XStream xs = new XStream();
        return xs.unmarshal(in);
    }

    public static Object readObjectFromBuffer(Buffer buffer) {
        BufferInputStream bufferIn = new BufferInputStream(buffer);
        HierarchicalStreamReader in = new JettisonMappedXmlDriver().createReader(bufferIn);
        XStream xs = new XStream();
        return xs.unmarshal(in);
    }

    public static String writeStringFromObject(Object object) {
        StringWriter buffer = new StringWriter();
        HierarchicalStreamWriter out = new JettisonMappedXmlDriver().createWriter(buffer);
        XStream xs = new XStream();
        xs.marshal(object, out);
        return buffer.toString();
    }

    public static Buffer writeBufferFromObject(Object object) {
        DataByteArrayOutputStream buffer = new DataByteArrayOutputStream();
        HierarchicalStreamWriter out = new JettisonMappedXmlDriver().createWriter(buffer);
        XStream xs = new XStream();
        xs.marshal(object, out);
        return buffer.toBuffer();
    }

//    public static StompFrame convert(StompJmsMessage message) throws JMSException {
//        message.storeContent();
////        StompFrame command = message.getFrame().clone();
////        command.setAction(MESSAGE);
////        command.headers.put(TRANSFORMATION, message.getMsgType().buffer);
//        return command;
//    }

    public static StompJmsMessage convert(StompFrame frame) throws JMSException {
        Map<AsciiBuffer, AsciiBuffer> headers = frame.headers;
        StompJmsMessage result = null;
        AsciiBuffer type = headers.get(TRANSFORMATION);
        if (type != null) {
            switch (StompJmsMessage.JmsMsgType.valueOf(type.toString())) {
                case BYTES:
                    result = new StompJmsBytesMessage();
                    break;
                case MAP:
                    result = new StompJmsMapMessage();
                    break;
                case OBJECT:
                    result = new StompJmsObjectMessage();
                    break;
                case STREAM:
                    result = new StompJmsStreamMessage();
                    break;
                case MESSAGE:
                    result = new StompJmsMessage();
                    break;
                default:
                    result = new StompJmsTextMessage();
            }
        }
        if (result == null) {
            result = new StompJmsMessage();
        }
        result.setFrame(frame);
        return result;
    }

    static public String toString(AsciiBuffer buffer) {
        if( buffer == null ) {
            return null;
        } else {
            return buffer.toString();
        }
    }

//    public static void copyJmsHeaders(StompFrame from, StompJmsMessage to) throws JMSException {
//        final Map<AsciiBuffer, AsciiBuffer> headers = new HashMap<AsciiBuffer, AsciiBuffer>(from.getHeaders());
//        final AsciiBuffer destination = headers.remove(DESTINATION);
//        if (destination != null) {
//            to.setJMSDestination(StompJmsDestination.createDestination(destination.toString()));
//        }
//        AsciiBuffer replyTo = headers.remove(REPLY_TO);
//        if (replyTo != null) {
//            to.setJMSReplyTo(StompJmsDestination.createDestination(replyTo.toString()));
//        }
//
//        // the standard JMS headers
//
//        to.setJMSMessageID(toString(headers.remove(MESSAGE_ID)));
//        to.setJMSCorrelationID(toString(headers.remove(CORRELATION_ID)));
//
//        AsciiBuffer o = headers.remove(EXPIRATION_TIME);
//        if (o != null) {
//            to.setJMSExpiration(Long.parseLong(o.toString()));
//        }
//
//        o = headers.remove(TIMESTAMP);
//        if (o != null) {
//            to.setJMSTimestamp(Long.parseLong(o.toString()));
//        }
//
//        o = headers.remove(PRIORITY);
//        if (o != null) {
//            to.setJMSPriority(Integer.parseInt(o.toString()));
//        }
//
//        o = headers.remove(REDELIVERED);
//        if (o != null) {
//            to.setJMSRedelivered(Boolean.parseBoolean(o.toString()));
//        }
//
//        o = headers.remove(TYPE);
//        if (o != null) {
//            to.setJMSType(o.toString());
//        }
//
//        o = headers.remove(PERSISTENT);
//        if (o != null) {
//            to.setPersistent(Boolean.parseBoolean(o.toString()));
//        }
//
//        // Stomp specific headers
//        headers.remove(RECEIPT_REQUESTED);
//
//        o = headers.get(SUBSCRIPTION);
//        if (o != null) {
//            to.setConsumerId(o.toString());
//        }
//
//        //set the properties
//        o = headers.remove(PROPERTIES);
//        if (o != null) {
//            Map<String, Object> props = (Map<String, Object>) readObjectFromString(o.toString());
//            to.setProperties(props);
//        }
//    }
//
//    public static Map<String, Object> getJmsHeaders(StompFrame frame) {
//        final Map<String, Object> headers = frame != null ? new HashMap<String, Object>(frame.getHeaders())
//                : new HashMap<String, Object>();
//        headers.remove(DESTINATION);
//        headers.remove(REPLY_TO);
//        headers.remove(MESSAGE_ID);
//        headers.remove(CORRELATION_ID);
//        headers.remove(EXPIRATION_TIME);
//        headers.remove(TIMESTAMP);
//        headers.remove(PRIORITY);
//        headers.remove(REDELIVERED);
//        headers.remove(TYPE);
//        headers.remove(PERSISTENT);
//        headers.remove(RECEIPT_REQUESTED);
//        return headers;
//    }
//
//    public static void copyJmsHeaders(StompJmsMessage from, StompFrame to) throws IOException {
//        final Map<AsciiBuffer, AsciiBuffer> headers = to.getHeaders();
//        headers.put(DESTINATION, from.getJMSDestination().toString());
//        headers.put(MESSAGE_ID, from.getJMSMessageID());
//
//        if (from.getJMSCorrelationID() != null) {
//            headers.put(CORRELATION_ID, from.getJMSCorrelationID());
//        }
//        headers.put(EXPIRATION_TIME, "" + from.getJMSExpiration());
//
//        if (from.getJMSRedelivered()) {
//            headers.put(REDELIVERED, "true");
//        }
//        headers.put(PRIORITY, "" + from.getJMSPriority());
//
//        if (from.getJMSReplyTo() != null) {
//            headers.put(REPLY_TO, from.getJMSReplyTo().toString());
//        }
//        headers.put(TIMESTAMP, "" + from.getJMSTimestamp());
//
//        if (from.getJMSType() != null) {
//            headers.put(TYPE, from.getJMSType());
//        }
//
//        headers.put(CONTENT_TYPE, from.getJMSXMimeType());
//
//
//        // now lets add all the message headers
//        final Map<String, Object> properties = from.getProperties();
//        if (properties != null && properties.isEmpty() == false) {
//            String str = writeStringFromObject(properties);
//            headers.put(PROPERTIES, str);
//        }
//    }

}
