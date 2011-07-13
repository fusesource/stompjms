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

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.stompjms.client.StompFrame;
import org.fusesource.stompjms.client.callback.Callback;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Connection {

    private final org.fusesource.stompjms.client.callback.Connection connection;

    private LinkedList<CallbackFuture<StompFrame>> receiveFutures = new LinkedList<CallbackFuture<StompFrame>>();
    private LinkedList<StompFrame> receivedFrames = new LinkedList<StompFrame>();

    Connection(org.fusesource.stompjms.client.callback.Connection connection) {
        this.connection = connection;
        this.connection.receive(new Callback<StompFrame>() {
            @Override
            public void failure(Throwable value) {
                getDispatchQueue().assertExecuting();
                ArrayList<CallbackFuture<StompFrame>> tmp = new ArrayList<CallbackFuture<StompFrame>>(receiveFutures);
                receiveFutures.clear();
                for (CallbackFuture<StompFrame> future : tmp) {
                    future.failure(value);
                }
            }

            @Override
            public void success(StompFrame value) {
                getDispatchQueue().assertExecuting();
                if( receiveFutures.isEmpty() ) {
                    receivedFrames.add(value);
                } else {
                    receiveFutures.removeFirst().success(value);
                }
            }
        });
        this.connection.resume();
    }

    public StompFrame connectedFrame() {
        return connection.connectedFrame();
    }

    private DispatchQueue getDispatchQueue() {
        return this.connection.getDispatchQueue();
    }

    public CallbackFuture<Void> close() {
        final CallbackFuture<Void> future = new CallbackFuture<Void>();
        connection.close(new Runnable() {
            public void run() {
                future.success(null);
            }
        });
        return future;
    }


    public AsciiBuffer nextId() {
        return connection.nextId();
    }

    public AsciiBuffer nextId(String prefix) {
        return connection.nextId(prefix);
    }

    public CallbackFuture<StompFrame> request(final StompFrame frame) {
        final CallbackFuture<StompFrame> future = new CallbackFuture<StompFrame>();
        connection.getDispatchQueue().execute(new Runnable() {
            public void run() {
                connection.request(frame, future);
            }
        });
        return future;
    }

    public CallbackFuture<Void> send(final StompFrame frame) {
        final CallbackFuture<Void> future = new CallbackFuture<Void>();
        connection.getDispatchQueue().execute(new Runnable() {
            public void run() {
                connection.send(frame, future);
            }
        });
        return future;
    }

    public CallbackFuture<StompFrame> receive() {
        final CallbackFuture<StompFrame> future = new CallbackFuture<StompFrame>();
        getDispatchQueue().execute(new Runnable(){
            public void run() {
                if( connection.getFailure()!=null ) {
                    future.failure(connection.getFailure());
                } else {
                    if( receivedFrames.isEmpty() ) {
                        receiveFutures.add(future);
                    } else {
                        future.success(receivedFrames.removeFirst());
                    }
                }
            }
        });
        return future;
    }

    public void resume() {
        connection.resume();
    }

    public void suspend() {
        connection.suspend();
    }
}
