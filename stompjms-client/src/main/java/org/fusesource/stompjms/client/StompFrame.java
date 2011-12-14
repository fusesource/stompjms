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


import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.ByteArrayOutputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.fusesource.stompjms.client.Constants.*;

/**
 * Represents all the data in a STOMP frame.
 *
 * @author <a href="http://hiramchirino.com">chirino</a>
 */
public class StompFrame {

    public static final Buffer NO_DATA = new Buffer(new byte[]{});

    static public class HeaderEntry {
        public final AsciiBuffer key;
        public final AsciiBuffer value;

        public HeaderEntry(AsciiBuffer key, AsciiBuffer value) {
            this.key = key;
            this.value = value;
        }

        public AsciiBuffer getKey() {
            return key;
        }

        public AsciiBuffer getValue() {
            return value;
        }
    }

    private AsciiBuffer action;
    private ArrayList<HeaderEntry> headerList;
    private HashMap<AsciiBuffer, AsciiBuffer> headerMap = new HashMap<AsciiBuffer, AsciiBuffer>(16);
    private Buffer content = NO_DATA;

    public StompFrame() {
    }

    public StompFrame(AsciiBuffer action) {
        this.action = action;
    }

    public StompFrame clone() {
        StompFrame rc = new StompFrame(action);
        if( headerList!=null ) {
            rc.headerList = new ArrayList<HeaderEntry>(headerList);
            rc.headerMap = null;
        } else {
            rc.headerMap = new HashMap<AsciiBuffer,AsciiBuffer>(headerMap);
            rc.headerList = null;
        }
        rc.content = content;
        return rc;
    }

    public AsciiBuffer action() {
        return action;
    }

    public StompFrame action(AsciiBuffer action) {
        assert action != null;
        this.action = action;
        return this;
    }

    public Buffer content() {
        return this.content;
    }

    public StompFrame content(Buffer content) {
        assert content != null;
        this.content = content;
        return this;
    }

    public String contentAsString() {
        try {
            return new String(content.getData(), content.getOffset(), content.getLength(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // Not expected
        }
    }

    public Map<AsciiBuffer, AsciiBuffer> headerMap() {
        if( headerMap==null ) {
            headerMap = new HashMap<AsciiBuffer, AsciiBuffer>();
            for (HeaderEntry HeaderEntry : headerList) {
                AsciiBuffer old = headerMap.put(HeaderEntry.getKey(), HeaderEntry.getValue());
                if( old !=null ) {
                    headerMap.put(HeaderEntry.getKey(), old);
                }
            }
            headerList = null;
        }
        return headerMap;
    }

    public List<HeaderEntry> headerList() {
        if( headerList==null ) {
            for (Map.Entry<AsciiBuffer,AsciiBuffer> entry : headerMap.entrySet()) {
                headerList.add(new HeaderEntry(entry.getKey(), entry.getValue()));
            }
            headerMap = null;
        }
        return headerList;
    }

    public void addHeader(AsciiBuffer key, AsciiBuffer value) {
        if( headerList!=null ) {
            headerList.add(0, new HeaderEntry(key, value));
        } else {
            headerMap.put(key, value);
        }
    }

    public AsciiBuffer getHeader(AsciiBuffer key) {
        if( headerList!=null ) {
            for (HeaderEntry HeaderEntry : headerList) {
                if( HeaderEntry.getKey().equals(key) ) {
                    return HeaderEntry.getValue();
                }
            }
            return null;
        } else {
            return headerMap.get(key);
        }
    }

    public void clearHeaders() {
        if( headerList!=null) {
            headerList.clear();
        } else {
            headerMap.clear();
        }
    }

    public void setHeaders(ArrayList<HeaderEntry> values) {
        headerList = values;
        headerMap = null;
    }

    public Buffer toBuffer() {
        return toBuffer(true);
    }

    public Buffer toBuffer(boolean includeBody) {
        try {
            DataByteArrayOutputStream out = new DataByteArrayOutputStream();
            write(out, includeBody);
            return out.toBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e); // not expected to occur.
        }
    }

    private void write(DataOutput out, Buffer buffer) throws IOException {
        out.write(buffer.data, buffer.offset, buffer.length);
    }

    public void write(DataOutput out) throws IOException {
        write(out, true);
    }

    public void addContentLengthHeader() {
        addHeader(CONTENT_LENGTH, new AsciiBuffer(Integer.toString(content.length())));
    }

    public int size() {
        int rc = action.length() + 1;
        if( headerList!=null ) {
            for (HeaderEntry entry : headerList) {
                rc += entry.getKey().length() + entry.getValue().length() + 2;
            }
        } else {
            for (Map.Entry<AsciiBuffer,AsciiBuffer> entry : headerMap.entrySet()) {
                rc += entry.getKey().length() + entry.getValue().length() + 2;
            }
        }
        rc += content.length() + 3;
        return rc;
    }

    public void write(DataOutput out, boolean includeBody) throws IOException {
        write(out, action);
        out.writeByte(NEWLINE_BYTE);

        if( headerList!=null ) {
            for (HeaderEntry entry : headerList) {
                write(out, entry.getKey());
                out.writeByte(COLON_BYTE);
                write(out, entry.getValue());
                out.writeByte(NEWLINE_BYTE);
            }
        } else {
            for (Map.Entry<AsciiBuffer,AsciiBuffer> entry : headerMap.entrySet()) {
                write(out, entry.getKey());
                out.writeByte(COLON_BYTE);
                write(out, entry.getValue());
                out.writeByte(NEWLINE_BYTE);
            }
        }

        //denotes end of headers with a new line
        out.writeByte(NEWLINE_BYTE);
        if (includeBody) {
            write(out, content);
            out.writeByte(NULL_BYTE);
            out.writeByte(NEWLINE_BYTE);
        }
    }

    public String toString() {
        return toBuffer(false).ascii().toString();
    }

    public String errorMessage() {
        AsciiBuffer value = getHeader(MESSAGE_HEADER);
        if (value != null) {
            return decodeHeader(value);
        } else {
            return contentAsString();
        }
    }

    public static String decodeHeader(Buffer value) {
        if (value == null)
            return null;

        ByteArrayOutputStream rc = new ByteArrayOutputStream(value.length);
        Buffer pos = new Buffer(value);
        int max = value.offset + value.length;
        while (pos.offset < max) {
            if (pos.startsWith(ESCAPE_ESCAPE_SEQ)) {
                rc.write(ESCAPE_BYTE);
                pos.moveHead(2);
            } else if (pos.startsWith(COLON_ESCAPE_SEQ)) {
                rc.write(COLON_BYTE);
                pos.moveHead(2);
            } else if (pos.startsWith(NEWLINE_ESCAPE_SEQ)) {
                rc.write(NEWLINE_BYTE);
                pos.moveHead(2);
            } else {
                rc.write(pos.data[pos.offset]);
                pos.moveHead(1);
            }
        }
        try {
            return new String(rc.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // not expected.
        }
    }

    public static AsciiBuffer encodeHeader(String value) {
        if (value == null)
            return null;
        try {
            byte[] data = value.getBytes("UTF-8");
            ByteArrayOutputStream rc = new ByteArrayOutputStream(data.length);
            for (byte d : data) {
                switch (d) {
                    case ESCAPE_BYTE:
                        rc.write(ESCAPE_ESCAPE_SEQ);
                        break;
                    case COLON_BYTE:
                        rc.write(COLON_ESCAPE_SEQ);
                        break;
                    case NEWLINE_BYTE:
                        rc.write(COLON_ESCAPE_SEQ);
                        break;
                    default:
                        rc.write(d);
                }
            }
            return rc.toBuffer().ascii();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // not expected.
        }
    }

    public static Map<AsciiBuffer, AsciiBuffer> encodeHeaders(Map<String, String> headers) {
        if(headers==null)
            return null;
        HashMap<AsciiBuffer, AsciiBuffer> rc = new HashMap<AsciiBuffer, AsciiBuffer>(headers.size());
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            rc.put(StompFrame.encodeHeader(entry.getKey()), StompFrame.encodeHeader(entry.getValue()));
        }
        return rc;
    }
}
