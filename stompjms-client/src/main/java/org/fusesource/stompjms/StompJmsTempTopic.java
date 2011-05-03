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

import javax.jms.TemporaryTopic;


/**
 * Temporary Topic
 */
public class StompJmsTempTopic extends StompJmsDestination implements TemporaryTopic {

    /**
     * Constructor
     *
     * @param name
     */
    public StompJmsTempTopic(String name) {
        super(name);
        this.topic = true;
    }

    protected String getType() {
        return StompJmsDestination.TEMP_TOPIC_QUALIFED_PREFIX;
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
