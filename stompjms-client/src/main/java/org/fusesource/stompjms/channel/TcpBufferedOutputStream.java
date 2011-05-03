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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An optimized buffered outputstream for Tcp
 *
 * @version $Revision: 1.1.1.1 $
 */

public class TcpBufferedOutputStream extends FilterOutputStream {
    private static final int BUFFER_SIZE = 8192;
    private byte[] buffer;
    private int bufferlen;
    private int count;
    //private volatile long writeTimestamp = -1;//concurrent reads of this value


    /**
     * Constructor
     *
     * @param out
     */
    public TcpBufferedOutputStream(OutputStream out) {
        this(out, BUFFER_SIZE);
    }

    /**
     * Creates a new buffered output stream to write data to the specified
     * underlying output stream with the specified buffer size.
     *
     * @param out  the underlying output stream.
     * @param size the buffer size.
     * @throws IllegalArgumentException if size <= 0.
     */
    public TcpBufferedOutputStream(OutputStream out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buffer = new byte[size];
        bufferlen = size;
    }

    /**
     * write a byte on to the stream
     *
     * @param b - byte to write
     * @throws IOException
     */
    public void write(int b) throws IOException {
        if ((bufferlen - count) < 1) {
            flush();
        }
        buffer[count++] = (byte) b;
    }

    /**
     * write a byte array to the stream
     *
     * @param b   the byte buffer
     * @param off the offset into the buffer
     * @param len the length of data to write
     * @throws IOException
     */
    public void write(byte b[], int off, int len) throws IOException {
        if (b != null) {
            if ((bufferlen - count) < len) {
                flush();
            }
            if (buffer.length >= len) {
                System.arraycopy(b, off, buffer, count, len);
                count += len;
            } else {
                out.write(b, off, len);
            }
        }
    }

    /**
     * flush the data to the output stream This doesn't call flush on the
     * underlying outputstream, because Tcp is particularly efficent at doing
     * this itself ....
     *
     * @throws IOException
     */
    public void flush() throws IOException {
        if (count > 0 && out != null) {

            out.write(buffer, 0, count);

            count = 0;
        }
    }

    /**
     * close this stream
     *
     * @throws IOException
     */
    public void close() throws IOException {
        super.close();
    }

}
