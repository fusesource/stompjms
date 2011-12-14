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

    private final StompJmsConnection connection;

    /**
     * Constructor
     *
     * @param name
     */
    public StompJmsTempQueue(StompJmsConnection connection, String name) {
        super(connection.tempQueuePrefix, name);
        this.connection = connection;
        this.topic = false;
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
