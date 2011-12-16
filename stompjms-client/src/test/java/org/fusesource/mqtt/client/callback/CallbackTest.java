/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.mqtt.client.callback;

import junit.framework.TestCase;
import org.fusesource.hawtbuf.Buffer;
import static org.fusesource.hawtbuf.Buffer.*;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.fusesource.stompjms.client.future.CallbackFuture;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class CallbackTest extends TestCase {
//    ApolloBroker broker = new ApolloBroker();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
//        broker.start();
    }

    @Override
    protected void tearDown() throws Exception {
//        broker.stop();
        super.tearDown();
    }

    public void testCallbackInterface() throws Exception {
        final CallbackFuture<Buffer> result = new CallbackFuture<Buffer>();
        MQTT.callback("localhost", 1883/* broker.port*/).clientId("Hiram").connect(new Callback<Connection>() {
            @Override
            public void failure(Throwable value) {
                result.failure(value);
            }

            @Override
            public void success(final Connection connection) {

                connection.listener(new Listener() {
                    @Override
                    public void failure(Throwable value) {
                        result.failure(value);
                        connection.close(null);
                    }

                    @Override
                    public void onPublish(UTF8Buffer topic, Buffer payload, Runnable onComplete) {
                        result.success(payload);
                        onComplete.run();
                    }
                });

                connection.resume();
                connection.subscribe(new Topic[]{new Topic(utf8("foo"), QoS.AT_LEAST_ONCE)}, new Callback<byte[]>(){
                    @Override
                    public void failure(Throwable value) {
                        result.failure(value);
                        connection.close(null);
                    }

                    @Override
                    public void success(byte[] value) {
                        connection.publish(utf8("foo"), ascii("Hello"), null);
                    }
                });

            }
        });

        assertEquals(ascii("Hello"), result.await().ascii());
    }
}
