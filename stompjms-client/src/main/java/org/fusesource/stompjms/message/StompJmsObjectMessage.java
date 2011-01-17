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
import org.fusesource.stompjms.channel.StompTranslator;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import java.io.Serializable;

/**
 * An <CODE>ObjectMessage</CODE> object is used to send a message that contains
 * a serializable object in the Java programming language ("Java object"). It
 * inherits from the <CODE>Message</CODE> interface and adds a body containing a
 * single reference to an object. Only <CODE>Serializable</CODE> Java objects
 * can be used.
 * <p/>
 * <p/>
 * If a collection of Java objects must be sent, one of the
 * <CODE>Collection</CODE> classes provided since JDK 1.2 can be used.
 * <p/>
 * <p/>
 * When a client receives an <CODE>ObjectMessage</CODE>, it is in read-only
 * mode. If a client attempts to write to the message at this point, a
 * <CODE>MessageNotWriteableException</CODE> is thrown. If
 * <CODE>clearBody</CODE> is called, the message can now be both read from and
 * written to.
 *
 * @openwire:marshaller code="26"
 * @see javax.jms.Session#createObjectMessage()
 * @see javax.jms.Session#createObjectMessage(Serializable)
 * @see javax.jms.BytesMessage
 * @see javax.jms.MapMessage
 * @see javax.jms.Message
 * @see javax.jms.StreamMessage
 * @see javax.jms.TextMessage
 */
public class StompJmsObjectMessage extends StompJmsMessage implements ObjectMessage {
    protected transient Serializable object;

    public JmsMsgType getMsgType() {
        return JmsMsgType.OBJECT;
    }

    public StompJmsMessage copy() throws JMSException {
        StompJmsObjectMessage copy = new StompJmsObjectMessage();
        copy(copy);
        return copy;
    }

    private void copy(StompJmsObjectMessage copy) throws JMSException {
        super.copy(copy);
        storeContent();
        copy.object = null;
    }

    public void storeContent() throws JMSException {
        Buffer buffer = getContent();
        if (buffer == null && object != null) {
            setContent(StompTranslator.writeBufferFromObject(object));
        }
    }


    public String getJMSXMimeType() {
        return "jms/object-message";
    }

    /**
     * Clears out the message body. Clearing a message's body does not clear its
     * header values or property entries.
     * <p/>
     * <p/>
     * If this message body was read-only, calling this method leaves the
     * message body in the same state as an empty body in a newly created
     * message.
     *
     * @throws JMSException if the JMS provider fails to clear the message body due to
     *                      some internal error.
     */

    public void clearBody() throws JMSException {
        super.clearBody();
        this.object = null;
    }

    /**
     * Sets the serializable object containing this message's data. It is
     * important to note that an <CODE>ObjectMessage</CODE> contains a snapshot
     * of the object at the time <CODE>setObject()</CODE> is called; subsequent
     * modifications of the object will have no effect on the
     * <CODE>ObjectMessage</CODE> body.
     *
     * @param newObject the message's data
     * @throws JMSException if the JMS provider fails to set the object due to some
     *                      internal error.
     * @throws javax.jms.MessageFormatException
     *                      if object serialization fails.
     * @throws javax.jms.MessageNotWriteableException
     *                      if the message is in read-only mode.
     */

    public void setObject(Serializable newObject) throws JMSException {
        checkReadOnlyBody();
        this.object = newObject;
        setContent(null);
        storeContent();
    }

    /**
     * Gets the serializable object containing this message's data. The default
     * value is null.
     *
     * @return the serializable object containing this message's data
     * @throws JMSException
     */
    public Serializable getObject() throws JMSException {
        Buffer buffer = getContent();
        if (this.object == null && buffer != null) {
            this.object = (Serializable) StompTranslator.readObjectFromBuffer(buffer);
        }
        return this.object;
    }


    public String toString() {
        try {
            getObject();
        } catch (JMSException e) {
        }
        return super.toString();
    }
}
