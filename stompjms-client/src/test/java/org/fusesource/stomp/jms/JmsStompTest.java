/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stomp.jms;

import junit.framework.TestCase;
import org.fusesource.stomp.client.ApolloBroker;

import javax.jms.*;
import javax.naming.InitialContext;

import java.util.Hashtable;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class JmsStompTest extends TestCase {
    ApolloBroker broker = new ApolloBroker();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        broker.start();
    }

    @Override
    protected void tearDown() throws Exception {
        broker.stop();
        super.tearDown();
    }

    public void testJmsInterface() throws Exception {

        // You can use JNDI to avoid a compile time dependency on the stompjms library
        Hashtable jndiConfig = new Hashtable();
        jndiConfig.put("java.naming.factory.initial", "org.fusesource.stomp.jms.jndi.StompJmsInitialContextFactory");
        jndiConfig.put("java.naming.provider.url", "tcp://localhost:" + broker.port);
        jndiConfig.put("java.naming.security.principal", "admin");
        jndiConfig.put("java.naming.security.credentials", "password");
        InitialContext ctx = new InitialContext(jndiConfig);

        ConnectionFactory factory = (ConnectionFactory) ctx.lookup("ConnectionFactory");
        Queue queue = (Queue) ctx.lookup("queue/test");

        Connection connection = factory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(queue);

        producer.send(session.createTextMessage("test"));

        MessageConsumer consumer = session.createConsumer(queue);
        Message msg = consumer.receive();

        assertEquals("test", ((TextMessage)msg).getText());

        connection.close();
    }
}
