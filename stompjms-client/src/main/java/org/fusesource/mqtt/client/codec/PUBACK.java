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
import static org.fusesource.mqtt.client.codec.CommandSupport.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class PUBACK extends CommandSupport.AckBase implements Command {

    public static final byte TYPE = 4;

    public byte getType() {
        return TYPE;
    }

    @Override
    public PUBACK decode(MQTTFrame frame) throws ProtocolException {
        return (PUBACK) super.decode(frame);
    }

    @Override
    public PUBACK messageId(short messageId) {
        return (PUBACK) super.messageId(messageId);
    }
}
