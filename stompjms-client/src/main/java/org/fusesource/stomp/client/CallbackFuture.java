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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class CallbackFuture<T> extends Callback<T> implements Future<T> {

    private final CountDownLatch latch = new CountDownLatch(1);

    Throwable error;
    T value;

    @Override
    public void failure(Throwable value) {
        error = value;
        latch.countDown();
    }

    @Override
    public void success(T value) {
        this.value = value;
        latch.countDown();
    }

    public T await(long amount, TimeUnit unit) throws Exception {
        latch.await(amount, unit);
        return get();
    }

    public T await() throws Exception {
        latch.await();
        return get();
    }

    private T get() throws Exception {
        Throwable e = error;
        if( e !=null ) {
            if( e instanceof RuntimeException ) {
                throw (RuntimeException) e;
            } else if( e instanceof Exception) {
                throw (Exception) e;
            } else if( e instanceof Error) {
                throw (Error) e;
            } else {
                // don't expect to hit this case.
                throw new RuntimeException(e);
            }
        }
        return value;
    }
}
