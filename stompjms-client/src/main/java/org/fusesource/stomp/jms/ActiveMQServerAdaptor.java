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

import javax.jms.JMSException;
import java.util.Map;

import static org.fusesource.stomp.client.Constants.*;

/**
 *
 */
public class ActiveMQServerAdaptor extends StompServerAdaptor {

    private static final AsciiBuffer SUBSCRIPTION_NAME=new AsciiBuffer("activemq.subscriptionName");
    private static final AsciiBuffer NO_LOCAL=new AsciiBuffer("activemq.noLocal");

    @Override
    public boolean matchesServerAndVersion(String server) {
        return server!=null && server.startsWith("ActiveMQ/");
    }

    @Override
    public void addSubscribeHeaders(Map<AsciiBuffer, AsciiBuffer> headerMap, boolean persistent, boolean browser, boolean noLocal, StompJmsPrefetch prefetch) throws JMSException {
        if (browser) {
            throw new JMSException("ActiveMQ does not support browsing over STOMP");
        }
        if (noLocal) {
            headerMap.put(NO_LOCAL, TRUE);
        }
        if (persistent) {
            headerMap.put(SUBSCRIPTION_NAME, headerMap.get(ID));
        }
    }
}
