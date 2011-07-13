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
import org.fusesource.stompjms.StompJmsExceptionSupport;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.net.SocketFactory;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.fusesource.stompjms.channel.Stomp.*;
import static org.fusesource.hawtbuf.Buffer.*;

/**
 * @version $Revision$
 */
public class StompSocket implements Runnable {
    private static final int MAX_COMMAND_LENGTH = 1024;
    private static final int MAX_DATA_LENGTH = 1024 * 1024 * 32;
    private static final int MAX_HEADER_LENGTH = 1024 * 10;
    private static final int MAX_HEADERS = 1024;

    private SocketFactory socketFactory;
    private final URI remoteLocation;
    private final URI localLocation;
    private int connectionTimeout = 30000;
    private int soTimeout;
    private int socketBufferSize = 64 * 1024;
    private int ioBufferSize = 8 * 1024;
    private Socket socket;
    private Thread thread;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private ExceptionListener exceptionListener;
    private Boolean keepAlive;
    private Boolean tcpNoDelay;
    private AtomicBoolean started = new AtomicBoolean(false);
    private AtomicBoolean stopping = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private AtomicBoolean connected = new AtomicBoolean(false);
    private StompFrameListener stompListener;

    /**
     * Connect to a Broker
     *
     * @param factory
     * @param localLocation
     * @param remoteLocation
     * @throws UnknownHostException
     * @throws IOException
     */
    public StompSocket(SocketFactory factory, URI localLocation, URI remoteLocation) throws UnknownHostException,
            IOException {
        this.socketFactory = factory;
        try {
            this.socket = this.socketFactory.createSocket();
        } catch (SocketException e) {
            this.socket = null;
        }
        this.remoteLocation = remoteLocation;
        this.localLocation = localLocation;

    }

    public void setExceptionListener(ExceptionListener l) {
        this.exceptionListener = l;
    }

    public void setStompFrameListener(StompFrameListener l) {
        this.stompListener = l;
    }

    /**
     * @return true if this Socket is started
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * @return true if this Socket is started
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * @return true if this socket is in the process of closing
     */
    public boolean isStopping() {
        return stopping.get();
    }

    /**
     * @return true if this socket is stopped
     */
    public boolean isStopped() {
        return stopped.get();
    }


    /**
     * A one way asynchronous send
     *
     * @param frame
     * @throws IOException
     */
    public synchronized void sendFrame(StompFrame frame) throws IOException {
//        System.out.println("===>");
//        System.out.println(frame);
//        System.out.println("===>");
        frame.write(dataOut);
        dataOut.flush();
    }

    /**
     * @return pretty print of 'this'
     */
    @Override
    public String toString() {
        return ""
                + (socket.isConnected() ? "tcp://" + socket.getInetAddress() + ":" + socket.getPort()
                : (localLocation != null ? localLocation : remoteLocation));
    }

    /**
     * reads packets from a Socket
     */
    public void run() {
        try {
            while (!isStopped()) {
                doRun();
            }
        } catch (IOException e) {
            onException(e);
        } catch (Throwable e) {
            e.printStackTrace();
            IOException ioe = new IOException("Unexpected error occurred: " + e.getMessage(), e);
            onException(ioe);
        }
    }

    private void doRun() throws IOException {
        try {
            StompFrame frame = readFrame(this.dataIn);
            if (frame != null) {
                StompFrameListener l = this.stompListener;
                if (l != null) {
                    l.onFrame(frame);
                }
            }

        } catch (SocketTimeoutException e) {
        } catch (InterruptedIOException e) {
        }
    }

    public int getSocketBufferSize() {
        return socketBufferSize;
    }

    /**
     * Sets the buffer size to use on the socket
     *
     * @param socketBufferSize
     */
    public void setSocketBufferSize(int socketBufferSize) {
        this.socketBufferSize = socketBufferSize;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    /**
     * Sets the socket timeout
     *
     * @param soTimeout
     */
    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the timeout used to connect to the socket
     *
     * @param connectionTimeout
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    /**
     * Enable/disable TCP KEEP_ALIVE mode
     *
     * @param keepAlive
     */
    public void setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public Boolean getTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * Enable/disable the TCP_NODELAY option on the socket
     *
     * @param tcpNoDelay
     */
    public void setTcpNoDelay(Boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    /**
     * @return the ioBufferSize
     */
    public int getIoBufferSize() {
        return this.ioBufferSize;
    }

    /**
     * @param ioBufferSize the ioBufferSize to set
     */
    public void setIoBufferSize(int ioBufferSize) {
        this.ioBufferSize = ioBufferSize;
    }

    private String resolveHostName(String host) throws UnknownHostException {

        String localName = "localhost";
        try {
            localName = (InetAddress.getLocalHost()).getHostName();
        } catch (UnknownHostException uhe) {
            localName = uhe.getMessage(); // host = "hostname: hostname"
            if (localName != null) {
                int colon = localName.indexOf(':');
                if (colon > 0) {
                    localName = localName.substring(0, colon);
                }
            } else {
                throw uhe;
            }
        }
        if (localName != null && localName.equals(host)) {
            return "localhost";
        }

        return host;
    }

    /**
     * Configures the socket for use
     *
     * @param userName
     * @param password
     * @param clientId
     * @throws IOException
     */

    public StompFrame connect(String userName, String password, String clientId) throws IOException {
        if (connected.get()) {
            String host = resolveHostName(remoteLocation.getHost());
            // Now send the connect Frame
            HashMap<AsciiBuffer, AsciiBuffer> headers = new HashMap<AsciiBuffer, AsciiBuffer>();
            headers.put(ACCEPT_VERSION, V1_1);
            headers.put(HOST, ascii(host));
            if (userName != null && userName.isEmpty() == false) {
                headers.put(LOGIN, ascii(userName));
                headers.put(PASSCODE, ascii(password));
            }
            if (clientId != null && clientId.isEmpty() == false) {
                headers.put(CLIENT_ID, ascii(clientId));
            }
            StompFrame frame = new StompFrame(CONNECT, headers);
            sendFrame(frame);

            StompFrame response = readFrame(this.dataIn);
            if (response.getAction().equals(ERROR)) {
                AsciiBuffer value = response.headers.get(MESSAGE_HEADER);
                if( value!=null ) {
                    throw new IOException("Could not connect: " + value.toString());
                } else {
                    throw new IOException("Could not connect: " + response.getBody());
                }
            } else if (!response.getAction().equals(CONNECTED)) {
                throw new IOException("Could not connect. Received unexpected frame: " + response.toString());
            }
            return response;
        } else {
            throw new IOException("Not initialized");

        }
    }

    public void initialize() throws IOException {

        if (socket == null) {
            throw new IllegalStateException("Cannot connect if the socket or have not been created");
        }
        synchronized (connected) {
            if (connected.get() == false) {
                String host = "localhost";
                InetSocketAddress localAddress = null;
                InetSocketAddress remoteAddress = null;

                if (localLocation != null) {
                    localAddress = new InetSocketAddress(InetAddress.getByName(localLocation.getHost()), localLocation
                            .getPort());
                }

                if (remoteLocation != null) {
                    host = resolveHostName(remoteLocation.getHost());
                    remoteAddress = new InetSocketAddress(host, remoteLocation.getPort());
                }

                if (socket != null) {

                    if (localAddress != null) {
                        socket.bind(localAddress);
                    }

                    // If it's a server accepted socket.. we don't need to
                    // connect it
                    // to a remote address.
                    if (remoteAddress != null) {
                        if (connectionTimeout >= 0) {
                            socket.connect(remoteAddress, connectionTimeout);
                        } else {
                            socket.connect(remoteAddress);
                        }
                    }

                } else {
                    if (localAddress != null) {
                        socket = socketFactory.createSocket(remoteAddress.getAddress(), remoteAddress.getPort(),
                                localAddress.getAddress(), localAddress.getPort());
                    } else {
                        socket = socketFactory.createSocket(remoteAddress.getAddress(), remoteAddress.getPort());
                    }
                }
                socket.setSendBufferSize(getSocketBufferSize());
                socket.setReceiveBufferSize(getSocketBufferSize());

                initializeStreams();

            }
            this.connected.set(true);
        }
    }

    public void close() throws IOException {
        if (started.get()) {
            StompFrame frame = new StompFrame();
            frame.setAction(DISCONNECT);
            sendFrame(frame);
            connected.set(false);
            stop();
        }
    }

    /**
     * Start receiving messages
     *
     * @throws IOException
     */
    public void start() throws IOException {
        if (connected.get() == false) {
            throw new IOException("StompSocket is not connected");
        }
        if (started.compareAndSet(false, true)) {
            boolean success = false;
            try {
                thread = new Thread(null, this, "SocketChannel:" + toString());
                thread.start();
                success = true;
            } finally {
                started.set(success);
            }
        }
    }

    /**
     * stop and close the socket
     *
     * @throws IOException
     */

    public void stop() throws IOException {
        if (stopped.compareAndSet(false, true)) {
            connected.set(false);
            if (socket != null) {
                socket.close();
                if (this.thread != null && this.thread != Thread.currentThread()) {
                    try {
                        this.thread.join(2000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    private void initializeStreams() throws IOException {
        TcpBufferedInputStream buffIn = new TcpBufferedInputStream(socket.getInputStream(), ioBufferSize);
        this.dataIn = new DataInputStream(buffIn);
        TcpBufferedOutputStream outputStream = new TcpBufferedOutputStream(socket.getOutputStream(), ioBufferSize);
        this.dataOut = new DataOutputStream(outputStream);
    }

    public String getRemoteAddress() {
        if (socket != null) {
            return "" + socket.getRemoteSocketAddress();
        }
        return null;
    }

    private void onException(Throwable e) {
        ExceptionListener l = this.exceptionListener;
        if (l != null) {
            JMSException jmsEx = StompJmsExceptionSupport.create(e.getMessage(), e);
            l.onException(jmsEx);
        }
    }

    private StompFrame readFrame(DataInput in) throws IOException {

        try {

            // parse action
            AsciiBuffer action = parseAction(in);

            // Parse the headers
            HashMap<AsciiBuffer, AsciiBuffer> headers = parseHeaders(in);

            // Read in the data part.
            Buffer data = null;
            AsciiBuffer contentLength = headers.get(CONTENT_LENGTH);
            if (contentLength != null) {

                // Bless the client, he's telling us how much data to read in.
                int length = parseContentLength(contentLength);

                byte[] b = new byte[length];
                in.readFully(b);
                data = new Buffer(b);
                if (in.readByte() != 0) {
                    throw new ProtocolException(CONTENT_LENGTH + " bytes were read and "
                            + "there was no trailing null byte", true);
                }

            } else {

                // We don't know how much to read.. data ends when we hit a 0
                byte b;
                DataByteArrayOutputStream baos = null;
                while ((b = in.readByte()) != 0) {

                    if (baos == null) {
                        baos = new DataByteArrayOutputStream();
                    } else if (baos.size() > MAX_DATA_LENGTH) {
                        throw new ProtocolException("The maximum data length was exceeded", true);
                    }

                    baos.write(b);
                }

                if (baos != null) {
                    baos.close();
                    data = baos.toBuffer();
                }

            }

            StompFrame frame = new StompFrame(action, headers, data);
//            System.out.println("<===");
//            System.out.println(frame);
//            System.out.println("<===");
            return frame;

        } catch (ProtocolException e) {
            return new StompFrameError(e);
        }

    }

    protected int parseContentLength(AsciiBuffer contentLength) throws ProtocolException {
        int length;
        try {
            length = Integer.parseInt(contentLength.trim().toString());
        } catch (NumberFormatException e) {
            throw new ProtocolException("Specified content-length is not a valid integer", true);
        }

        if (length > MAX_DATA_LENGTH) {
            throw new ProtocolException("The maximum data length was exceeded", true);
        }

        return length;
    }

    protected HashMap<AsciiBuffer, AsciiBuffer> parseHeaders(DataInput in) throws IOException {
        HashMap<AsciiBuffer, AsciiBuffer> headers = new HashMap<AsciiBuffer, AsciiBuffer>(25);
        while (true) {
            Buffer line = readLine(in, MAX_HEADER_LENGTH, "The maximum header length was exceeded");
            if (line != null && line.trim().length() > 0) {

                if (headers.size() > MAX_HEADERS) {
                    throw new ProtocolException("The maximum number of headers was exceeded", true);
                }

                try {
                    int seperatorIndex = line.indexOf(COLON);
                    AsciiBuffer name = line.slice(0, seperatorIndex).trim().ascii();
                    AsciiBuffer value = line.slice(seperatorIndex + 1, line.length()).trim().ascii();
                    headers.put(name, value);
                } catch (Exception e) {
                    throw new ProtocolException("Unable to parser header line [" + line + "]", true);
                }
            } else {
                break;
            }
        }
        return headers;
    }

    private AsciiBuffer parseAction(DataInput in) throws IOException {
        Buffer action = null;

        // skip white space to next real action line
        while (true) {
            action = readLine(in, MAX_COMMAND_LENGTH, "The maximum command length was exceeded");
            if (action == null) {
                throw new IOException("connection was closed");
            } else {
                action = action.trim();
                if (action.length() > 0) {
                    break;
                }
            }
        }
        return action.ascii();
    }

    private Buffer readLine(DataInput in, int maxLength, String errorMessage) throws IOException {
        byte b;
        DataByteArrayOutputStream baos = new DataByteArrayOutputStream();
        while ((b = in.readByte()) != '\n') {
            if (baos.size() > maxLength) {
                throw new ProtocolException(errorMessage, true);
            }
            baos.write(b);
        }
        baos.close();
        return baos.toBuffer();
    }

}
