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

import javax.jms.TemporaryTopic;


/**
 * Temporary Topic
 */
public class StompJmsTempTopic extends StompJmsDestination implements TemporaryTopic {

    private final StompJmsConnection connection;

    /**
     * Constructor
     *
     * @param name
     */
    public StompJmsTempTopic(StompJmsConnection connection, String name) {
        super(connection.tempTopicPrefix, name);
        this.connection = connection;
        this.topic = true;
    }

    protected String getType() {
        return connection.tempTopicPrefix;
    }

    /**
     * @see javax.jms.TemporaryTopic#delete()
     */
    public void delete() {
        // TODO Auto-generated method stub

    }

    /**
     * @return name
     * @see javax.jms.Topic#getTopicName()
     */
    public String getTopicName() {
        return getPhysicalName();
    }
}
