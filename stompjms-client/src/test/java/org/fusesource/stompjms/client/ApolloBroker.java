/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms.client;

import org.apache.activemq.apollo.broker.Broker;
import org.apache.activemq.apollo.broker.BrokerFactory;
import org.apache.activemq.apollo.util.ServiceControl;

import java.io.File;
import java.net.URL;

/**
 * Used to start an apollo broker.
 *
 * @version $Revision$
 */
public class ApolloBroker {

    public int port = -1;
    public Broker broker;

    protected Broker createBroker() throws Exception {
        URL resource = getClass().getResource("apollo-stomp.xml");
        return BrokerFactory.createBroker(resource.toURI().toString());
    }

    public void start() throws Exception {
        if (System.getProperty("basedir") == null) {
            File file = new File(".");
            System.setProperty("basedir", file.getAbsolutePath());
        }
        broker = createBroker();
        ServiceControl.start(broker, "Starting Apollo Broker");
        this.port = broker.get_socket_address().getPort();
    }

    public void stop() throws Exception {
        ServiceControl.stop(broker, "Stopped Apollo Broker");
    }

}
