/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.stompjms.util;

import org.fusesource.hawtbuf.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utilities for ByteBuffers
 */
public class IOUtils {
    /**
     * Create an InputStream to read a ByteBuffer
     *
     * @param buf
     * @return the InputStream
     */
    public static InputStream getByteBufferInputStream(final ByteBuffer buf) {
        return new InputStream() {
            public int read() throws IOException {
                if (!buf.hasRemaining()) {
                    return -1;
                }
                return buf.get();
            }

            public int read(byte[] bytes, int off, int len) throws IOException {
                int toWrite = Math.min(len, buf.remaining());
                buf.get(bytes, off, toWrite);
                return len;
            }
        };
    }

    /**
     * Create an OutputStream for a ByteBuffer
     *
     * @param buf
     * @return
     */
    public static OutputStream getByteBufferOutputStream(final ByteBuffer buf) {
        return new OutputStream() {
            public void write(int b) throws IOException {
                buf.put((byte) b);
            }

            public void write(byte[] bytes, int off, int len) throws IOException {
                buf.put(bytes, off, len);
            }
        };
    }

    /**
     * Create a Buffer from an Object
     *
     * @param object
     * @return
     * @throws Exception
     */
    public static Buffer getBuffer(Object object) throws IOException {
        if (object != null) {
            BufferOutputStream bufferOut = new BufferOutputStream(512);
            DataOutputStream dataOut = new DataOutputStream(bufferOut);
            ObjectOutputStream objOut = new ObjectOutputStream(dataOut);
            objOut.writeObject(object);
            objOut.flush();
            objOut.reset();
            objOut.close();
            return bufferOut.toBuffer();
        }
        return null;
    }

    /**
     * @param out
     * @param object
     * @throws IOException
     */
    public static void writeObject(DataByteArrayOutputStream out, Object object) throws IOException {
        if (object != null) {
            ByteArrayOutputStream bufferOut = new ByteArrayOutputStream(512);
            ObjectOutputStream objOut = new ObjectOutputStream(bufferOut);
            objOut.writeObject(object);
            objOut.flush();
            objOut.close();
            byte[] data = bufferOut.toByteArray();
            out.writeInt(data.length);
            out.write(data);
        } else {
            out.writeInt(0);
        }
    }

    /**
     * @param in
     * @return an Object
     * @throws IOException
     */
    public static Object readObject(DataByteArrayInputStream in) throws IOException {
        Object result = null;
        int len = in.readInt();
        if (len > 0) {
            byte[] rawData = new byte[len];
            in.readFully(rawData);
            InputStream is = new ByteArrayInputStream(rawData);
            DataInputStream dataIn = new DataInputStream(is);
            ClassLoadingAwareObjectInputStream objIn = new ClassLoadingAwareObjectInputStream(dataIn);
            try {
                result = objIn.readObject();
            } catch (ClassNotFoundException e) {
                IOException ex = new IOException("Class not Found " + e.getMessage());
                ex.initCause(e);
                throw ex;
            }
        }
        return result;
    }

    /**
     * Create an Object from a Buffer
     *
     * @param buffer
     * @return the Object
     * @throws IOException
     */
    public static Object getObject(Buffer buffer) throws IOException {
        if (buffer != null) {
            InputStream is = new ByteArrayInputStream(buffer.data, buffer.offset, buffer.length);
            DataInputStream dataIn = new DataInputStream(is);
            ClassLoadingAwareObjectInputStream objIn = new ClassLoadingAwareObjectInputStream(dataIn);
            try {
                return objIn.readObject();
            } catch (ClassNotFoundException e) {
                IOException ex = new IOException("Class not Found " + e.getMessage());
                ex.initCause(e);
                throw ex;
            }
        }
        return null;
    }


    /**
     * Compress the buffer
     *
     * @param buffer
     * @return compressed Buffer
     * @throws IOException
     */
    public static Buffer compress(Buffer buffer) throws IOException {
        Buffer result = buffer;
        if (buffer != null) {
            BufferOutputStream bytesOut = new BufferOutputStream(buffer.length);
            GZIPOutputStream gzipOut = new GZIPOutputStream(bytesOut, buffer.length);
            gzipOut.write(buffer.toByteArray());
            gzipOut.close();
            bytesOut.close();
            result = bytesOut.toBuffer();
        }
        return result;
    }

    /**
     * Inflate a compressed buffer
     *
     * @param buffer
     * @return inflated buffer
     * @throws IOException
     */
    public static Buffer inflate(Buffer buffer) throws IOException {
        Buffer result = buffer;
        if (isCompressed(buffer)) {
            InputStream bytesIn = new BufferInputStream(buffer);
            GZIPInputStream gzipIn = new GZIPInputStream(bytesIn);
            BufferOutputStream bytesOut = new BufferOutputStream(buffer.length);
            byte[] data = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = gzipIn.read(data, 0, data.length)) > 0) {
                bytesOut.write(data, 0, bytesRead);
            }
            gzipIn.close();
            bytesIn.close();
            result = bytesOut.toBuffer();
            bytesOut.close();
        }
        return result;
    }

    static boolean isCompressed(Buffer data) {
        boolean result = false;
        if (data != null && data.length > 2) {
            int ch1 = (int) (data.get(0) & 0xff);
            int ch2 = (int) (data.get(1) & 0xff);
            int magic = (ch1 | (ch2 << 8));
            result = (magic == GZIPInputStream.GZIP_MAGIC);
        }
        return result;
    }
}
