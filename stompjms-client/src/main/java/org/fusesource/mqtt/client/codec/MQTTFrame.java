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

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.mqtt.client.QoS;

/**
* <p>
* </p>
*
* @author <a href="http://hiramchirino.com">Hiram Chirino</a>
*/
public class MQTTFrame extends CommandSupport.HeaderBase {

    public Buffer[] buffers;

    public MQTTFrame() {
    }
    public MQTTFrame( Buffer buffer) {
        this(new Buffer[]{buffer});
    }
    public MQTTFrame( Buffer[] buffers) {
        this.buffers = buffers;
    }

    public Buffer[] buffers() {
        return buffers;
    }
    public MQTTFrame buffers(Buffer...buffers) {
        this.buffers = buffers;
        return this;
    }

    public MQTTFrame buffer(Buffer buffer) {
        this.buffers = new Buffer[]{buffer};
        return this;
    }

    @Override
    public byte header() {
        return super.header();
    }

    @Override
    public MQTTFrame header(byte header) {
        return (MQTTFrame)super.header(header);
    }

    @Override
    public byte commandType() {
        return super.commandType();
    }

    @Override
    public MQTTFrame commandType(int type) {
        return (MQTTFrame)super.commandType(type);
    }

    @Override
    public boolean dup() {
        return super.dup();
    }

    @Override
    public MQTTFrame dup(boolean dup) {
        return (MQTTFrame) super.dup(dup);
    }

    @Override
    public QoS qos() {
        return super.qos();
    }

    @Override
    public MQTTFrame qos(QoS qos) {
        return (MQTTFrame) super.qos(qos);
    }

    @Override
    public boolean retain() {
        return super.retain();
    }

    @Override
    public MQTTFrame retain(boolean retain) {
        return (MQTTFrame) super.retain(retain);
    }
}
