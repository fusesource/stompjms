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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
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

/**
 * Frame translator implementation that uses XStream to convert messages to and
 * from XML and JSON
 * 
 */
public class StompTranslator {

    

    public static StompJmsDestination getDestination(StompFrame frame) throws JMSException {
        final String destination = frame.getHeaders().remove(Stomp.Headers.Send.DESTINATION);
        return StompJmsDestination.createDestination(destination);
    }

    public static StompJmsDestination getReplyTo(StompFrame frame) throws JMSException {
        final String destination = frame.getHeaders().remove(Stomp.Headers.Send.REPLY_TO);
        return StompJmsDestination.createDestination(destination);
    }
    
    public static Object readObjectFromString(String text){
        HierarchicalStreamReader in = new JettisonMappedXmlDriver().createReader(new StringReader(text));
        XStream xs = new XStream();
        return xs.unmarshal(in);
    }
    
    public static Object readObjectFromBuffer(Buffer buffer){
        BufferInputStream bufferIn = new BufferInputStream(buffer);
        HierarchicalStreamReader in = new JettisonMappedXmlDriver().createReader(bufferIn);
        XStream xs = new XStream();
        return xs.unmarshal(in);
    }
    
    public static String  writeStringFromObject(Object object){
        StringWriter buffer = new StringWriter();
        HierarchicalStreamWriter out  = new JettisonMappedXmlDriver().createWriter(buffer); 
        XStream xs = new XStream();
        xs.marshal(object, out);
        return buffer.toString();
    }
    
    public static Buffer writeBufferFromObject(Object object){
        DataByteArrayOutputStream buffer = new DataByteArrayOutputStream();
        HierarchicalStreamWriter out  = new JettisonMappedXmlDriver().createWriter(buffer); 
        XStream xs = new XStream();
        xs.marshal(object, out);
        return buffer.toBuffer();
    }
    
    public static StompFrame convert(StompJmsMessage message) throws JMSException{
        StompFrame command = new StompFrame();
        command.setAction(Stomp.Responses.MESSAGE);
        try {
            copyJmsHeaders(message, command);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.create(e);
        }
        command.getHeaders().put(Stomp.Headers.TRANSFORMATION, message.getMsgType().toString());
        message.storeContent();
        Buffer buffer = message.getContent();
        command.setContent(buffer);
        return command;
    }
    
    public static StompJmsMessage convert(StompFrame frame) throws JMSException {
        Map<String,String> headers = frame.getHeaders();
        StompJmsMessage result = null;
        String type  = headers.get(Stomp.Headers.TRANSFORMATION);
        if (type != null) {
            switch (StompJmsMessage.JmsMsgType.valueOf(type)) {
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
        copyJmsHeaders(frame, result);
        result.setContent(frame.getContent());
        return result;
    }
    
    
    
    public static void copyJmsHeaders(StompFrame from, StompJmsMessage to) throws JMSException {
        final Map<String, String> headers = new HashMap<String, String>(from.getHeaders());
        final String destination = headers.remove(Stomp.Headers.Send.DESTINATION);
        if (destination != null) {
            to.setJMSDestination(StompJmsDestination.createDestination(destination));
        }
        String replyTo = headers.remove(Stomp.Headers.Send.REPLY_TO);
        if (replyTo != null) {
            to.setJMSReplyTo(StompJmsDestination.createDestination(replyTo));
        }

        // the standard JMS headers

        to.setJMSMessageID(headers.remove(Stomp.Headers.Message.MESSAGE_ID));
        to.setJMSCorrelationID(headers.remove(Stomp.Headers.Message.CORRELATION_ID));

        String o = headers.remove(Stomp.Headers.Message.EXPIRATION_TIME);
        if (o != null) {
            to.setJMSExpiration(Long.parseLong(o));
        }

        o = headers.remove(Stomp.Headers.Message.TIMESTAMP);
        if (o != null) {
            to.setJMSTimestamp(Integer.parseInt(o));
        }

        o = headers.remove(Stomp.Headers.Message.PRIORITY);
        if (o != null) {
            to.setJMSPriority(Integer.parseInt(o));
        }

        o = headers.remove(Stomp.Headers.Message.REDELIVERED);
        if (o != null) {
            to.setJMSRedelivered(Boolean.parseBoolean(o));
        }

        o = headers.remove(Stomp.Headers.Send.TYPE);
        if (o != null) {
            to.setJMSType(o);
        }

        o = headers.remove(Stomp.Headers.Send.PERSISTENT);
        if (o != null) {
            to.setPersistent(Boolean.parseBoolean(o));
        }

        // Stomp specific headers
        headers.remove(Stomp.Headers.RECEIPT_REQUESTED);
        
        o = headers.get(Stomp.Headers.Message.SUBSCRIPTION);
        if (o != null) {
            to.setConsumerId(o);
        }
        
        //set the properties
        o = headers.remove(Stomp.Headers.Message.PROPERTIES);
        if (o != null) {
            Map<String,Object> props = (Map<String, Object>) readObjectFromString(o);
            to.setProperties(props);
        }
    }

    public static Map<String, Object> getJmsHeaders(StompFrame frame) {
        final Map<String, Object> headers = frame != null ? new HashMap<String, Object>(frame.getHeaders())
                : new HashMap<String, Object>();
        headers.remove(Stomp.Headers.Send.DESTINATION);
        headers.remove(Stomp.Headers.Send.REPLY_TO);
        headers.remove(Stomp.Headers.Message.MESSAGE_ID);
        headers.remove(Stomp.Headers.Message.CORRELATION_ID);
        headers.remove(Stomp.Headers.Message.EXPIRATION_TIME);
        headers.remove(Stomp.Headers.Message.TIMESTAMP);
        headers.remove(Stomp.Headers.Message.PRIORITY);
        headers.remove(Stomp.Headers.Message.REDELIVERED);
        headers.remove(Stomp.Headers.Send.TYPE);
        headers.remove(Stomp.Headers.Send.PERSISTENT);
        headers.remove(Stomp.Headers.RECEIPT_REQUESTED);
        return headers;
    }
    
    public static void copyJmsHeaders(StompJmsMessage from, StompFrame to) throws IOException {
        final Map<String, String> headers = to.getHeaders();
        headers.put(Stomp.Headers.Message.DESTINATION, from.getJMSDestination().toString());
        headers.put(Stomp.Headers.Message.MESSAGE_ID, from.getJMSMessageID());

        if (from.getJMSCorrelationID() != null) {
            headers.put(Stomp.Headers.Message.CORRELATION_ID, from.getJMSCorrelationID());
        }
        headers.put(Stomp.Headers.Message.EXPIRATION_TIME, "" + from.getJMSExpiration());

        if (from.getJMSRedelivered()) {
            headers.put(Stomp.Headers.Message.REDELIVERED, "true");
        }
        headers.put(Stomp.Headers.Message.PRIORITY, "" + from.getJMSPriority());

        if (from.getJMSReplyTo() != null) {
            headers.put(Stomp.Headers.Message.REPLY_TO, from.getJMSReplyTo().toString());
        }
        headers.put(Stomp.Headers.Message.TIMESTAMP, "" + from.getJMSTimestamp());

        if (from.getJMSType() != null) {
            headers.put(Stomp.Headers.Message.TYPE, from.getJMSType());
        }

        headers.put(Stomp.Headers.CONTENT_TYPE,from.getJMSXMimeType());

                
        // now lets add all the message headers
        final Map<String, Object> properties = from.getProperties();
        if (properties != null && properties.isEmpty()==false) {
            String str = writeStringFromObject(properties);
            headers.put(Stomp.Headers.Message.PROPERTIES, str);
        }
    }

}
