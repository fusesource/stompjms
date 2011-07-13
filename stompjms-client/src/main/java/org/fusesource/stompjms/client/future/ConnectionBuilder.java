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

import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.stompjms.client.callback.Callback;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.concurrent.Executor;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ConnectionBuilder {
    private org.fusesource.stompjms.client.callback.ConnectionBuilder callback;

    public ConnectionBuilder(org.fusesource.stompjms.client.callback.ConnectionBuilder callback) {
        this.callback = callback;
    }

    public ConnectionBuilder login(String login) {
        callback.login(login);
        return this;
    }

    public ConnectionBuilder passcode(String passcode) {
        callback.passcode(passcode);
        return this;
    }

    public ConnectionBuilder host(String host) {
        callback.login(host);
        return this;
    }

    public ConnectionBuilder blockingExecutor(Executor blockingExecutor) {
        callback.blockingExecutor(blockingExecutor);
        return this;
    }

    public ConnectionBuilder dispatchQueue(DispatchQueue dispatchQueue) {
        callback.dispatchQueue(dispatchQueue);
        return this;
    }

    public ConnectionBuilder localURI(URI localURI) {
        callback.localURI(localURI);
        return this;
    }

    public ConnectionBuilder maxReadRate(int maxReadRate) {
        callback.maxReadRate(maxReadRate);
        return this;
    }

    public ConnectionBuilder maxWriteRate(int maxWriteRate) {
        callback.maxWriteRate(maxWriteRate);
        return this;
    }

    public ConnectionBuilder receiveBufferSize(int receiveBufferSize) {
        callback.receiveBufferSize(receiveBufferSize);
        return this;
    }

    public ConnectionBuilder sendBufferSize(int sendBufferSize) {
        callback.sendBufferSize(sendBufferSize);
        return this;
    }

    public ConnectionBuilder sslContext(SSLContext sslContext) {
        callback.sslContext(sslContext);
        return this;
    }

    public ConnectionBuilder trafficClass(int trafficClass) {
        callback.trafficClass(trafficClass);
        return this;
    }

    public ConnectionBuilder useLocalHost(boolean useLocalHost) {
        callback.useLocalHost(useLocalHost);
        return this;
    }

    public Future<Connection> connect() {
        final CallbackFuture<Connection> future = new CallbackFuture<Connection>();
        callback.connect(new Callback<org.fusesource.stompjms.client.callback.Connection>() {
            public void failure(Throwable value) {
                future.failure(value);
            }
            public void success(org.fusesource.stompjms.client.callback.Connection value) {
                future.success(new Connection(value));
            }
        });
        return future;
    }

}
