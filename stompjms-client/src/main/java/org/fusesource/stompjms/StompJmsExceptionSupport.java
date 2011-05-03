/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms;

import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;

/**
 * Create those nice, old fashioned JMS Exceptions
 */
public final class StompJmsExceptionSupport {

    private StompJmsExceptionSupport() {
    }

    public static JMSException create(String msg, Throwable cause) {
        JMSException exception = new JMSException(msg);
        exception.initCause(cause);
        return exception;
    }

    public static JMSException create(String msg, Exception cause) {
        JMSException exception = new JMSException(msg);
        exception.setLinkedException(cause);
        exception.initCause(cause);
        return exception;
    }

    public static JMSException create(Throwable cause) {
        if (cause instanceof JMSException) {
            return (JMSException) cause;
        }
        String msg = cause.getMessage();
        if (msg == null || msg.length() == 0) {
            msg = cause.toString();
        }
        JMSException exception = new JMSException(msg);
        exception.initCause(cause);
        return exception;
    }

    public static JMSException create(Exception cause) {
        if (cause instanceof JMSException) {
            return (JMSException) cause;
        }
        String msg = cause.getMessage();
        if (msg == null || msg.length() == 0) {
            msg = cause.toString();
        }
        JMSException exception = new JMSException(msg);
        exception.setLinkedException(cause);
        exception.initCause(cause);
        return exception;
    }

    public static MessageEOFException createMessageEOFException(Exception cause) {
        String msg = cause.getMessage();
        if (msg == null || msg.length() == 0) {
            msg = cause.toString();
        }
        MessageEOFException exception = new MessageEOFException(msg);
        exception.setLinkedException(cause);
        exception.initCause(cause);
        return exception;
    }

    public static MessageFormatException createMessageFormatException(Throwable cause) {
        String msg = cause.getMessage();
        if (msg == null || msg.length() == 0) {
            msg = cause.toString();
        }
        MessageFormatException exception = new MessageFormatException(msg);
        exception.initCause(cause);
        return exception;
    }


}
