/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stomp.client;

import static org.fusesource.hawtdispatch.Dispatch.NOOP;
import static org.fusesource.hawtdispatch.Dispatch.createQueue;
import static org.fusesource.stomp.client.Constants.ACCEPT_VERSION;
import static org.fusesource.stomp.client.Constants.CLIENT_ID;
import static org.fusesource.stomp.client.Constants.CONNECT;
import static org.fusesource.stomp.client.Constants.CONNECTED;
import static org.fusesource.stomp.client.Constants.ERROR;
import static org.fusesource.stomp.client.Constants.HEARTBEAT;
import static org.fusesource.stomp.client.Constants.HOST;
import static org.fusesource.stomp.client.Constants.LOGIN;
import static org.fusesource.stomp.client.Constants.PASSCODE;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.transport.DefaultTransportListener;
import org.fusesource.hawtdispatch.transport.SslTransport;
import org.fusesource.hawtdispatch.transport.TcpTransport;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.TransportListener;
import org.fusesource.stomp.codec.StompFrame;
import org.fusesource.stomp.codec.StompProtocolCodec;


/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Stomp {

    ///////////////////////////////////////////////////////////////////
    // Class variables / methods
    ///////////////////////////////////////////////////////////////////

    private static final long KEEP_ALIVE = Long.parseLong(System.getProperty("stompjms.thread.keep_alive", ""+1000));
    private static final long STACK_SIZE = Long.parseLong(System.getProperty("stompjms.thread.stack_size", ""+1024*512));
    public static final String HEARTBEAT_INTERVAL = System.getProperty("stompjms.heartbeat", "0,0");

    private static ThreadPoolExecutor blockingThreadPool;
    public synchronized static ThreadPoolExecutor getBlockingThreadPool() {
        if( blockingThreadPool == null ) {
            blockingThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, KEEP_ALIVE, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
                    public Thread newThread(final Runnable r) {
                        Thread rc = new Thread(null, r, "Stomp JMS Task", STACK_SIZE);
                        rc.setDaemon(true);
                        return rc;
                    }
                }) {

                    @Override
                    public void shutdown() {
                        // we don't ever shutdown since we are shared..
                    }

                    @Override
                    public List<Runnable> shutdownNow() {
                        // we don't ever shutdown since we are shared..
                        return Collections.emptyList();
                    }
                };
        }
        return blockingThreadPool;
    }

    public synchronized static void setBlockingThreadPool(final ThreadPoolExecutor pool) {
        blockingThreadPool = pool;
    }

    ///////////////////////////////////////////////////////////////////
    // Instance Variables
    ///////////////////////////////////////////////////////////////////

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

    ///////////////////////////////////////////////////////////////////
    // Instance methods
    ///////////////////////////////////////////////////////////////////

    public void connectCallback(final Callback<CallbackConnection> cb) {
        assert cb!=null : "Callback should not be null.";
        try {
            String scheme = remoteURI.getScheme();
            final Transport transport;
            if( "tcp".equals(scheme) ) {
                transport = new TcpTransport();
            }  else if( SslTransport.protocol(scheme)!=null ) {
                SslTransport ssl = new SslTransport();
                if( sslContext == null ) {
                    sslContext = SSLContext.getDefault();
                }
                ssl.setSSLContext(sslContext);
                transport = ssl;
            } else {
                throw new Exception("Unsupported URI scheme '"+scheme+"'");
            }

            if( blockingExecutor == null ) {
                blockingExecutor = Stomp.getBlockingThreadPool();
            }
            transport.setBlockingExecutor(blockingExecutor);

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

            TransportListener commandListener = new DefaultTransportListener() {
                @Override
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
                        frame.addHeader(CLIENT_ID, StompFrame.encodeHeader(clientId));
                    }
                    frame.addHeader(HEARTBEAT, StompFrame.encodeHeader(HEARTBEAT_INTERVAL));
                    if( customHeaders!=null ) {
                        for (Object key : customHeaders.keySet()) {
                            frame.addHeader(StompFrame.encodeHeader(key.toString()), StompFrame.encodeHeader(customHeaders.get(key).toString()));
                        }
                    }

                    boolean accepted = transport.offer(frame);
                    assert accepted: "First frame should always be accepted by the transport";

                }

                @Override
				public void onTransportCommand(final Object command) {
                    StompFrame response = (StompFrame) command;
                    if (response.action().equals(ERROR)) {
                        cb.onFailure(new IOException("Could not connect: " + response.errorMessage()));
                    } else if (!response.action().equals(CONNECTED)) {
                        cb.onFailure(new IOException("Could not connect. Received unexpected frame: " + response.toString()));
                    } else {
                        transport.suspendRead();
                        cb.onSuccess(new CallbackConnection(transport, response));
                    }
                }

                @Override
				public void onTransportFailure(final IOException error) {
                    transport.stop(new Task() {
                        @Override
						public void run() {
                            cb.onFailure(error);
                        }
                    });
                }
            };
            transport.setTransportListener(commandListener);
            transport.start(NOOP);

        } catch (Throwable e) {
            cb.onFailure(e);
        }

    }

    public Future<FutureConnection> connectFuture() {
        final Promise<FutureConnection> future = new Promise<FutureConnection>();
        connectCallback(new Callback<CallbackConnection>() {
            @Override
			public void onFailure(final Throwable value) {
                future.onFailure(value);
            }

            @Override
			public void onSuccess(final CallbackConnection value) {
                future.onSuccess(new FutureConnection(value));
            }
        });
        return future;
    }

    public BlockingConnection connectBlocking() throws IOException {
        try {
            return new BlockingConnection(connectFuture().await());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////

    public Stomp() {
    }
    public Stomp(final String uri) throws URISyntaxException {
        this(new URI(uri));
    }
    public Stomp(final String host, final int port) throws URISyntaxException {
        this(new URI("tcp://"+host+":"+port));
    }

    public Stomp(final URI remoteURI) {
        assert remoteURI !=null : "URI should not be null.";
        this.remoteURI = remoteURI;
        this.host = remoteURI.getHost();
    }

    ///////////////////////////////////////////////////////////////////
    // Getters/Setters
    ///////////////////////////////////////////////////////////////////

    public void setRemoteURI(final URI remoteURI) {
        assert remoteURI !=null : "URI should not be null.";
        this.remoteURI = remoteURI;
    }

    public void setLogin(final String login) {
        this.login = login;
    }

    public void setPasscode(final String passcode) {
        this.passcode = passcode;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setCustomHeaders(final Properties customHeaders) {
        this.customHeaders = customHeaders;
    }

    public void setBlockingExecutor(final Executor blockingExecutor) {
        this.blockingExecutor = blockingExecutor;
    }

    public void setDispatchQueue(final DispatchQueue dispatchQueue) {
        this.dispatchQueue = dispatchQueue;
    }

    public void setLocalURI(final URI localURI) {
        this.localURI = localURI;
    }

    public void setMaxReadRate(final int maxReadRate) {
        this.maxReadRate = maxReadRate;
    }

    public void setMaxWriteRate(final int maxWriteRate) {
        this.maxWriteRate = maxWriteRate;
    }

    public void setReceiveBufferSize(final int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public void setSendBufferSize(final int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public void setSslContext(final SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public void setTrafficClass(final int trafficClass) {
        this.trafficClass = trafficClass;
    }

    public void setUseLocalHost(final boolean useLocalHost) {
        this.useLocalHost = useLocalHost;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public Executor getBlockingExecutor() {
        return blockingExecutor;
    }

    public String getClientId() {
        return clientId;
    }

    public Properties getCustomHeaders() {
        return customHeaders;
    }

    public DispatchQueue getDispatchQueue() {
        return dispatchQueue;
    }

    public String getHost() {
        return host;
    }

    public URI getLocalURI() {
        return localURI;
    }

    public String getLogin() {
        return login;
    }

    public int getMaxReadRate() {
        return maxReadRate;
    }

    public int getMaxWriteRate() {
        return maxWriteRate;
    }

    public String getPasscode() {
        return passcode;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public URI getRemoteURI() {
        return remoteURI;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public int getTrafficClass() {
        return trafficClass;
    }

    public boolean isUseLocalHost() {
        return useLocalHost;
    }

    public String getVersion() {
        return version;
    }



//    static public CallbackConnectionBuilder callback(URI uri) {
//        return new CallbackConnectionBuilder(uri);
//    }
//    static public CallbackConnectionBuilder callback(String uri) throws URISyntaxException {
//        return callback(new URI(uri));
//    }
//    static public CallbackConnectionBuilder callback(String host, int port) throws URISyntaxException {
//        return callback("tcp://"+host+":"+port);
//    }
//
//    static public FutureConnectionBuilder future(URI uri) {
//        return new FutureConnectionBuilder(callback(uri));
//    }
//    static public FutureConnectionBuilder future(String uri) throws URISyntaxException {
//        return future(new URI(uri));
//    }
//    static public FutureConnectionBuilder future(String host, int port) throws URISyntaxException {
//        return future("tcp://"+host+":"+port);
//    }
//
//    static public BlokcingConnectionBuilder blocking(URI uri) {
//        return new BlokcingConnectionBuilder(future(uri));
//    }
//    static public BlokcingConnectionBuilder blocking(String uri) throws URISyntaxException {
//        return blocking(new URI(uri));
//    }
//    static public BlokcingConnectionBuilder blocking(String host, int port) throws URISyntaxException {
//        return blocking("tcp://"+host+":"+port);
//    }

}
