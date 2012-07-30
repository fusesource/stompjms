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

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class StompJmsPrefetch {

    static final StompJmsPrefetch DEFAULT = new StompJmsPrefetch();

    int maxMessages = 1;
    long maxBytes = 64*1024;

    public StompJmsPrefetch() {
    }

    public StompJmsPrefetch(int maxMessages) {
        this(maxMessages, 0);
    }

    public StompJmsPrefetch(StompJmsPrefetch other) {
        this(other.maxMessages, other.maxBytes);
    }

    public StompJmsPrefetch(int maxMessages, long maxBytes) {
        this.maxBytes = maxBytes;
        this.maxMessages = maxMessages;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public void setMaxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StompJmsPrefetch)) return false;

        StompJmsPrefetch that = (StompJmsPrefetch) o;

        if (maxBytes != that.maxBytes) return false;
        if (maxMessages != that.maxMessages) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = maxMessages;
        result = 31 * result + (int) (maxBytes ^ (maxBytes >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "StompJmsPrefetch{" +
                "maxBytes=" + maxBytes +
                ", maxMessages=" + maxMessages +
                '}';
    }
}
