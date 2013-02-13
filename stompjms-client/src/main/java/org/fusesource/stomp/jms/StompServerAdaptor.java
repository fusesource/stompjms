/**
 * Copyright (C) 2013, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stomp.jms;

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.stomp.codec.StompFrame;

import javax.jms.JMSException;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import java.util.Map;
import java.util.UUID;

import static org.fusesource.stomp.client.Constants.BROWSER;
import static org.fusesource.stomp.client.Constants.PERSISTENT;
import static org.fusesource.stomp.client.Constants.TRUE;

/**
 *
 */
public class StompServerAdaptor {

    public boolean matchesServerAndVersion(String server) {
        return true;
    }

    public StompJmsTempQueue isTempQueue(StompJmsConnection connection, String value) {
        if( connection.tempQueuePrefix!=null && value.startsWith(connection.tempQueuePrefix) ) {
            return new StompJmsTempQueue(connection.tempQueuePrefix, value.substring(connection.tempQueuePrefix.length()));
        }
        return null;
    }

    public StompJmsTempTopic isTempTopic(StompJmsConnection connection, String value) throws JMSException {
        if( connection.tempTopicPrefix!=null && value.startsWith(connection.tempTopicPrefix) ) {
            return new StompJmsTempTopic(connection.tempTopicPrefix, value.substring(connection.tempTopicPrefix.length()));
        }
        return null;
    }

    public StompFrame createCreditFrame(StompJmsMessageConsumer consumer, StompFrame messageFrame) {
        return null;
    }

    public TemporaryQueue createTemporaryQueue(StompJmsSession session) throws JMSException {
        if( session.connection.tempQueuePrefix!=null ) {
            return new StompJmsTempQueue(session.connection.tempQueuePrefix, UUID.randomUUID().toString());
        }
        return null;
    }

    public TemporaryTopic createTemporaryTopic(StompJmsSession session) throws JMSException {
        if( session.connection.tempTopicPrefix!=null ) {
            return new StompJmsTempTopic(session.connection.tempTopicPrefix, UUID.randomUUID().toString());
        }
        return null;
    }

    public void addSubscribeHeaders(Map<AsciiBuffer, AsciiBuffer> headerMap, boolean persistent, boolean browser, boolean noLocal, StompJmsPrefetch prefetch) throws JMSException {
        if (browser) {
            throw new JMSException("Server does not support browsing over STOMP");
        }
        if (noLocal) {
            throw new JMSException("Server does not support 'no local' semantics over STOMP");
        }
        if (persistent) {
            throw new JMSException("Server does not durable subscriptions over STOMP");
        }
    }
}
