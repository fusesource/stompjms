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

import static org.fusesource.stomp.jms.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import junit.framework.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class JmsTransactionTestSupport extends JmsTestSupport implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(JmsTransactionTestSupport.class);
    private static final int MESSAGE_COUNT = 5;
    private static final String MESSAGE_TEXT = "message";

    protected Session session;
    protected MessageConsumer consumer;
    protected MessageProducer producer;
    protected int batchCount = 10;
    protected int batchSize = 20;
    protected Destination destination;

    public String destinationType;

    // for message listener test
    private List<Message> unackMessages = new ArrayList<Message>(MESSAGE_COUNT);
    private List<Message> ackMessages = new ArrayList<Message>(MESSAGE_COUNT);
    private boolean resendPhase;

    public static Test suite() {
        return suite(JmsTransactionTestSupport.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    protected MessageConsumer createConsumer(Session session, Destination destination) throws Exception
    {
    	return session.createConsumer(destination);
    }

    protected void setUp() throws Exception {
    	super.setUp();
    	destination = createDestination(destinationType);

        reconnect();

        // lets consume any outstanding messages from prev test runs
        while (consumer.receive(200) != null) {
        }
        session.commit();
    }

    protected void tearDown() throws Exception {
        LOG.info("Closing down connection");

        session.close();
        session = null;

        super.tearDown();

        LOG.info("Connection closed.");
    }

    public void initCombosForTestSendReceiveTransactedBatches() {
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    /**
     * Sends a batch of messages and validates that the messages are received.
     *
     * @throws Exception
     */
    public void testSendReceiveTransactedBatches() throws Exception {

        TextMessage message = session.createTextMessage("Batch Message");
        for (int j = 0; j < batchCount; j++) {
            LOG.info("Producing bacth " + j + " of " + batchSize + " messages");

            for (int i = 0; i < batchSize; i++) {
                producer.send(message);
            }
            messageSent();
            session.commit();
            LOG.info("Consuming bacth " + j + " of " + batchSize + " messages");

            for (int i = 0; i < batchSize; i++) {
                message = (TextMessage)consumer.receive(1000 * 5);
                assertNotNull("Received only " + i + " messages in batch " + j, message);
                assertEquals("Batch Message", message.getText());
            }

            session.commit();
        }
    }

    protected void messageSent() throws Exception {
    }

    public void initCombosForTestSendRollback() {
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    /**
     * Sends a batch of messages and validates that the rollbacked message was
     * not consumed.
     *
     * @throws Exception
     */
    public void testSendRollback() throws Exception {
        Message[] outbound = new Message[] {session.createTextMessage("First Message"), session.createTextMessage("Second Message")};

        // sends a message
        producer.send(outbound[0]);
        session.commit();

        // sends a message that gets rollbacked
        producer.send(session.createTextMessage("I'm going to get rolled back."));
        session.rollback();

        // sends a message
        producer.send(outbound[1]);
        session.commit();

        // receives the first message
        ArrayList<Message> messages = new ArrayList<Message>();
        LOG.info("About to consume message 1");
        Message message = consumer.receive(1000);
        messages.add(message);
        LOG.info("Received: " + message);

        // receives the second message
        LOG.info("About to consume message 2");
        message = consumer.receive(4000);
        messages.add(message);
        LOG.info("Received: " + message);

        // validates that the rollbacked was not consumed
        session.commit();
        Message inbound[] = new Message[messages.size()];
        messages.toArray(inbound);
        assertTextMessagesEqual("Rollback did not work.", outbound, inbound);
    }

    public void initCombosForTestAckMessageInTx() {
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    /**
     * spec section 3.6 acking a message with automation acks has no effect.
     * @throws Exception
     */
    public void testAckMessageInTx() throws Exception {
        Message[] outbound = new Message[] {session.createTextMessage("First Message")};

        // sends a message
        producer.send(outbound[0]);
        outbound[0].acknowledge();
        session.commit();
        outbound[0].acknowledge();

        // receives the first message
        ArrayList<Message> messages = new ArrayList<Message>();
        LOG.info("About to consume message 1");
        Message message = consumer.receive(1000);
        messages.add(message);
        LOG.info("Received: " + message);

        // validates that the rollbacked was not consumed
        session.commit();
        Message inbound[] = new Message[messages.size()];
        messages.toArray(inbound);
        assertTextMessagesEqual("Message not delivered.", outbound, inbound);
    }

    public void initCombosForTestSendSessionClose() {
    	// TODO - Fails for "/topic/"
        addCombinationValues("destinationType", new Object[]{"/queue/"});
    }

    /**
     * Sends a batch of messages and validates that the message sent before
     * session close is not consumed.
     *
     * This test only works with local transactions, not xa.
     * @throws Exception
     */
    public void testSendSessionClose() throws Exception {
        Message[] outbound = new Message[] {session.createTextMessage("First Message"),
        									session.createTextMessage("Second Message")};

        consumer.close();

        // sends a message
        producer.send(outbound[0]);
        session.commit();

        // sends a message that gets rollbacked
        producer.send(session.createTextMessage("I'm going to get rolled back."));

        reconnectSession();

        // sends a message
        producer.send(outbound[1]);
        session.commit();

        // receives the first message
        ArrayList<Message> messages = new ArrayList<Message>();
        LOG.info("About to consume message 1");

        Message message = consumer.receive(1000);
        messages.add(message);
        LOG.info("Received: " + message);

        // receives the second message
        LOG.info("About to consume message 2");
        message = consumer.receive(4000);
        messages.add(message);
        LOG.info("Received: " + message);

        // validates that the rollbacked was not consumed
        session.commit();
        Message inbound[] = new Message[messages.size()];
        messages.toArray(inbound);
        assertTextMessagesEqual("Rollback did not work.", outbound, inbound);
    }

    public void initCombosForTestSendSessionAndConnectionClose() {
    	// TODO - Fails for , "/topic/"
        addCombinationValues("destinationType", new Object[]{"/queue/"});
    }

    /**
     * Sends a batch of messages and validates that the message sent before
     * session close is not consumed.
     *
     * @throws Exception
     */
    public void testSendSessionAndConnectionClose() throws Exception {
        Message[] outbound = new Message[] {session.createTextMessage("First Message"), session.createTextMessage("Second Message")};

        // sends a message
        producer.send(outbound[0]);
        session.commit();

        // sends a message that gets rollbacked
        producer.send(session.createTextMessage("I'm going to get rolled back."));
        consumer.close();
        session.close();

        reconnect();

        // sends a message
        producer.send(outbound[1]);
        session.commit();

        // receives the first message
        ArrayList<Message> messages = new ArrayList<Message>();
        LOG.info("About to consume message 1");
        ;
        Message message = consumer.receive(1000);
        messages.add(message);
        LOG.info("Received: " + message);

        // receives the second message
        LOG.info("About to consume message 2");
        message = consumer.receive(4000);
        messages.add(message);
        LOG.info("Received: " + message);

        // validates that the rollbacked was not consumed
        session.commit();
        Message inbound[] = new Message[messages.size()];
        messages.toArray(inbound);
        assertTextMessagesEqual("Rollback did not work.", outbound, inbound);
    }

    public void initCombosForTestReceiveRollback() {
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    /**
     * Sends a batch of messages and validates that the rollbacked message was
     * redelivered.
     *
     * @throws Exception
     */
    // TODO - implement client side rollback redelivery.
    public void x_testReceiveRollback() throws Exception {
        Message[] outbound = new Message[] {session.createTextMessage("First Message"), session.createTextMessage("Second Message")};

        // sent both messages
        producer.send(outbound[0]);
        producer.send(outbound[1]);
        session.commit();

        LOG.info("Sent 0: " + outbound[0]);
        LOG.info("Sent 1: " + outbound[1]);

        ArrayList<Message> messages = new ArrayList<Message>();

        Message message = consumer.receive(1000);
        messages.add(message);
        assertEquals(outbound[0], message);
        session.commit();

        // rollback so we can get that last message again.
        message = consumer.receive(1000);
        assertNotNull(message);
        assertEquals(outbound[1], message);
        session.rollback();

        // Consume again.. the prev message should get redelivered.
        message = consumer.receive(5000);
        assertNotNull("Should have re-received the message again!", message);
        messages.add(message);
        session.commit();

        Message inbound[] = new Message[messages.size()];
        messages.toArray(inbound);
        assertTextMessagesEqual("Rollback did not work", outbound, inbound);
    }

    public void initCombosForTestReceiveTwoThenRollback() {
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    /**
     * Sends a batch of messages and validates that the rollbacked message was
     * redelivered.
     *
     * @throws Exception
     */
    public void x_testReceiveTwoThenRollback() throws Exception {
        Message[] outbound = new Message[] {session.createTextMessage("First Message"), session.createTextMessage("Second Message")};

        producer.send(outbound[0]);
        producer.send(outbound[1]);
        session.commit();

        LOG.info("Sent 0: " + outbound[0]);
        LOG.info("Sent 1: " + outbound[1]);

        ArrayList<Message> messages = new ArrayList<Message>();
        Message message = consumer.receive(1000);
        assertEquals(outbound[0], message);

        message = consumer.receive(1000);
        assertNotNull(message);
        assertEquals(outbound[1], message);
        session.rollback();

        // Consume again.. the prev message should get redelivered.
        message = consumer.receive(5000);
        assertNotNull("Should have re-received the first message again!", message);
        messages.add(message);
        assertEquals(outbound[0], message);
        message = consumer.receive(5000);
        assertNotNull("Should have re-received the second message again!", message);
        messages.add(message);
        assertEquals(outbound[1], message);

        assertNull(consumer.receiveNoWait());
        session.commit();

        Message inbound[] = new Message[messages.size()];
        messages.toArray(inbound);
        assertTextMessagesEqual("Rollback did not work", outbound, inbound);
    }

    public void initCombosForTestReceiveTwoThenRollbackManyTimes() {
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    /**
     * Perform the test that validates if the rollbacked message was redelivered
     * multiple times.
     *
     * @throws Exception
     */
    public void x_testReceiveTwoThenRollbackManyTimes() throws Exception {
        for (int i = 0; i < 5; i++) {
            x_testReceiveTwoThenRollback();
        }
    }

    public void initCombosForTestCloseConsumerBeforeCommit() {
    	// TODO "/topic/" currently fails
        addCombinationValues("destinationType", new Object[]{"/queue/"});
    }

    /**
     * Tests if the messages can still be received if the consumer is closed
     * (session is not closed).
     *
     * @throws Exception see http://jira.codehaus.org/browse/AMQ-143
     */
    public void testCloseConsumerBeforeCommit() throws Exception {
        TextMessage[] outbound = new TextMessage[] {session.createTextMessage("First Message"), session.createTextMessage("Second Message")};

        // sends the messages
        producer.send(outbound[0]);
        producer.send(outbound[1]);
        session.commit();
        LOG.info("Sent 0: " + outbound[0]);
        LOG.info("Sent 1: " + outbound[1]);

        TextMessage message = (TextMessage)consumer.receive(1000);
        assertEquals(outbound[0].getText(), message.getText());
        // Close the consumer before the commit. This should not cause the
        // received message to rollback.
        consumer.close();
        session.commit();

        // Create a new consumer
        consumer = createConsumer(session, destination);
        LOG.info("Created consumer: " + consumer);

        message = (TextMessage)consumer.receive(2000);
        assertNotNull("Should have received: " + outbound[1], message);
        assertEquals(outbound[1].getText(), message.getText());
        session.commit();
    }

    @Override
    protected void reconnect() throws Exception {
    	super.reconnect();
        reconnectSession();
        connection.start();
    }

    protected void reconnectSession() throws JMSException {
        if (session != null) {
            session.close();
        }

        destination = reCreateDestination(destinationType);
        session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
        producer = session.createProducer(destination);
        consumer = session.createConsumer(destination);
    }

    public void initCombosForTestMessageListener() {
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    public void x_testMessageListener() throws Exception {
        // send messages
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            producer.send(session.createTextMessage(MESSAGE_TEXT + i));
        }
        session.commit();
        consumer.setMessageListener(this);
        // wait receive
        waitReceiveUnack();
        assertEquals(unackMessages.size(), MESSAGE_COUNT);
        // resend phase
        waitReceiveAck();
        assertEquals(ackMessages.size(), MESSAGE_COUNT);
        // should no longer re-receive
        consumer.setMessageListener(null);
        assertNull(consumer.receive(500));
        reconnect();
    }

    public void onMessage(Message message) {
        if (!resendPhase) {
            unackMessages.add(message);
            if (unackMessages.size() == MESSAGE_COUNT) {
                try {
                    session.rollback();
                    resendPhase = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            ackMessages.add(message);
            if (ackMessages.size() == MESSAGE_COUNT) {
                try {
                    session.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void waitReceiveUnack() throws Exception {
        for (int i = 0; i < 100 && !resendPhase; i++) {
            Thread.sleep(100);
        }
        assertTrue(resendPhase);
    }

    private void waitReceiveAck() throws Exception {
        for (int i = 0; i < 100 && ackMessages.size() < MESSAGE_COUNT; i++) {
            Thread.sleep(100);
        }
        assertFalse(ackMessages.size() < MESSAGE_COUNT);
    }
}
