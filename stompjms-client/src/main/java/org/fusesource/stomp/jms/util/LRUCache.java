/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stomp.jms.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Simple LRU Cache
 *
 * @param <K>
 * @param <V>
 * @version $Revision$
 */

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = -342098639681884413L;
    protected int maxCacheSize = 10000;

    /**
     * Default constructor for an LRU Cache The default capacity is 10000
     */
    public LRUCache() {
        this(0, 10000, 0.75f, true);
    }

    /**
     * Constructs a LRUCache with a maximum capacity
     *
     * @param maximumCacheSize
     */
    public LRUCache(int maximumCacheSize) {
        this(0, maximumCacheSize, 0.75f, true);
    }

    /**
     * Constructs an empty <tt>LRUCache</tt> instance with the specified
     * initial capacity, maximumCacheSize,load factor and ordering mode.
     *
     * @param initialCapacity  the initial capacity.
     * @param maximumCacheSize
     * @param loadFactor       the load factor.
     * @param accessOrder      the ordering mode - <tt>true</tt> for access-order,
     *                         <tt>false</tt> for insertion-order.
     * @throws IllegalArgumentException if the initial capacity is negative or
     *                                  the load factor is non-positive.
     */

    public LRUCache(int initialCapacity, int maximumCacheSize, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
        this.maxCacheSize = maximumCacheSize;
    }

    /**
     * @return Returns the maxCacheSize.
     */
    public int getMaxCacheSize() {
        return this.maxCacheSize;
    }

    /**
     * @param maxCacheSize The maxCacheSize to set.
     */
    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > this.maxCacheSize;
    }
}

