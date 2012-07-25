/**
 * Copyright (C) 2012, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stomp.jms;

import org.fusesource.stomp.jms.message.StompJmsMessage;

import java.util.LinkedList;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class TxMessageQueue extends MessageQueue {

    private final LinkedList<QueueEntry> removed = new LinkedList<QueueEntry>();

    public TxMessageQueue(long maxSize) {
        super(maxSize);
    }

    @Override
    protected void removed(QueueEntry entry) {
        removed.addFirst(entry);
    }

    public void commit() {
        synchronized (this) {
            removed.clear();
        }
    }

    public void rollback() {
        synchronized (this) {
            for (QueueEntry entry : removed) {
                entry.message.setJMSRedelivered(true);
                list.addFirst(entry);
                size += entry.size;
            }
            this.notify();
        }
    }

}
