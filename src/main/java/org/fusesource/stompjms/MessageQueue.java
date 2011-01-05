/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.stompjms;

import org.fusesource.stompjms.message.StompJmsMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MessageQueue {

    private final Object mutex = new Object();
    private final LinkedList<StompJmsMessage> list;
    private boolean closed;
    private boolean running;

    public MessageQueue() {
        this.list = new LinkedList<StompJmsMessage>();
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#enqueue(org.apache.activemq.command.MessageDispatch)
     */
    public void enqueue(StompJmsMessage message) {
        synchronized (mutex) {
            list.addLast(message);
            mutex.notify();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.activemq.MessageDispatchChannelI#enqueueFirst(org.apache.activemq.command.MessageDispatch)
     */
    public void enqueueFirst(StompJmsMessage message) {
        synchronized (mutex) {
            list.addFirst(message);
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
            return list.removeFirst();
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
            return list.removeFirst();
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
            return list.getFirst();
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
            ArrayList<StompJmsMessage> rc = new ArrayList<StompJmsMessage>(list);
            list.clear();
            return rc;
        }
    }

    public void rollback(String transactionId) {
        synchronized (mutex) {
            if (transactionId != null && transactionId.isEmpty() == false) {
                List<StompJmsMessage> tmp = new ArrayList<StompJmsMessage>(this.list);
                for (StompJmsMessage message:tmp){
                    String tid = message.getTransactionId();
                    if (tid != null && tid.equals(transactionId)) {
                        this.list.remove(message);
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
}
