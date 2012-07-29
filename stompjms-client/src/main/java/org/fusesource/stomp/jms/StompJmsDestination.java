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
import org.fusesource.stomp.codec.StompFrame;
import org.fusesource.stomp.jms.jndi.JNDIStorable;

import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;


/**
 * Jms Destination
 */
public class StompJmsDestination extends JNDIStorable implements Externalizable, javax.jms.Destination,
        Comparable<StompJmsDestination> {

    protected transient String prefix;
    protected transient String name;
    protected transient boolean topic;
    protected transient boolean temporary;
    protected transient int hashValue;
    protected transient String toString;
    protected transient AsciiBuffer buffer;
    protected transient Map<String, String> subscribeHeaders;

    public StompJmsDestination() {
    }
    public StompJmsDestination(String name) {
        this("", name);
    }

    public StompJmsDestination(String prefix, String name) {
        this.prefix = prefix;
        setName(name);
    }

    public String toString() {
        if (toString == null) {
            toString = getPrefix() + getName();
        }
        return toString;
    }

    public AsciiBuffer toBuffer() {
        if (buffer == null) {
            buffer = StompFrame.encodeHeader(toString());
        }
        return buffer;
    }

    public String getPrefix() {
        return prefix;
    }
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return name of destination
     */
    public String getName() {
        return this.name;
    }

    private void setName(String name) {
        this.name = name;
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
        setPrefix(getProperty(props, "prefix", ""));
        setName(getProperty(props, "name", ""));
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
        props.put("prefix", getPrefix());
        props.put("name", getName());
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
                return getName().compareTo(other.getName());
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
        return getName().equals(d.getName());
    }

    public int hashCode() {
        if (hashValue == 0) {
            hashValue = getName().hashCode();
        }
        return hashValue;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(getPrefix());
        out.writeUTF(getName());
        out.writeBoolean(isTopic());
        out.writeBoolean(isTemporary());
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        setPrefix(in.readUTF());
        setName(in.readUTF());
        this.topic = in.readBoolean();
        this.temporary = in.readBoolean();
    }

    public static StompJmsDestination createDestination(StompJmsConnection connection, String name) throws JMSException {

        StompJmsDestination x = connection.isTempQueue(name);
        if ( x !=null ) {
            return x;
        }
        x = connection.isTempTopic(name);
        if ( x !=null ) {
            return x;
        }

        if (name.startsWith(connection.topicPrefix)) {
            return new StompJmsTopic(connection, name.substring(connection.topicPrefix.length()));
        }

        if (name.startsWith(connection.queuePrefix)) {
            return new StompJmsQueue(connection, name.substring(connection.queuePrefix.length()));
        }
        return new StompJmsDestination("", name);

    }

    public Map<String, String> getSubscribeHeaders() {
        return subscribeHeaders;
    }

    public void setSubscribeHeaders(Map<String, String> subscribeHeaders) {
        this.subscribeHeaders = subscribeHeaders;
    }
}
