/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stompjms.client.callback;

import junit.framework.TestCase;
import org.fusesource.stompjms.client.ApolloBroker;
import org.fusesource.stompjms.client.Stomp;
import org.fusesource.stompjms.client.StompFrame;
import org.fusesource.stompjms.client.future.CallbackFuture;

import static org.fusesource.stompjms.client.Constants.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class CallbackStompTest extends TestCase {
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

    public void testCallbackInterface() throws Exception {
        final CallbackFuture<StompFrame> result = new CallbackFuture<StompFrame>();
        Stomp.callback("localhost", broker.port).connect(new Callback<Connection>(){
            @Override
            public void failure(Throwable value) {
                result.failure(value);
            }

            @Override
            public void success(final Connection connection) {
                connection.receive(new Callback<StompFrame>(){
                    @Override
                    public void failure(Throwable value) {
                        result.failure(value);
                        connection.close(null);
                    }

                    @Override
                    public void success(StompFrame value) {
                        result.success(value);
                        connection.close(null);
                    }
                });

                connection.resume();

                StompFrame frame = new StompFrame(SUBSCRIBE);
                frame.addHeader(DESTINATION, StompFrame.encodeHeader("/queue/test"));
                frame.addHeader(ID, connection.nextId());
                connection.request(frame, new Callback<StompFrame>(){
                    @Override
                    public void failure(Throwable value) {
                        result.failure(value);
                        connection.close(null);
                    }

                    @Override
                    public void success(StompFrame reply) {
                        StompFrame frame = new StompFrame(SEND);
                        frame.addHeader(DESTINATION, StompFrame.encodeHeader("/queue/test"));
                        frame.addHeader(MESSAGE_ID, StompFrame.encodeHeader("test"));
                        connection.send(frame, null);
                    }
                });

            }
        });
        StompFrame received = result.await();
        assertTrue(received.action().equals(MESSAGE));
        assertTrue(received.getHeader(MESSAGE_ID).toString().equals("test"));
    }
}
