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
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.stomp.codec.StompFrame;

import javax.jms.JMSException;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;

import java.util.Map;
import java.util.UUID;

import static org.fusesource.stomp.client.Constants.*;
import static org.fusesource.stomp.client.Constants.BROWSER;
import static org.fusesource.stomp.client.Constants.TRUE;

/**
 *
 */
public class ApolloServerAdaptor extends StompServerAdaptor {

    @Override
    public boolean matchesServerAndVersion(String server) {
        return server!=null && server.startsWith("apache-apollo/");
    }

    @Override
    public StompJmsTempQueue isTempQueue(StompJmsConnection connection, String value) {
        if( value.startsWith(connection.queuePrefix+"temp.")) {
            return new StompJmsTempQueue(connection.queuePrefix, value.substring(connection.queuePrefix.length()));
        }
        return super.isTempQueue(connection, value);
    }

    @Override
    public StompJmsTempTopic isTempTopic(StompJmsConnection connection, String value) throws JMSException {
        if( value.startsWith(connection.topicPrefix+"temp.")) {
            return new StompJmsTempTopic(connection.topicPrefix, value.substring(connection.topicPrefix.length()));
        }
        return super.isTempTopic(connection, value);
    }

    public StompFrame createCreditFrame(StompJmsMessageConsumer consumer, StompFrame messageFrame) {
        final Buffer content = messageFrame.content();
        String credit = "1";
        if( content!=null ) {
            credit += ","+content.length();
        }

        StompFrame frame = new StompFrame();
        frame.action(ACK);
        frame.headerMap().put(SUBSCRIPTION, consumer.id);
        frame.headerMap().put(CREDIT, AsciiBuffer.ascii(credit));
        return frame;
    }

    private String createApolloTempDestName(StompJmsSession session) throws JMSException {
        String host = session.getChannel().getConnectedHostId();
        if( host == null ) {
            host = session.connection.brokerURI.getHost();
        }
        final String sessionId = session.getChannel().getConnectedSessionId();
        return "temp."+ host +"."+ sessionId +"."+ UUID.randomUUID().toString();
    }

    @Override
    public TemporaryQueue createTemporaryQueue(StompJmsSession session) throws JMSException {
        return new StompJmsTempQueue(session.connection.queuePrefix, createApolloTempDestName(session));
    }

    @Override
    public TemporaryTopic createTemporaryTopic(StompJmsSession session) throws JMSException {
        return new StompJmsTempTopic(session.connection.topicPrefix, createApolloTempDestName(session));
    }

    static final StompJmsPrefetch DEFAULT_PREFETCH = new StompJmsPrefetch();

    @Override
    public void addSubscribeHeaders(Map<AsciiBuffer, AsciiBuffer> headerMap, boolean persistent, boolean browser, boolean noLocal, StompJmsPrefetch prefetch) throws JMSException {
        if (noLocal) {
            throw new JMSException("Server does not support 'no local' semantics over STOMP");
        }
        if (persistent) {
            headerMap.put(PERSISTENT, TRUE);
        }
        if (browser) {
            headerMap.put(BROWSER, TRUE);
        }
        if( !prefetch.equals(DEFAULT_PREFETCH) ){
            headerMap.put(CREDIT, AsciiBuffer.ascii(prefetch.getMaxMessages()+","+prefetch.getMaxBytes()));
        }
    }

    @Override
    public StompFrame createUnsubscribeFrame(AsciiBuffer consumerId, boolean persistent) throws JMSException {
        StompFrame frame = new StompFrame();
        frame.action(UNSUBSCRIBE);
        frame.headerMap().put(ID, consumerId);
        if (persistent) {
            frame.headerMap().put(PERSISTENT, TRUE);
        }
        return frame;
    }
}
