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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * state on a request
 */
public class SendRequest {
    private final CountDownLatch done = new CountDownLatch(1);
    volatile private StompFrame response;

    public StompFrame get(long timeout) throws InterruptedException {
        if( timeout == 0 ) {
        } else if( timeout < 0 ) {
            done.await();
        } else {
            done.await(timeout, TimeUnit.MILLISECONDS);
        }
        return this.response;
    }

    public void put(AsciiBuffer id, StompFrame r) {
        this.response = r;
        cancel();
    }

    void cancel() {
        done.countDown();
    }
}
