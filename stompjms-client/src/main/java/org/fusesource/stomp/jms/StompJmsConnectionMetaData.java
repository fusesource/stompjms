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

import javax.jms.ConnectionMetaData;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A <CODE>ConnectionMetaData</CODE> object provides information describing
 * the <CODE>Connection</CODE> object.
 */

public final class StompJmsConnectionMetaData implements ConnectionMetaData {

    public static final String PROVIDER_VERSION;
    public static final int PROVIDER_MAJOR_VERSION;
    public static final int PROVIDER_MINOR_VERSION;

    public static final StompJmsConnectionMetaData INSTANCE = new StompJmsConnectionMetaData();

    static {
        String version = null;
        int major = 0;
        int minor = 0;
        try {
            Package p = Package.getPackage("org.apache.activemq.apollo.stomp.jms");
            if (p != null) {
                version = p.getImplementationVersion();
                Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+).*");
                Matcher m = pattern.matcher(version);
                if (m.matches()) {
                    major = Integer.parseInt(m.group(1));
                    minor = Integer.parseInt(m.group(2));
                }
            }
        } catch (Throwable e) {
        }
        PROVIDER_VERSION = version;
        PROVIDER_MAJOR_VERSION = major;
        PROVIDER_MINOR_VERSION = minor;
    }

    private StompJmsConnectionMetaData() {
    }

    /**
     * Gets the JMS API version.
     *
     * @return the JMS API version
     */

    public String getJMSVersion() {
        return "1.1";
    }

    /**
     * Gets the JMS major version number.
     *
     * @return the JMS API major version number
     */

    public int getJMSMajorVersion() {
        return 1;
    }

    /**
     * Gets the JMS minor version number.
     *
     * @return the JMS API minor version number
     */

    public int getJMSMinorVersion() {
        return 1;
    }

    /**
     * Gets the JMS provider name.
     *
     * @return the JMS provider name
     */

    public String getJMSProviderName() {
        return "ActiveBlaze";
    }

    /**
     * Gets the JMS provider version.
     *
     * @return the JMS provider version
     */

    public String getProviderVersion() {
        return PROVIDER_VERSION;
    }

    /**
     * Gets the JMS provider major version number.
     *
     * @return the JMS provider major version number
     */

    public int getProviderMajorVersion() {
        return PROVIDER_MAJOR_VERSION;
    }

    /**
     * Gets the JMS provider minor version number.
     *
     * @return the JMS provider minor version number
     */

    public int getProviderMinorVersion() {
        return PROVIDER_MINOR_VERSION;
    }

    /**
     * Gets an enumeration of the JMSX property names.
     *
     * @return an Enumeration of JMSX property names
     */

    public Enumeration<String> getJMSXPropertyNames() {
        Vector<String> jmxProperties = new Vector<String>();
        return jmxProperties.elements();
    }


}
