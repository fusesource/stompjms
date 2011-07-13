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

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.stompjms.client.StompFrame;

import java.io.IOException;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Connection {

    private final org.fusesource.stompjms.client.future.Connection connection;

    Connection(org.fusesource.stompjms.client.future.Connection connection) {
        this.connection = connection;
    }

    public void close() throws IOException {
        try {
            connection.close().await();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public AsciiBuffer nextId() {
        return connection.nextId();
    }

    public AsciiBuffer nextId(String prefix) {
        return connection.nextId(prefix);
    }

    public StompFrame request(StompFrame frame) throws IOException {
        try {
            return connection.request(frame).await();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public void send(StompFrame frame) throws IOException {
        try {
            connection.send(frame).await();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public StompFrame receive() throws IOException {
        try {
            return connection.receive().await();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public StompFrame connectedFrame() {
        return connection.connectedFrame();
    }

    public void resume() {
        connection.resume();
    }

    public void suspend() {
        connection.suspend();
    }
}
