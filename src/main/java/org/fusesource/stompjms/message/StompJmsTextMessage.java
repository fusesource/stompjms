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
import org.fusesource.stompjms.StompJmsExceptionSupport;

import javax.jms.JMSException;
import javax.jms.MessageNotWriteableException;
import javax.jms.TextMessage;
import java.io.UnsupportedEncodingException;



public class StompJmsTextMessage extends StompJmsMessage implements TextMessage {
    protected String text;

    public JmsMsgType getMsgType() {
        return JmsMsgType.TEXT;
    }
    
    public StompJmsMessage copy() throws JMSException {
        StompJmsTextMessage copy = new StompJmsTextMessage();
        copy(copy);
        return copy;
    }

    private void copy(StompJmsTextMessage copy) {
        super.copy(copy);
        copy.text = text;
    }

    
    public String getJMSXMimeType() {
        return "jms/text-message";
    }

    public void setText(String text) throws MessageNotWriteableException {
        checkReadOnlyBody();
        this.text = text;
        setContent(null);
    }

    public String getText() throws JMSException {
        Buffer buffer = getContent();
        if (text == null && buffer != null) {
            this.text = new String(buffer.getData(),buffer.getOffset(),buffer.getLength());
            setContent(null);
        }
        return text;
    }
    
    public void storeContent() throws JMSException {
        try {
            setContent(new Buffer(text.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
           throw StompJmsExceptionSupport.create(e.getMessage(), e);
        }
    }   
    
    /**
     * Clears out the message body. Clearing a message's body does not clear its
     * header values or property entries. <p/>
     * <P>
     * If this message body was read-only, calling this method leaves the
     * message body in the same state as an empty body in a newly created
     * message.
     * 
     * @throws JMSException if the JMS provider fails to clear the message body
     *                 due to some internal error.
     */
    public void clearBody() throws JMSException {
        super.clearBody();
        this.text = null;
    }

        
    public String toString() {
        return super.toString() + ":text="+text;
    }
}
