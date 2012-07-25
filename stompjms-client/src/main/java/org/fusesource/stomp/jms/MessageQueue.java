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

import org.fusesource.stomp.jms.message.StompJmsMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MessageQueue {

    protected static class QueueEntry {
        final StompJmsMessage message;
        final int size;
        QueueEntry(StompJmsMessage message, int size) {
            this.message = message;
            this.size = size;
        }
    }

    protected final long maxSize;
    protected final LinkedList<QueueEntry> list = new LinkedList<QueueEntry>();
    protected boolean closed;
    protected boolean running;
    protected long size;

    public MessageQueue(long maxSize) {
        this.maxSize = maxSize;
    }

    public void enqueue(StompJmsMessage message) {
        QueueEntry entry = new QueueEntry(message,  message.getFrame().size());
        synchronized (this) {
            list.addLast(entry);
            size += entry.size;
            this.notify();
        }
    }

    public boolean isEmpty() {
        synchronized (this) {
            return list.isEmpty();
        }
    }

    public StompJmsMessage dequeue(long timeout) throws InterruptedException {
        synchronized (this) {
            // Wait until the consumer is ready to deliver messages.
            while (timeout != 0 && !closed && (list.isEmpty() || !running)) {
                if (timeout == -1) {
                    this.wait();
                } else {
                    this.wait(timeout);
                    break;
                }
            }
            if (closed || !running || list.isEmpty()) {
                return null;
            }
            QueueEntry entry = list.removeFirst();
            size -= entry.size;
            removed(entry);
            return entry.message;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#dequeueNoWait()
     */
    public StompJmsMessage dequeueNoWait() {
        synchronized (this) {
            if (closed || !running || list.isEmpty()) {
                return null;
            }
            QueueEntry entry = list.removeFirst();
            size -= entry.size;
            removed(entry);
            return entry.message;
        }
    }

    protected void removed(QueueEntry entry) {
    }

    public void start() {
        synchronized (this) {
            running = true;
            this.notifyAll();
        }
    }

    public void stop() {
        synchronized (this) {
            running = false;
            this.notifyAll();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void close() {
        synchronized (this) {
            if (!closed) {
                running = false;
                closed = true;
            }
            this.notifyAll();
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public int size() {
        synchronized (this) {
            return list.size();
        }
    }

    public void clear() {
        synchronized (this) {
            list.clear();
        }
    }

    public List<StompJmsMessage> removeAll() {
        synchronized (this) {
            ArrayList<StompJmsMessage> rc = new ArrayList<StompJmsMessage>(list.size());
            for (QueueEntry entry : list) {
                rc.add(entry.message);
            }
            list.clear();
            size = 0;
            return rc;
        }
    }

    @Override
    public String toString() {
        synchronized (this) {
            return list.toString();
        }
    }

    public boolean isFull() {
        synchronized (this) {
            return this.size >= maxSize;
        }
    }
}
