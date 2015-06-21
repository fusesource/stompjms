package org.fusesource.stomp.jms;

import static org.fusesource.stomp.client.Constants.ID;
import static org.fusesource.stomp.client.Constants.PERSISTENT;
import static org.fusesource.stomp.client.Constants.TRUE;
import static org.fusesource.stomp.client.Constants.UNSUBSCRIBE;

import java.util.Map;

import javax.jms.JMSException;

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.stomp.codec.StompFrame;

/**
 * Stomp adaptor for RabbitMQ
 */
public class RabbitMQServerAdaptor extends StompServerAdaptor {

	@Override
	public boolean matchesServerAndVersion(String server) {
		return server.startsWith("RabbitMQ/");
	}

	@Override
	public void addSubscribeHeaders(Map<AsciiBuffer, AsciiBuffer> headerMap, boolean persistent, boolean browser,
	        boolean noLocal, StompJmsPrefetch prefetch) throws JMSException {
		if (browser) {
			throw new JMSException("Server does not support browsing over STOMP");
		}
		if (noLocal) {
			throw new JMSException("Server does not support 'no local' semantics over STOMP");
		}
		if (persistent) {
			headerMap.put(PERSISTENT, TRUE);
		}
	}

	@Override
	public StompFrame createUnsubscribeFrame(AsciiBuffer consumerId, boolean persistent) throws JMSException {
		StompFrame frame = new StompFrame();
		Map<AsciiBuffer, AsciiBuffer> headerMap = frame.headerMap();
		frame.action(UNSUBSCRIBE);
		headerMap.put(ID, consumerId);
		if (persistent) {
			headerMap.put(PERSISTENT, TRUE);
		}
		return frame;
	}
}
