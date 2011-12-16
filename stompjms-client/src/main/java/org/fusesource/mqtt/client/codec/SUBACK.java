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

import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Arrays;
import static org.fusesource.mqtt.client.codec.CommandSupport.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class SUBACK implements Command {

    public static final byte[] NO_GRANTED_QOS = new byte[0];
    public static final byte TYPE = 9;

    private short messageId;
    private byte grantedQos[] = NO_GRANTED_QOS;

    public byte getType() {
        return TYPE;
    }

    public SUBACK decode(MQTTFrame frame) throws ProtocolException {
        assert(frame.buffers.length == 1);
        DataByteArrayInputStream is = new DataByteArrayInputStream(frame.buffers[0]);
        messageId = is.readShort();
        grantedQos = is.readBuffer(is.available()).toByteArray();
        return this;
    }
    
    public MQTTFrame encode() {
        try {
            DataByteArrayOutputStream os = new DataByteArrayOutputStream(2+grantedQos.length);
            os.writeShort(messageId);
            os.write(grantedQos);

            MQTTFrame frame = new MQTTFrame();
            frame.commandType(TYPE);
            return frame.buffer(os.toBuffer());
        } catch (IOException e) {
            throw new RuntimeException("The impossible happened");
        }
    }

    public byte[] grantedQos() {
        return grantedQos;
    }

    public SUBACK grantedQos(byte[] grantedQos) {
        this.grantedQos = grantedQos;
        return this;
    }

    public short messageId() {
        return messageId;
    }

    public SUBACK messageId(short messageId) {
        this.messageId = messageId;
        return this;
    }

    @Override
    public String toString() {
        return "SUBACK{" +
                "grantedQos=" + grantedQos +
                ", messageId=" + Arrays.asList(messageId) +
                '}';
    }
}
