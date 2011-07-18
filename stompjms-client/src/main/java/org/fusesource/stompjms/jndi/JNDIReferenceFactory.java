/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms.jndi;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


/**
 * Converts objects implementing JNDIStorable into a property fields so they can be stored and regenerated from JNDI
 */
public class JNDIReferenceFactory implements ObjectFactory {

    /**
     * This will be called by a JNDIprovider when a Reference is retrieved from a JNDI store - and generates the orignal
     * instance
     *
     * @param object      the Reference object
     * @param name        the JNDI name
     * @param nameCtx     the context
     * @param environment the environment settings used by JNDI
     * @return the instance built from the Reference object
     * @throws Exception if building the instance from Reference fails (usually class not found)
     */
    public Object getObjectInstance(Object object, Name name, Context nameCtx, Hashtable<?, ?> environment)
            throws Exception {
        Object result = null;
        if (object instanceof Reference) {
            Reference reference = (Reference) object;
            Class<?> theClass = loadClass(this, reference.getClassName());
            if (JNDIStorable.class.isAssignableFrom(theClass)) {
                JNDIStorable store = (JNDIStorable) theClass.newInstance();
                Map<String, String> properties = new HashMap<String, String>();
                for (Enumeration<RefAddr> iter = reference.getAll(); iter.hasMoreElements();) {
                    StringRefAddr addr = (StringRefAddr) iter.nextElement();
                    properties.put(addr.getType(), (addr.getContent() == null) ? "" : addr.getContent().toString());
                }
                store.setProperties(properties);
                result = store;
            }
        } else {
            throw new RuntimeException("Object " + object + " is not a reference");
        }
        return result;
    }

    /**
     * Create a Reference instance from a JNDIStorable object
     *
     * @param instanceClassName
     * @param po
     * @return Reference
     * @throws NamingException
     */
    public static Reference createReference(String instanceClassName, JNDIStorable po) throws NamingException {
        Reference result = new Reference(instanceClassName, JNDIReferenceFactory.class.getName(), null);
        try {
            Map<String, String> props = po.getProperties();
            for (Map.Entry<String, String> entry : props.entrySet()) {
                javax.naming.StringRefAddr addr = new javax.naming.StringRefAddr(entry.getKey(), entry.getValue());
                result.add(addr);
            }
        } catch (Exception e) {
            throw new NamingException(e.getMessage());
        }
        return result;
    }

    /**
     * Retrieve the class loader for a named class
     *
     * @param thisObj
     * @param className
     * @return the class
     * @throws ClassNotFoundException
     */
    public static Class<?> loadClass(Object thisObj, String className) throws ClassNotFoundException {
        // try local ClassLoader first.
        ClassLoader loader = thisObj.getClass().getClassLoader();
        Class<?> theClass;
        if (loader != null) {
            theClass = loader.loadClass(className);
        } else {
            // Will be null in jdk1.1.8
            // use default classLoader
            theClass = Class.forName(className);
        }
        return theClass;
    }
}
