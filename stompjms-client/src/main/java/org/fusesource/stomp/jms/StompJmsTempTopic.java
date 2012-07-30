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

    public StompJmsTempTopic() {
        this(null, null);
    }
    public StompJmsTempTopic(String prefix, String name) {
        super(prefix, name);
    }

    public StompJmsTempTopic copy() {
        final StompJmsTempTopic copy = new StompJmsTempTopic();
        copy.setProperties(getProperties());
        return copy;
    }

    /**
     * @see javax.jms.TemporaryTopic#delete()
     */
    public void delete() {
        // TODO: stomp does not really have a way to delete destinations.. :(
    }

    /**
     * @return name
     * @see javax.jms.Topic#getTopicName()
     */
    public String getTopicName() {
        return getName();
    }
}
