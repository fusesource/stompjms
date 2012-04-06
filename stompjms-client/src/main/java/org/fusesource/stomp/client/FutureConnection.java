/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stomp.client;

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.stomp.codec.StompFrame;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class FutureConnection {

    private final CallbackConnection connection;

    private LinkedList<Promise<StompFrame>> receiveFutures = new LinkedList<Promise<StompFrame>>();
    private LinkedList<StompFrame> receivedFrames = new LinkedList<StompFrame>();

    FutureConnection(CallbackConnection connection) {
        this.connection = connection;
        this.connection.receive(new Callback<StompFrame>() {
            @Override
            public void onFailure(Throwable value) {
                getDispatchQueue().assertExecuting();
                ArrayList<Promise<StompFrame>> tmp = new ArrayList<Promise<StompFrame>>(receiveFutures);
                receiveFutures.clear();
                for (Promise<StompFrame> future : tmp) {
                    future.onFailure(value);
                }
            }

            @Override
            public void onSuccess(StompFrame value) {
                getDispatchQueue().assertExecuting();
                if( receiveFutures.isEmpty() ) {
                    receivedFrames.add(value);
                } else {
                    receiveFutures.removeFirst().onSuccess(value);
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

    public Future<Void> close() {
        final Promise<Void> future = new Promise<Void>();
        connection.close(new Runnable() {
            public void run() {
                future.onSuccess(null);
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

    public Future<StompFrame> request(final StompFrame frame) {
        final Promise<StompFrame> future = new Promise<StompFrame>();
        connection.getDispatchQueue().execute(new Task() {
            public void run() {
                connection.request(frame, future);
            }
        });
        return future;
    }

    public Future<Void> send(final StompFrame frame) {
        final Promise<Void> future = new Promise<Void>();
        connection.getDispatchQueue().execute(new Task() {
            public void run() {
                connection.send(frame, future);
            }
        });
        return future;
    }

    public Future<StompFrame> receive() {
        final Promise<StompFrame> future = new Promise<StompFrame>();
        getDispatchQueue().execute(new Task(){
            public void run() {
                if( connection.getFailure()!=null ) {
                    future.onFailure(connection.getFailure());
                } else {
                    if( receivedFrames.isEmpty() ) {
                        receiveFutures.add(future);
                    } else {
                        future.onSuccess(receivedFrames.removeFirst());
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
