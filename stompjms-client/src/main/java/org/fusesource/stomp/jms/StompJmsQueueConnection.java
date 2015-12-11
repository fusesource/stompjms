/**
 * Copyright (C) 2012, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stomp.jms;

import javax.jms.*;
import java.net.URI;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class StompJmsQueueConnection extends StompJmsConnection {

    public StompJmsQueueConnection(URI brokerURI, URI localURI, String userName, String password, int connectTimeoutMs) throws JMSException {
        super(brokerURI, localURI, userName, password, connectTimeoutMs);
    }

    @Override
    public TopicSession createTopicSession(boolean transacted, int acknowledgeMode) throws JMSException {
        throw new javax.jms.IllegalStateException("Operation not supported by a QueueConnection");
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        throw new javax.jms.IllegalStateException("Operation not supported by a QueueConnection");
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Topic topic, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        throw new javax.jms.IllegalStateException("Operation not supported by a QueueConnection");
    }

}
