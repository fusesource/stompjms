/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms.channel;


import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.fusesource.stompjms.channel.Stomp.*;
import static org.fusesource.hawtbuf.Buffer.*;

/**
 * Represents all the data in a STOMP frame.
 *
 * @author <a href="http://hiramchirino.com">chirino</a>
 */
public class StompFrame {

    public static final Buffer NO_DATA = new Buffer(new byte[]{});

    public AsciiBuffer action;
    public Map<AsciiBuffer, AsciiBuffer> headers = new HashMap<AsciiBuffer, AsciiBuffer>();
    public Buffer content = NO_DATA;

    public StompFrame(AsciiBuffer command) {
        this(command, null, (Buffer) null);
    }

    public StompFrame(AsciiBuffer command, Map<AsciiBuffer, AsciiBuffer> headers) {
        this(command, headers, (Buffer) null);
    }

    public StompFrame(AsciiBuffer command, Map<AsciiBuffer, AsciiBuffer> headers, Buffer data) {
        this.action = command;
        if (headers != null)
            this.headers = headers;
        if (data != null)
            this.content = data;
    }

    public StompFrame(AsciiBuffer command, Map<AsciiBuffer, AsciiBuffer> headers, byte[] data) {
        this(command, headers, (data != null ? new Buffer(data) : null));
    }

    public StompFrame() {
    }

    public StompFrame clone() {
        return new StompFrame(action, new HashMap(headers), content);
    }

    public AsciiBuffer getAction() {
        return action;
    }

    public void setAction(AsciiBuffer command) {
        this.action = command;
    }

    public Buffer getContent() {
        return this.content;
    }

    public String getBody() {
        try {
            Buffer b = getContent();
            if (b != null) {
                return new String(b.getData(), b.getOffset(), b.getLength(), "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
        }
        return new String("");
    }

    public void setContent(Buffer data) {
        this.content = data;
    }

    public void clearContent() {
        this.content = NO_DATA;
    }

    public Map<AsciiBuffer, AsciiBuffer> getHeaders() {
        return headers;
    }

    public Buffer toBuffer() {
        try {
            DataByteArrayOutputStream out = new DataByteArrayOutputStream();
            out.write(getAction());
            out.write(Stomp.NEWLINE);

            for (Map.Entry<AsciiBuffer, AsciiBuffer> entry: headers.entrySet()){
                out.write(entry.getKey());
                out.write(SEPERATOR);
                out.write(entry.getValue());
                out.write(Stomp.NEWLINE);
            }

            //denotes end of headers with a new line
            out.write(CONTENT_LENGTH);
            out.write(SEPERATOR);
            int contentLength = content != null ? content.length() : 0;
            out.write(ascii(Integer.toString(contentLength)));
            out.write(Stomp.NEWLINE);
            out.write(Stomp.NEWLINE);
            if (content != null) {
                out.write(content);
            }
            out.write(Stomp.NULL);
            return out.toBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e); // not expected to occur.
        }
    }

    public String toString() {
        return toBuffer().utf8().toString();
    }
}
