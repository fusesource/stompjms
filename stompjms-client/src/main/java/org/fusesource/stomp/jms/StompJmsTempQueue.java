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

    public StompJmsTempQueue(String prefix, String name) {
        super(prefix, name);
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
