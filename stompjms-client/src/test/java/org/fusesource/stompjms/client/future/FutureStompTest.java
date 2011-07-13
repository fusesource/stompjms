/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stompjms.client.future;

import junit.framework.TestCase;
import org.fusesource.stompjms.client.ApolloBroker;
import org.fusesource.stompjms.client.Stomp;
import org.fusesource.stompjms.client.StompFrame;

import static org.fusesource.stompjms.client.Constants.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class FutureStompTest extends TestCase {
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

    public void testFutureInterface() throws Exception {
        Future<Connection> future = Stomp.future("localhost", broker.port).connect();
        Connection connection = future.await();

        // Lets setup a receive.. this does not block until you await it..
        CallbackFuture<StompFrame> receiveFuture = connection.receive();

        StompFrame frame = new StompFrame(SUBSCRIBE);
        frame.addHeader(DESTINATION, StompFrame.encodeHeader("/queue/test"));
        frame.addHeader(ID, connection.nextId());
        CallbackFuture<StompFrame> response = connection.request(frame);

        // This unblocks once the response frame is received.
        assertNotNull(response.await());

        frame = new StompFrame(SEND);
        frame.addHeader(DESTINATION, StompFrame.encodeHeader("/queue/test"));
        frame.addHeader(MESSAGE_ID, StompFrame.encodeHeader("test"));
        CallbackFuture<Void> sendFuture = connection.send(frame);

        // This unblocks once the frame is accepted by the socket.  Use it
        // to avoid flow control issues.
        sendFuture.await();

        // Try to get the received message.
        StompFrame received = receiveFuture.await();
        assertTrue(received.action().equals(MESSAGE));
        assertTrue(received.getHeader(MESSAGE_ID).toString().equals("test"));
    }
}
