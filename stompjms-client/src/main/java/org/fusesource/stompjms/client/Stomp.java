/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stompjms.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;


/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Stomp {

    private Stomp() {}

    private static final long KEEP_ALIVE = Long.parseLong(System.getProperty("stompjms.thread.keep_alive", ""+1000));
    private static final long STACK_SIZE = Long.parseLong(System.getProperty("stompjms.thread.stack_size", ""+1024*512));
    private static ThreadPoolExecutor blockingThreadPool;

    public synchronized static ThreadPoolExecutor getBlockingThreadPool() {
        if( blockingThreadPool == null ) {
            blockingThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, KEEP_ALIVE, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread rc = new Thread(null, r, "Stomp JMS Task", STACK_SIZE);
                        rc.setDaemon(true);
                        return rc;
                    }
                }) {

                    @Override
                    public void shutdown() {
                        // we don't ever shutdown since we are shared..
                    }

                    @Override
                    public List<Runnable> shutdownNow() {
                        // we don't ever shutdown since we are shared..
                        return Collections.emptyList();
                    }
                };
        }
        return blockingThreadPool;
    }

    static public org.fusesource.stompjms.client.callback.ConnectionBuilder callback(URI uri) {
        return new org.fusesource.stompjms.client.callback.ConnectionBuilder(uri);
    }
    static public org.fusesource.stompjms.client.callback.ConnectionBuilder callback(String uri) throws URISyntaxException {
        return callback(new URI(uri));
    }
    static public org.fusesource.stompjms.client.callback.ConnectionBuilder callback(String host, int port) throws URISyntaxException {
        return callback("tcp://"+host+":"+port);
    }

    static public org.fusesource.stompjms.client.future.ConnectionBuilder future(URI uri) {
        return new org.fusesource.stompjms.client.future.ConnectionBuilder(callback(uri));
    }
    static public org.fusesource.stompjms.client.future.ConnectionBuilder future(String uri) throws URISyntaxException {
        return future(new URI(uri));
    }
    static public org.fusesource.stompjms.client.future.ConnectionBuilder future(String host, int port) throws URISyntaxException {
        return future("tcp://"+host+":"+port);
    }

    static public org.fusesource.stompjms.client.blocking.ConnectionBuilder blocking(URI uri) {
        return new org.fusesource.stompjms.client.blocking.ConnectionBuilder(future(uri));
    }
    static public org.fusesource.stompjms.client.blocking.ConnectionBuilder blocking(String uri) throws URISyntaxException {
        return blocking(new URI(uri));
    }
    static public org.fusesource.stompjms.client.blocking.ConnectionBuilder blocking(String host, int port) throws URISyntaxException {
        return blocking("tcp://"+host+":"+port);
    }

}
