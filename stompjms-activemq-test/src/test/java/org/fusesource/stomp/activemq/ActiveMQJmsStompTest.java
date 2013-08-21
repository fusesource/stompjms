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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ActiveMQJmsStompTest {
    BrokerService broker;
    int port;

    @Before
    public void setUp() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        TransportConnector connector = broker.addConnector("stomp://0.0.0.0:0");
        broker.start();
        broker.waitUntilStarted();
        port = connector.getConnectUri().getPort();
    }

    @After
    public void tearDown() throws Exception {
        broker.stop();
        broker.waitUntilStopped();
    }

    protected ConnectionFactory createConnectionFactory() throws Exception {
        StompJmsConnectionFactory result = new StompJmsConnectionFactory();
        result.setBrokerURI("tcp://localhost:" + this.port);
        return result;
    }

	@Test
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

	@Test
	@Ignore("Test added as support for AMQ-4493 - chained request/replies over ActiveMQ. The test *will* fail as of 2013-08-21")
	public void testChainedRequestReply() throws Exception {
		final String firstTopic = "mytopic";
		final String secondTopic = "secondtopic";
		// First block sending "1" and expecting "123" as reply.
        Connection connectionSource = createConnectionFactory().createConnection();
        connectionSource.start();
        Session sessionSource = connectionSource.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        MessageProducer producer = sessionSource.createProducer(sessionSource.createTopic(firstTopic));

        Destination replyToProducer = sessionSource.createTemporaryQueue();
        MessageConsumer sourceReplySubscription = sessionSource.createConsumer(replyToProducer);

        // Second block receiving "1" and using "mytopic" to append "3" after it's own "2"
        final Connection connectionIntermediate = createConnectionFactory().createConnection();
        connectionIntermediate.start();
        final Session sessionIntermediate = connectionIntermediate.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        final MessageConsumer consumerIntermediate = sessionIntermediate.createConsumer(sessionIntermediate.createTopic(firstTopic));
        Thread intermediateThread = new Thread(new Runnable() {

			public void run() {
				try {
					Message message = consumerIntermediate.receive();
					System.out.println("Producer -> Intermediate: " + message);
					Destination replyToIntermediate = sessionIntermediate.createTemporaryQueue();
					MessageProducer intermediateProducer = sessionIntermediate.createProducer(sessionIntermediate.createTopic(secondTopic));
					TextMessage secondMessage = sessionIntermediate.createTextMessage(((TextMessage) message).getText() + "2");
					intermediateProducer.send(secondMessage);

					MessageConsumer intermediateReplyConsumer = sessionIntermediate.createConsumer(replyToIntermediate);
					Message finalResponse = intermediateReplyConsumer.receive();
					MessageProducer replyProducer = sessionIntermediate.createProducer(message.getJMSDestination());

					System.out.println("Intermediate -> Producer: " + finalResponse);
					replyProducer.send(finalResponse);
				} catch (JMSException e) {
					Assert.fail(e.getMessage());
				}
			}});
        intermediateThread.setDaemon(true);
        intermediateThread.start();

        // Final block receiving "12" appending "3" and replying
        final Connection connectionFinal = createConnectionFactory().createConnection();
        connectionIntermediate.start();
        final Session sessionFinal = connectionIntermediate.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        final MessageConsumer consumerFinal = sessionIntermediate.createConsumer(sessionIntermediate.createTopic(secondTopic));
        Thread finalThread = new Thread(new Runnable() {

			public void run() {
				try {
					Message message = consumerFinal.receive();
					System.out.println("Intermediate -> Final: " + message);
					TextMessage finalResponse = sessionFinal.createTextMessage(((TextMessage) message).getText() + "3");

					MessageProducer replyProducer = sessionFinal.createProducer(message.getJMSDestination());
					System.out.println("Final -> Intermediate: " + finalResponse);
					replyProducer.send(finalResponse);
				} catch (JMSException e) {
					Assert.fail(e.getMessage());
				}
			}});
        finalThread.setDaemon(true);
        finalThread.start();

        TextMessage sourceTextMessage = sessionSource.createTextMessage("1");
        sourceTextMessage.setJMSReplyTo(replyToProducer);
        producer.send(sourceTextMessage);

        assertTextMessageReceived("123", sourceReplySubscription);
        connectionSource.close();
        connectionIntermediate.close();
        connectionFinal.close();
	}
	
    private void assertTextMessageReceived(final String expected, final MessageConsumer sub) throws JMSException {
        Message msg = sub.receive(1000*5);
        Assert.assertNotNull("A message was not received.", msg);
        Assert.assertEquals(expected, ((TextMessage)msg).getText());
    }
}
