/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.stompjms;

import junit.framework.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Small burn test moves sends a moderate amount of messages through the broker,
 * to checking to make sure that the broker does not lock up after a while of
 * sustained messaging.
 * 
 * @version $Revision$
 */
public class LoadTestBurnIn extends JmsTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(JMSConsumerTest.class);

    public StompJmsDestination destination;
    public String destinationType;
    public int deliveryMode;
    public boolean durableConsumer;
    public int messageCount = 10000;
    public int messageSize = 1024;

    public static Test suite() {
        return suite(LoadTestBurnIn.class);
    }

    protected void setUp() throws Exception {
        LOG.info("Start: " + getName());
        super.setUp();
    }

    protected void tearDown() throws Exception {
        try {
            super.tearDown();
        } catch (Throwable e) {
            e.printStackTrace(System.out);
        } finally {
            LOG.info("End: " + getName());
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }





    public void initCombosForTestSendReceive() {
        addCombinationValues("deliveryMode", new Object[] {Integer.valueOf(DeliveryMode.NON_PERSISTENT),Integer.valueOf(DeliveryMode.PERSISTENT),
                                                           });
        addCombinationValues("messageSize", new Object[] {Integer.valueOf(101), Integer.valueOf(102),
                                                          Integer.valueOf(103), Integer.valueOf(104),
                                                          Integer.valueOf(105), Integer.valueOf(106),
                                                          Integer.valueOf(107), Integer.valueOf(108)});
    }

    public void testSendReceive() throws Exception {


        connection.setClientID(getName());
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = createDestination("/queue/");
        MessageConsumer consumer;


        consumer = session.createConsumer(destination);

        profilerPause("Ready: ");

        final CountDownLatch producerDoneLatch = new CountDownLatch(1);

        // Send the messages, async
        new Thread() {
            public void run() {
                Connection connection2 = null;
                try {
                    connection2 = factory.createConnection();
                    Session session = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    MessageProducer producer = session.createProducer(destination);
                    producer.setDeliveryMode(deliveryMode);
                    for (int i = 0; i < messageCount; i++) {
                        BytesMessage m = session.createBytesMessage();
                        m.writeBytes(new byte[messageSize]);
                        producer.send(m);
                    }
                    producer.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                } finally {
                    safeClose(connection2);
                    producerDoneLatch.countDown();
                }

            }
        }.start();

        // Make sure all the messages were delivered.
        Message message = null;
        for (int i = 0; i < messageCount; i++) {
            message = consumer.receive(5000);
            assertNotNull("Did not get message: " + i, message);
        }

        profilerPause("Done: ");

        assertNull(consumer.receiveNoWait());

        // Make sure the producer thread finishes.
        assertTrue(producerDoneLatch.await(5, TimeUnit.SECONDS));
    }

}
