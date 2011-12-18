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

import java.io.IOException;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtocolException extends IOException {

    private static final long serialVersionUID = -2869735532997332242L;

    private final boolean fatal;

    public ProtocolException() {
        this(null);
    }

    public ProtocolException(String s) {
        this(s, false);
    }

    public ProtocolException(String s, boolean fatal) {
        this(s, fatal, null);
    }

    public ProtocolException(String s, boolean fatal, Throwable cause) {
        super(s);
        this.fatal = fatal;
        initCause(cause);
    }

    public boolean isFatal() {
        return fatal;
    }

}
