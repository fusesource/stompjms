/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.mqtt.client;

import org.fusesource.hawtbuf.UTF8Buffer;

/**
* <p>
* </p>
*
* @author <a href="http://hiramchirino.com">Hiram Chirino</a>
*/
public class Topic {

    private final UTF8Buffer name;
    private final QoS qos;

    public Topic(String name, QoS qos) {
        this(new UTF8Buffer(name), qos);
    }

    public Topic(UTF8Buffer name, QoS qos) {
        this.name = name;
        this.qos = qos;
    }

    public UTF8Buffer name() {
        return name;
    }

    public QoS qos() {
        return qos;
    }
}
