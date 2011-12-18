/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stomp.jms;

import javax.jms.IllegalStateException;

/**
 * An exception thrown when attempt is made to use a connection when the connection has been closed.
 *
 * @version $Revision: 1.2 $
 */
public class StompJmsConnectionClosedException extends IllegalStateException {
    private static final long serialVersionUID = -7681404582227153308L;

    public StompJmsConnectionClosedException() {
        super("The connection is already closed", "AlreadyClosed");
    }
}
