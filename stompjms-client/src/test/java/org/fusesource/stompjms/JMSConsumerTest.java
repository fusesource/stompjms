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

import junit.framework.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Test cases used to test the JMS message consumer.
 *
 * @version $Revision$
 */
public class JMSConsumerTest extends JmsTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(JMSConsumerTest.class);

    public StompJmsDestination destination;
    public int deliveryMode;
    public int ackMode;
    public String destinationType;
    public boolean durableConsumer;

    public static Test suite() {
        return suite(JMSConsumerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

//    public void initCombosForTestFlowControl() {
//        addCombinationValues("deliveryMode", new Object[]{Integer.valueOf(DeliveryMode.NON_PERSISTENT), Integer.valueOf(DeliveryMode.PERSISTENT)});
//        addCombinationValues("destinationType", new Object[]{"/queue/"});
//    }
//
//    public void testFlowControl() throws Exception {
//
//        // Receive a message with the JMS API
//        connection.start();
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        destination = createDestination(destinationType);
//        StompJmsMessageConsumer consumer = (StompJmsMessageConsumer)session.createConsumer(destination);
//        MessageProducer producer = session.createProducer(destination);
//        producer.setDeliveryMode(deliveryMode);
//
//        for (int i = 0; i < 1000; i++) {
//            BytesMessage message = session.createBytesMessage();
//            message.writeBytes(new byte[1024]);
//            message.setIntProperty("seq", i);
//            producer.send(message);
//        }
//
//        Thread.sleep(1000);
//        int size = consumer.messageQueue.size();
//
//        // We should not get all the messages.
//        assertTrue(size < 1000);
//
//        // We should not get any more messages since we have not acked any..
//        Thread.sleep(500);
//        assertEquals(size, consumer.messageQueue.size());
//
//        // Ack some messages.  That should cause us to get some
//        // more messages.
//        consumer.receive().acknowledge();
//        consumer.receive().acknowledge();
//        consumer.receive().acknowledge();
//
//        Thread.sleep(500);
//        assertTrue(consumer.messageQueue.size() > size-3 );
//
//    }

    public void initCombosForTestTransactions() {
        addCombinationValues("deliveryMode", new Object[]{Integer.valueOf(DeliveryMode.NON_PERSISTENT), Integer.valueOf(DeliveryMode.PERSISTENT)});
//        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/", "/temp-queue/", "/temp-topic/"});
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    public void testTransactions() throws Exception {

        // Receive a message with the JMS API
        connection.start();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        destination = createDestination(destinationType);
        MessageConsumer consumer = session.createConsumer(destination);
        MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(deliveryMode);

        Message message = session.createMessage();
        message.setStringProperty("seq", "1");
        producer.send(message);

        message = session.createMessage();
        message.setStringProperty("seq", "2");
        producer.send(message);

        assertNull(consumer.receive(500));

        session.commit();

        // We should not get the messages
        message = consumer.receive(1000);
        assertEquals("1", message.getStringProperty("seq"));
        message = consumer.receive(1000);
        assertEquals("2", message.getStringProperty("seq"));
        assertNull(consumer.receive(500));

        session.commit();

        // no more messages should be received.
        assertNull(consumer.receive(500));

    }

    public void testMessageProperties() throws Exception {
        destinationType = "/queue/";
        deliveryMode = DeliveryMode.NON_PERSISTENT;

        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = createDestination(destinationType);
        MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(deliveryMode);

        Message msg = session.createMessage();
        msg.setBooleanProperty("p1", true);
        msg.setByteProperty("p2", (byte) 2);
        msg.setShortProperty("p3", (short) 3);
        producer.send(msg);

        MessageConsumer consumer = session.createConsumer(destination);
        Message m = consumer.receive(1000);
        assertNotNull(m);
        assertTrue(msg.getBooleanProperty("p1"));
        assertEquals((byte) 2, m.getByteProperty("p2"));
        assertEquals((short) 3, m.getShortProperty("p3"));

        assertNull(consumer.receiveNoWait());
    }

    public void initCombosForTestQueueBrowser() {
        addCombinationValues("deliveryMode", new Object[]{Integer.valueOf(DeliveryMode.NON_PERSISTENT), Integer.valueOf(DeliveryMode.PERSISTENT)});
        addCombinationValues("destinationType", new Object[]{"/queue/"});
    }

    public void testQueueBrowser() throws Exception {
        // Receive a message with the JMS API
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = createDestination(destinationType);
        MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(deliveryMode);

        producer.send(session.createTextMessage("1"));
        producer.send(session.createTextMessage("2"));
        producer.send(session.createTextMessage("3"));

        Thread.sleep(1000);

        // Make sure only 1 message was delivered.
        QueueBrowser browser = session.createBrowser((Queue) destination);

        ArrayList<String> expected = new ArrayList<String>();
        expected.add("1");
        expected.add("2");
        expected.add("3");

        ArrayList<String> results = new ArrayList<String>();
        Enumeration enumeration = browser.getEnumeration();
        while (browser.getEnumeration().hasMoreElements()) {
            TextMessage m = (TextMessage) enumeration.nextElement();
            results.add(m.getText());
        }
        System.out.println(results);
        assertEquals(expected, results);

        browser = session.createBrowser((Queue) destination);
        results = new ArrayList<String>();
        enumeration = browser.getEnumeration();
        while (browser.getEnumeration().hasMoreElements()) {
            TextMessage m = (TextMessage) enumeration.nextElement();
            results.add(m.getText());
        }
        System.out.println(results);
        assertEquals(expected, results);
    }

    public void initCombosForTestMessageListenerWithConsumerCanBeStopped() {
        addCombinationValues("deliveryMode", new Object[]{Integer.valueOf(DeliveryMode.NON_PERSISTENT), Integer.valueOf(DeliveryMode.PERSISTENT)});
//        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/", "/temp-queue/", "/temp-topic/"});
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    public void testMessageListenerWithConsumerCanBeStopped() throws Exception {

        final AtomicInteger counter = new AtomicInteger(0);
        final CountDownLatch done1 = new CountDownLatch(1);
        final CountDownLatch done2 = new CountDownLatch(1);

        // Receive a message with the JMS API
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = createDestination(destinationType);
        StompJmsMessageConsumer consumer = (StompJmsMessageConsumer) session.createConsumer(destination);
        consumer.setMessageListener(new MessageListener() {
            public void onMessage(Message m) {
                counter.incrementAndGet();
                if (counter.get() == 1) {
                    done1.countDown();
                }
                if (counter.get() == 2) {
                    done2.countDown();
                }
            }
        });

        // Send a first message to make sure that the consumer dispatcher is
        // running
        sendMessages(session, destination, 1);
        assertTrue(done1.await(1, TimeUnit.SECONDS));
        assertEquals(1, counter.get());

        // Stop the consumer.
        consumer.stop();

        // Send a message, but should not get delivered.
        sendMessages(session, destination, 1);
        assertFalse(done2.await(1, TimeUnit.SECONDS));
        assertEquals(1, counter.get());

        // Start the consumer, and the message should now get delivered.
        consumer.start();
        assertTrue(done2.await(1, TimeUnit.SECONDS));
        assertEquals(2, counter.get());
    }


    public void initCombosForTestSendReceiveBytesMessage() {
        addCombinationValues("deliveryMode", new Object[]{Integer.valueOf(DeliveryMode.NON_PERSISTENT), Integer.valueOf(DeliveryMode.PERSISTENT)});
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }


    public void testSendReceiveBytesMessage() throws Exception {

        // Receive a message with the JMS API
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = createDestination(destinationType);
        MessageConsumer consumer = session.createConsumer(destination);
        MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(deliveryMode);

        BytesMessage message = session.createBytesMessage();
        message.writeBoolean(true);
        message.writeBoolean(false);
        producer.send(message);

        // Make sure only 1 message was delivered.
        BytesMessage m = (BytesMessage) consumer.receive(1000);
        assertNotNull(m);
        assertTrue(m.readBoolean());
        assertFalse(m.readBoolean());
        assertNull(consumer.receiveNoWait());
    }

    public void initCombosForTestSetMessageListenerAfterStart() {
        addCombinationValues("deliveryMode", new Object[]{Integer.valueOf(DeliveryMode.NON_PERSISTENT), Integer.valueOf(DeliveryMode.PERSISTENT)});
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    public void testSetMessageListenerAfterStart() throws Exception {

        final AtomicInteger counter = new AtomicInteger(0);
        final CountDownLatch done = new CountDownLatch(1);

        // Receive a message with the JMS API
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = createDestination(destinationType);
        MessageConsumer consumer = session.createConsumer(destination);

        // Send the messages
        sendMessages(session, destination, 4);

        // See if the message get sent to the listener
        consumer.setMessageListener(new MessageListener() {
            public void onMessage(Message m) {
                counter.incrementAndGet();
                if (counter.get() == 4) {
                    done.countDown();
                }
            }
        });

        assertTrue(done.await(1000, TimeUnit.MILLISECONDS));
        Thread.sleep(200);

        // Make sure only 4 messages were delivered.
        assertEquals(4, counter.get());
    }

    public void initCombosForTestPassMessageListenerIntoCreateConsumer() {
        addCombinationValues("destinationType", new Object[]{"queue/", "topic/"});
    }


    public void initCombosForTestMessageListenerWithConsumer() {
        addCombinationValues("deliveryMode", new Object[]{Integer.valueOf(DeliveryMode.NON_PERSISTENT), Integer.valueOf(DeliveryMode.PERSISTENT)});
        addCombinationValues("destinationType", new Object[]{"/queue/", "/topic/"});
    }

    public void testMessageListenerWithConsumer() throws Exception {

        final AtomicInteger counter = new AtomicInteger(0);
        final CountDownLatch done = new CountDownLatch(1);

        // Receive a message with the JMS API
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = createDestination(destinationType);
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(new MessageListener() {
            public void onMessage(Message m) {
                counter.incrementAndGet();
                if (counter.get() == 4) {
                    done.countDown();
                }
            }
        });

        // Send the messages
        sendMessages(session, destination, 4);

        assertTrue(done.await(1000, TimeUnit.MILLISECONDS));
        Thread.sleep(200);

        // Make sure only 4 messages were delivered.
        assertEquals(4, counter.get());
    }
}
