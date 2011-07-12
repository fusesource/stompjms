/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms.util;


import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.stompjms.channel.StompFrame;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * state on a request
 */
public class SendRequest {
    private final AtomicBoolean done = new AtomicBoolean();
    private StompFrame response;

    public StompFrame get(long timeout) {
        synchronized (this.done) {
            if (this.done.get() == false && this.response == null) {
                try {
                    this.done.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return this.response;
    }

    public void put(AsciiBuffer id, StompFrame r) {
        this.response = r;
        cancel();
    }

    void cancel() {
        this.done.set(true);
        synchronized (this.done) {
            this.done.notifyAll();
        }
    }
}
