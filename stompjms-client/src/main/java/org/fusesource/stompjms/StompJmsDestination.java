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

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.stompjms.channel.StompChannel;
import org.fusesource.stompjms.jndi.JNDIStorable;

import javax.jms.InvalidDestinationException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;


/**
 * Jms Destination
 */
public abstract class StompJmsDestination extends JNDIStorable implements Externalizable, javax.jms.Destination,
        Comparable<StompJmsDestination> {
    public static final String QUEUE_QUALIFIED_PREFIX = "/queue/";
    public static final String TOPIC_QUALIFIED_PREFIX = "/topic/";
    public static final String TEMP_QUEUE_QUALIFED_PREFIX = "/temp-queue/";
    public static final String TEMP_TOPIC_QUALIFED_PREFIX = "/temp-topic/";
    protected transient String physicalName;
    protected transient boolean topic;
    protected transient boolean temporary;
    protected transient int hashValue;
    protected transient String toString;
    protected transient AsciiBuffer buffer;

    protected StompJmsDestination(String name) {
        setPhysicalName(name);
    }

    public String toString() {
        if (toString == null) {
            toString = getType() + getPhysicalName();
        }
        return toString;
    }

    public AsciiBuffer toBuffer() {
        if (buffer == null) {
            buffer = StompChannel.encodeHeader(toString());
        }
        return buffer;
    }

    protected abstract String getType();


    /**
     * @return name of destination
     */
    public String getPhysicalName() {
        return this.physicalName;
    }

    private void setPhysicalName(String physicalName) {
        this.physicalName = physicalName;
        this.toString = null;
        this.buffer = null;

    }

    /**
     * @return the topic
     */
    public boolean isTopic() {
        return this.topic;
    }

    /**
     * @return the temporary
     */
    public boolean isTemporary() {
        return this.temporary;
    }

    /**
     * @return true if a Topic
     */
    public boolean isQueue() {
        return !this.topic;
    }

    /**
     * @param props
     */
    @Override
    protected void buildFromProperties(Map<String, String> props) {

        setPhysicalName(getProperty(props, "name", ""));
        Boolean bool = Boolean.valueOf(getProperty(props, "topic", Boolean.TRUE.toString()));
        this.topic = bool.booleanValue();
        bool = Boolean.valueOf(getProperty(props, "temporary", Boolean.FALSE.toString()));
        this.temporary = bool.booleanValue();
    }

    /**
     * @param props
     */
    @Override
    protected void populateProperties(Map<String, String> props) {
        props.put("name", getPhysicalName());
        props.put("topic", Boolean.toString(isTopic()));
        props.put("temporary", Boolean.toString(isTemporary()));
    }

    /**
     * @param other the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(StompJmsDestination other) {
        if (other != null) {
            if (isTemporary() == other.isTemporary()) {
                return getPhysicalName().compareTo(other.getPhysicalName());
            }
            return -1;
        }
        return -1;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StompJmsDestination d = (StompJmsDestination) o;
        return getPhysicalName().equals(d.getPhysicalName());
    }

    public int hashCode() {
        if (hashValue == 0) {
            hashValue = getPhysicalName().hashCode();
        }
        return hashValue;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(getPhysicalName());
        out.writeBoolean(isTopic());
        out.writeBoolean(isTemporary());
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        setPhysicalName(in.readUTF());
        this.topic = in.readBoolean();
        this.temporary = in.readBoolean();
    }

    public static StompJmsDestination createDestination(String name) throws InvalidDestinationException {

        if (name.startsWith(QUEUE_QUALIFIED_PREFIX)) {
            return new StompJmsQueue(name.substring(QUEUE_QUALIFIED_PREFIX.length()));
        } else if (name.startsWith(TOPIC_QUALIFIED_PREFIX)) {
            return new StompJmsTopic(name.substring(TOPIC_QUALIFIED_PREFIX.length()));
        } else if (name.startsWith(TEMP_QUEUE_QUALIFED_PREFIX)) {
            return new StompJmsTempQueue(name.substring(TEMP_QUEUE_QUALIFED_PREFIX.length()));
        } else if (name.startsWith(TEMP_TOPIC_QUALIFED_PREFIX)) {
            return new StompJmsTempTopic(name.substring(TEMP_TOPIC_QUALIFED_PREFIX.length()));
        } else {
            throw new InvalidDestinationException("Invalid Destination name: " + name);
        }

    }

}
