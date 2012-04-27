/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stomp.codec;

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.hawtdispatch.transport.*;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;

import static org.fusesource.stomp.client.Constants.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class StompProtocolCodec extends AbstractProtocolCodec {

    private static final int max_command_length = 20;
    int max_header_length = 1024 * 10;
    int max_headers = 1000;
    int max_data_length = 1024 * 1024 * 100;

    @Override
    protected void encode(Object value) throws IOException {
        StompFrame frame = (StompFrame) value;
        frame.write(nextWriteBuffer);
    }

    @Override
    protected Action initialDecodeAction() {
        return read_action;
    }

    final Action read_action = new Action() {
        public Object apply() throws IOException {
            Buffer line = readUntil((byte) '\n', max_command_length, "The maximum command length was exceeded");
            if (line != null) {
                Buffer action = line.moveTail(-1);
                if (trim) {
                    action = action.trim();
                }
                if (action.length() > 0) {
                    StompFrame frame = new StompFrame(action.ascii());
                    nextDecodeAction = read_headers(frame);
                }
            }
            return null;
        }
    };

    private Action read_headers(final StompFrame frame) {
        final AsciiBuffer[] contentLengthValue = new AsciiBuffer[1];
        final ArrayList<StompFrame.HeaderEntry> headers = new ArrayList<StompFrame.HeaderEntry>(10);
        return new Action() {
            public Object apply() throws IOException {
                Buffer line = readUntil((byte) '\n', max_header_length, "The maximum header length was exceeded");
                if (line != null) {
                    line = line.moveTail(-1);
                    if (line.length > 0) {

                        if (max_headers != -1 && headers.size() > max_headers) {
                            throw new IOException("The maximum number of headers was exceeded");
                        }

                        try {
                            int seperatorIndex = line.indexOf(COLON_BYTE);
                            if (seperatorIndex < 0) {
                                throw new IOException("Header line missing seperator [" + line.ascii() + "]");
                            }
                            Buffer name = line.slice(0, seperatorIndex);
                            if (trim) {
                                name = name.trim();
                            }

                            Buffer value = line.slice(seperatorIndex + 1, line.length());
                            if (trim) {
                                value = value.trim();
                            }
                            StompFrame.HeaderEntry entry = new StompFrame.HeaderEntry(name.ascii(), value.ascii());
                            if (entry.key.equals(CONTENT_LENGTH)) {
                                contentLengthValue[0] = entry.value;
                            }
                            headers.add(entry);
                        } catch (Exception e) {
                            throw new IOException("Unable to parser header line [" + line + "]");
                        }

                    } else {
                        frame.setHeaders(headers);
                        AsciiBuffer contentLength = contentLengthValue[0];
                        if (contentLength != null) {
                            // Bless the client, he's telling us how much data to read in.
                            int length = 0;
                            try {
                                length = Integer.parseInt(contentLength.toString());
                            } catch (NumberFormatException e) {
                                throw new IOException("Specified content-length is not a valid integer");
                            }

                            if (max_data_length != -1 && length > max_data_length) {
                                throw new IOException("The maximum data length was exceeded");
                            }

                            nextDecodeAction = read_binary_body(frame, length);
                        } else {
                            nextDecodeAction = read_text_body(frame);
                        }
                    }
                }
                return null;
            }
        };
    }

    private Action read_binary_body(final StompFrame frame, final int contentLength) {
        return new Action() {
            public Object apply() throws IOException {
                Buffer content = readBytes(contentLength + 1);
                if (content != null) {
                    if (content.get(contentLength) != 0) {
                        throw new IOException("Expected null termintor after " + contentLength + " content bytes");
                    }
                    frame.content(content.moveTail(-1));
                    nextDecodeAction = read_action;
                    return frame;
                } else {
                    return null;
                }
            }
        };
    }

    private Action read_text_body(final StompFrame frame) {
        return new Action() {
            public Object apply() throws IOException {
                Buffer content = readUntil((byte) 0);
                if (content != null) {
                    nextDecodeAction = read_action;
                    frame.content(content.moveTail(-1));
                    return frame;
                } else {
                    return null;
                }
            }
        };
    }
}
