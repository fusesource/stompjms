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

import javax.jms.Queue;

/**
 * Queue implementation
 */
public class StompJmsQueue extends StompJmsDestination implements Queue {

    public StompJmsQueue() {
        super(null, null);
    }

    public StompJmsQueue(StompJmsConnection connection, String name) {
        this(connection.queuePrefix, name);
    }

    /**
     * Constructor
     *
     * @param name
     */
    public StompJmsQueue(String type, String name) {
        super(type, name);
    }

    /**
     * @return name
     * @see javax.jms.Queue#getQueueName()
     */
    public String getQueueName() {
        return getPhysicalName();
    }

}
