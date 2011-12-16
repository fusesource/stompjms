/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.mqtt.client.codec;

import java.net.ProtocolException;

import static org.fusesource.mqtt.client.codec.CommandSupport.AckBase;
import static org.fusesource.mqtt.client.codec.CommandSupport.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class UNSUBACK extends AckBase implements Command {

    public static final byte TYPE = 11;

    public byte getType() {
        return TYPE;
    }

    @Override
    public UNSUBACK decode(MQTTFrame frame) throws ProtocolException {
        return (UNSUBACK) super.decode(frame);
    }

    @Override
    public UNSUBACK messageId(short messageId) {
        return (UNSUBACK) super.messageId(messageId);
    }
}
