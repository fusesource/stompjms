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

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.stomp.jms.StompJmsExceptionSupport;

import javax.jms.JMSException;
import javax.jms.MessageNotWriteableException;
import javax.jms.TextMessage;
import java.io.UnsupportedEncodingException;

import static org.fusesource.stomp.client.Constants.TRANSFORMATION;


public class StompJmsTextMessage extends StompJmsMessage implements TextMessage {
    protected String text;

    public JmsMsgType getMsgType() {
        if( text == null ) {
            return JmsMsgType.TEXT_NULL;
        } else {
            return JmsMsgType.TEXT;
        }
    }

    public StompJmsMessage copy() throws JMSException {
        StompJmsTextMessage other = new StompJmsTextMessage();
        other.copy(this);
        return other;
    }

    private void copy(StompJmsTextMessage other) {
        super.copy(other);
        this.text = other.text;
    }

    public void setText(String text) throws MessageNotWriteableException {
        checkReadOnlyBody();
        this.text = text;
        setContent(null);
    }

    public String getText() throws JMSException {
        Buffer buffer = getContent();
        if (text == null && buffer != null) {
            this.text = new String(buffer.getData(), buffer.getOffset(), buffer.getLength());
            setContent(null);
        }
        return text;
    }

    public void storeContent() throws JMSException {
        try {
            if( text == null ) {
                setContent(new Buffer(0));
            } else {
                setContent(new Buffer(text.getBytes("UTF-8")));
            }
            getHeaderMap().put(TRANSFORMATION, getMsgType().buffer);
        } catch (UnsupportedEncodingException e) {
            throw StompJmsExceptionSupport.create(e.getMessage(), e);
        }
    }

    /**
     * Clears out the message body. Clearing a message's body does not clear its
     * header values or property entries. <p/>
     * <p/>
     * If this message body was read-only, calling this method leaves the
     * message body in the same state as an empty body in a newly created
     * message.
     *
     * @throws JMSException if the JMS provider fails to clear the message body
     *                      due to some internal error.
     */
    public void clearBody() throws JMSException {
        super.clearBody();
        this.text = null;
    }


    public String toString() {
        return super.toString() + ":text=" + text;
    }
}
