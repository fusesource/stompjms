/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms;

import org.fusesource.stompjms.message.StompJmsMessage;

/**
 * A listener for BlazeMessages
 */
public interface StompJmsMessageListener {

    /**
     * Called when a Message is available to be processes
     *
     * @param message
     */
    public void onMessage(StompJmsMessage message);
}
