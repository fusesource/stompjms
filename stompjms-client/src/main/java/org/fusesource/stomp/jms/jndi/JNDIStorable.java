/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stomp.jms.jndi;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

/**
 * Facilitates objects to be stored in JNDI as properties
 */

public abstract class JNDIStorable implements Referenceable, Externalizable {

    private Map<String, String> properties;

    /**
     * Set the properties that will represent the instance in JNDI
     *
     * @param props
     */
    protected abstract void buildFromProperties(Map<String, String> props);

    /**
     * Initialize the instance from properties stored in JNDI
     *
     * @param props
     */

    protected abstract void populateProperties(Map<String, String> props);

    /**
     * set the properties for this instance as retrieved from JNDI
     *
     * @param props
     */

    public synchronized void setProperties(Map<String, String> props) {
        this.properties = props;
        buildFromProperties(props);
    }

    /**
     * Get the properties from this instance for storing in JNDI
     *
     * @return the properties
     */

    public synchronized Map<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new HashMap<String, String>();
        }
        populateProperties(this.properties);
        return this.properties;
    }

    /**
     * Retrive a Reference for this instance to store in JNDI
     *
     * @return the built Reference
     * @throws NamingException if error on building Reference
     */
    public Reference getReference() throws NamingException {
        return JNDIReferenceFactory.createReference(this.getClass().getName(), this);
    }

    /**
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        Map<String, String> props = (Map<String, String>) in.readObject();
        if (props != null) {
            setProperties(props);
        }

    }

    /**
     * @param out
     * @throws IOException
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(getProperties());

    }

    protected String getProperty(Map<String, String> map, String key, String defaultValue) {
        String value = map.get(key);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

}
