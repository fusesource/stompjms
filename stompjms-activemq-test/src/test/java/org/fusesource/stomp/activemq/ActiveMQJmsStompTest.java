/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stomp.activemq;

import junit.framework.TestCase;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;

import javax.jms.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ActiveMQJmsStompTest extends TestCase {
    BrokerService broker;
    int port;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        broker = new BrokerService();
        broker.setPersistent(false);
        TransportConnector connector = broker.addConnector("stomp://0.0.0.0:0");
        broker.start();
        broker.waitUntilStarted();
        port = connector.getConnectUri().getPort();
    }

    @Override
    protected void tearDown() throws Exception {
        broker.stop();
        broker.waitUntilStopped();
        super.tearDown();
    }

    protected ConnectionFactory createConnectionFactory() throws Exception {
        StompJmsConnectionFactory result = new StompJmsConnectionFactory();
        result.setBrokerURI("tcp://localhost:" + this.port);
        return result;
    }

	public void testDurableSubs() throws Exception {
        Connection connection1 = createConnectionFactory().createConnection();
        connection1.setClientID("client1");
        connection1.start();

        Session session1 = connection1.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        MessageConsumer sub = session1.createDurableSubscriber(session1.createTopic("mytopic"), "sub1");

        Connection connection2 = createConnectionFactory().createConnection();
        connection2.start();
        Session session2 = connection2.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        MessageProducer producer = session2.createProducer(session2.createTopic("mytopic"));

        producer.send(session2.createTextMessage("1"));
        // Disconnect the durable sub..
        connection1.close();
        producer.send(session2.createTextMessage("2"));

        // Restore the durable sub..
        connection1 = createConnectionFactory().createConnection();
        connection1.setClientID("client1");
        connection1.start();
        session1 = connection1.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        sub = session1.createDurableSubscriber(session1.createTopic("mytopic"), "sub1");

        assertTextMessageReceived("1", sub);
        assertTextMessageReceived("2", sub);
        connection1.close();
        connection2.close();
    }

    public void testQueueSendReceiveSingleConnection() throws Exception {

        Connection connection1 = createConnectionFactory().createConnection();
        connection1.setClientID("client1");
        connection1.start();

        Session session1 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Queue q = session1.createQueue("myQueue");
        MessageProducer producer = session1.createProducer(q);

        producer.send(session1.createTextMessage("1"));

        MessageConsumer consumer = session1.createConsumer(q);
        assertTextMessageReceived("1", consumer);

        connection1.close();

        // verify it can't be consumed again

        connection1 = createConnectionFactory().createConnection();
        connection1.setClientID("client1");
        connection1.start();

        session1 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);

        consumer = session1.createConsumer(q);
        assertNull(consumer.receive(1000));

        connection1.close();
    }

    private void assertTextMessageReceived(String expected, MessageConsumer sub) throws JMSException {
        Message msg = sub.receive(1000*5);
        assertNotNull("A message was not received.", msg);
        assertEquals(expected, ((TextMessage)msg).getText());
    }
}
