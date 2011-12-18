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

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.stomp.jms.message.StompJmsMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MessageQueue {

    static class QueueEntry {
        private final StompJmsMessage message;
        private final int size;
        QueueEntry(StompJmsMessage message, int size) {
            this.message = message;
            this.size = size;
        }
    }

    private final long maxSize;
    private final Object mutex = new Object();
    private final LinkedList<QueueEntry> list = new LinkedList<QueueEntry>();
    private boolean closed;
    private boolean running;
    private long size;

    public MessageQueue(long maxSize) {
        this.maxSize = maxSize;
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#enqueue(org.apache.activemq.command.MessageDispatch)
     */
    public void enqueue(StompJmsMessage message) {
        QueueEntry entry = new QueueEntry(message,  message.getFrame().size());
        synchronized (mutex) {
            list.addLast(entry);
            size += entry.size;
            mutex.notify();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#enqueueFirst(org.apache.activemq.command.MessageDispatch)
     */
    public void enqueueFirst(StompJmsMessage message) {
        QueueEntry entry = new QueueEntry(message,  message.getFrame().size());
        synchronized (mutex) {
            list.addFirst(entry);
            size += entry.size;
            mutex.notify();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#isEmpty()
     */
    public boolean isEmpty() {
        synchronized (mutex) {
            return list.isEmpty();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#dequeue(long)
     */
    public StompJmsMessage dequeue(long timeout) throws InterruptedException {
        synchronized (mutex) {
            // Wait until the consumer is ready to deliver messages.
            while (timeout != 0 && !closed && (list.isEmpty() || !running)) {
                if (timeout == -1) {
                    mutex.wait();
                } else {
                    mutex.wait(timeout);
                    break;
                }
            }
            if (closed || !running || list.isEmpty()) {
                return null;
            }
            QueueEntry entry = list.removeFirst();
            size -= entry.size;
            return entry.message;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#dequeueNoWait()
     */
    public StompJmsMessage dequeueNoWait() {
        synchronized (mutex) {
            if (closed || !running || list.isEmpty()) {
                return null;
            }
            QueueEntry entry = list.removeFirst();
            size -= entry.size;
            return entry.message;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#peek()
     */
    public StompJmsMessage peek() {
        synchronized (mutex) {
            if (closed || !running || list.isEmpty()) {
                return null;
            }
            return list.getFirst().message;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#start()
     */
    public void start() {
        synchronized (mutex) {
            running = true;
            mutex.notifyAll();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#stop()
     */
    public void stop() {
        synchronized (mutex) {
            running = false;
            mutex.notifyAll();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#close()
     */
    public void close() {
        synchronized (mutex) {
            if (!closed) {
                running = false;
                closed = true;
            }
            mutex.notifyAll();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#clear()
     */
    public void clear() {
        synchronized (mutex) {
            list.clear();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#isClosed()
     */
    public boolean isClosed() {
        return closed;
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#size()
     */
    public int size() {
        synchronized (mutex) {
            return list.size();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#getMutex()
     */
    public Object getMutex() {
        return mutex;
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#isRunning()
     */
    public boolean isRunning() {
        return running;
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#removeAll()
     */
    public List<StompJmsMessage> removeAll() {
        synchronized (mutex) {
            ArrayList<StompJmsMessage> rc = new ArrayList<StompJmsMessage>(list.size());
            for (QueueEntry entry : list) {
                rc.add(entry.message);
            }
            list.clear();
            size = 0;
            return rc;
        }
    }

    public void rollback(AsciiBuffer transactionId) {
        synchronized (mutex) {
            if (transactionId != null && transactionId.isEmpty() == false) {
                List<QueueEntry> tmp = new ArrayList<QueueEntry>(this.list);
                for (QueueEntry entry : tmp) {
                    AsciiBuffer tid = entry.message.getTransactionId();
                    if (tid != null && tid.equals(transactionId)) {
                        this.list.remove(entry);
                        this.size -= entry.size;
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        synchronized (mutex) {
            return list.toString();
        }
    }

    public boolean isFull() {
        return this.size >= maxSize;
    }
}
