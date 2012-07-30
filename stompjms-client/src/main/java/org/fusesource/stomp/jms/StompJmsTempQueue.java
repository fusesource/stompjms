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

import javax.jms.TemporaryQueue;


/**
 * TemporaryQueue
 */
public class StompJmsTempQueue extends StompJmsDestination implements TemporaryQueue {

    public StompJmsTempQueue() {
        super(null, null);
    }

    public StompJmsTempQueue(String prefix, String name) {
        super(prefix, name);
    }

    public StompJmsTempQueue copy() {
        final StompJmsTempQueue copy = new StompJmsTempQueue();
        copy.setProperties(getProperties());
        return copy;
    }

    /**
     * @see javax.jms.TemporaryQueue#delete()
     */
    public void delete() {
        // TODO: stomp does not really have a way to delete destinations.. :(
    }

    /**
     * @return name
     * @see javax.jms.Queue#getQueueName()
     */
    public String getQueueName() {
        return getName();
    }
}
