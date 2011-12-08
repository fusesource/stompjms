/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stompjms.client.callback;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.stompjms.client.Stomp;
import org.fusesource.stompjms.client.StompFrame;
import org.fusesource.stompjms.client.StompProtocolCodec;
import org.fusesource.stompjms.client.transport.SslTransport;
import org.fusesource.stompjms.client.transport.TcpTransport;
import org.fusesource.stompjms.client.transport.Transport;
import org.fusesource.stompjms.client.transport.TransportListener;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Executor;

import static org.fusesource.hawtdispatch.Dispatch.NOOP;
import static org.fusesource.hawtdispatch.Dispatch.createQueue;
import static org.fusesource.stompjms.client.Constants.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ConnectionBuilder {

    private URI remoteURI;
    private URI localURI;
    private SSLContext sslContext;
    private DispatchQueue dispatchQueue;
    private Executor blockingExecutor;
    private int maxReadRate;
    private int maxWriteRate;
    private int trafficClass = TcpTransport.IPTOS_THROUGHPUT;
    private int receiveBufferSize = 1024*64;
    private int sendBufferSize = 1024*64;
    private boolean useLocalHost = true;

    private String login;
    private String passcode;
    private String host;
    private String clientId;
    private String version = "1.1";
    private Properties customHeaders;

    public ConnectionBuilder(URI remoteURI) {
        assert remoteURI !=null : "URI should not be null.";
        this.remoteURI = remoteURI;
        this.host = remoteURI.getHost();
    }


    public ConnectionBuilder login(String login) {
        this.login = login;
        return this;
    }

    public ConnectionBuilder passcode(String passcode) {
        this.passcode = passcode;
        return this;
    }

    public ConnectionBuilder host(String host) {
        this.host = host;
        return this;
    }

    public ConnectionBuilder version(String version) {
        this.version = version;
        return this;
    }

    public ConnectionBuilder customHeaders(Properties customHeaders) {
        this.customHeaders = customHeaders;
        return this;
    }

    public ConnectionBuilder blockingExecutor(Executor blockingExecutor) {
        this.blockingExecutor = blockingExecutor;
        return this;
    }

    public ConnectionBuilder dispatchQueue(DispatchQueue dispatchQueue) {
        this.dispatchQueue = dispatchQueue;
        return this;
    }

    public ConnectionBuilder localURI(URI localURI) {
        this.localURI = localURI;
        return this;
    }

    public ConnectionBuilder maxReadRate(int maxReadRate) {
        this.maxReadRate = maxReadRate;
        return this;
    }

    public ConnectionBuilder maxWriteRate(int maxWriteRate) {
        this.maxWriteRate = maxWriteRate;
        return this;
    }

    public ConnectionBuilder receiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
        return this;
    }

    public ConnectionBuilder sendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }

    public ConnectionBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public ConnectionBuilder trafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
        return this;
    }

    public ConnectionBuilder useLocalHost(boolean useLocalHost) {
        this.useLocalHost = useLocalHost;
        return this;
    }

    public void connect(final Callback<Connection> cb) {
        assert cb!=null : "Callback should not be null.";
        try {
            String scheme = remoteURI.getScheme();
            final Transport transport;
            if( "tcp".equals(scheme) ) {
                transport = new TcpTransport();
            }  else if( SslTransport.SCHEME_MAPPINGS.containsKey(scheme)) {
                SslTransport ssl = new SslTransport();
                if( sslContext == null ) {
                    sslContext = SSLContext.getInstance(SslTransport.SCHEME_MAPPINGS.get(scheme));
                }
                ssl.setSSLContext(sslContext);
                if( blockingExecutor == null ) {
                    blockingExecutor = Stomp.getBlockingThreadPool();
                }
                ssl.setBlockingExecutor(blockingExecutor);
                transport = ssl;
            } else {
                throw new Exception("Unsupported URI scheme '"+scheme+"'");
            }

            if(dispatchQueue == null) {
                dispatchQueue = createQueue("stomp client");
            }
            transport.setDispatchQueue(dispatchQueue);
            transport.setProtocolCodec(new StompProtocolCodec());

            if( transport instanceof TcpTransport ) {
                TcpTransport tcp = (TcpTransport)transport;
                tcp.setMaxReadRate(maxReadRate);
                tcp.setMaxWriteRate(maxWriteRate);
                tcp.setReceiveBufferSize(receiveBufferSize);
                tcp.setSendBufferSize(sendBufferSize);
                tcp.setTrafficClass(trafficClass);
                tcp.setUseLocalHost(useLocalHost);
                tcp.connecting(remoteURI, localURI);
            }

            TransportListener commandListener = new TransportListener() {
                public void onTransportConnected() {
                    transport.resumeRead();

                    StompFrame frame = new StompFrame(CONNECT);
                    if (version != null) {
                        frame.addHeader(ACCEPT_VERSION, StompFrame.encodeHeader(version));
                    }
                    if (host != null) {
                        frame.addHeader(HOST, StompFrame.encodeHeader(host));
                    }
                    if (login != null) {
                        frame.addHeader(LOGIN, StompFrame.encodeHeader(login));
                    }
                    if (passcode != null) {
                        frame.addHeader(PASSCODE, StompFrame.encodeHeader(passcode));
                    }
                    if (clientId != null) {
                        frame.addHeader(CLIENT_ID, StompFrame.encodeHeader(passcode));
                    }
                    if( customHeaders!=null ) {
                        for (Object key : customHeaders.keySet()) {
                            frame.addHeader(StompFrame.encodeHeader(key.toString()), StompFrame.encodeHeader(customHeaders.get(key).toString()));
                        }
                    }

                    boolean accepted = transport.offer(frame);
                    assert accepted: "First frame should always be accepted by the transport";

                }

                public void onTransportCommand(Object command) {
                    StompFrame response = (StompFrame) command;
                    if (response.action().equals(ERROR)) {
                        cb.failure(new IOException("Could not connect: " + response.errorMessage()));
                    } else if (!response.action().equals(CONNECTED)) {
                        cb.failure(new IOException("Could not connect. Received unexpected frame: " + response.toString()));
                    } else {
                        transport.suspendRead();
                        cb.success(new Connection(transport, response));
                    }
                }

                public void onTransportFailure(final IOException error) {
                    transport.stop(new Runnable() {
                        public void run() {
                            cb.failure(error);
                        }
                    });
                }

                public void onRefill() {
                }

                public void onTransportDisconnected() {
                }
            };
            transport.setTransportListener(commandListener);
            transport.start(NOOP);

        } catch (Throwable e) {
            cb.failure(e);
        }


    }
}
