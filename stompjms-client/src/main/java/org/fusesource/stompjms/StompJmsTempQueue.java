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

import javax.jms.TemporaryQueue;


/**
 * TemporaryQueue
 */
public class StompJmsTempQueue extends StompJmsDestination implements TemporaryQueue {

    /**
     * Constructor
     *
     * @param name
     */
    public StompJmsTempQueue(String name) {
        super(name);
        this.topic = false;
    }

    protected String getType() {
        return StompJmsDestination.TEMP_QUEUE_QUALIFED_PREFIX;
    }

    /**
     * @see javax.jms.TemporaryQueue#delete()
     */
    public void delete() {
        // TODO Auto-generated method stub

    }

    /**
     * @return name
     * @see javax.jms.Queue#getQueueName()
     */
    public String getQueueName() {
        return getPhysicalName();
    }
}
