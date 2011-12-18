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

import org.fusesource.hawtbuf.AsciiBuffer;

/**
 * Implementation of a TopicSubscriber
 */
public class StompJmsDurableTopicSubscriber extends StompJmsTopicSubscriber {

    public StompJmsDurableTopicSubscriber(AsciiBuffer id, StompJmsSession s, StompJmsDestination destination, boolean noLocal, String selector) {
        super(id, s, destination, noLocal, selector);
    }

    @Override
    public boolean isDurableSubscription() {
        return true;
    }
}

