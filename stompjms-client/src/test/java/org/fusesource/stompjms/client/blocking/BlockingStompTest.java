/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stompjms.client.blocking;

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
public class BlockingStompTest extends TestCase {
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

    public void testBlockingInterface() throws Exception {
        Connection connection = Stomp.blocking("localhost", broker.port).connect();

        StompFrame frame = new StompFrame(SUBSCRIBE);
        frame.addHeader(DESTINATION, StompFrame.encodeHeader("/queue/test"));
        frame.addHeader(ID, connection.nextId());
        StompFrame response = connection.request(frame);

        // This unblocks once the response frame is received.
        assertNotNull(response);

        frame = new StompFrame(SEND);
        frame.addHeader(DESTINATION, StompFrame.encodeHeader("/queue/test"));
        frame.addHeader(MESSAGE_ID, StompFrame.encodeHeader("test"));
        connection.send(frame);

        // Try to get the received message.
        StompFrame received = connection.receive();
        assertTrue(received.action().equals(MESSAGE));
        assertTrue(received.getHeader(MESSAGE_ID).toString().equals("test"));
    }
}
