/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms.channel;


/**
 * Command indicating that an invalid Stomp Frame was received.
 *
 * @author <a href="http://hiramchirino.com">chirino</a>
 */
public class StompFrameError extends StompFrame {

    private final ProtocolException exception;

    public StompFrameError(ProtocolException exception) {
        this.exception = exception;
    }

    public ProtocolException getException() {
        return exception;
    }

}
