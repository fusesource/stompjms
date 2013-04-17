package org.fusesource.stomp.jms;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author zapodot at gmail dot com
 */
@RunWith(MockitoJUnitRunner.class)
public class StompJmsSessionUntransactedTest {

    @Mock
    private StompJmsConnection stompJmsConnection;

    @Mock
    private StompChannel stompChannel;

    @Mock
    private StompServerAdaptor stompServerAdaptor;

    @Mock
    private StompJmsPrefetch stompJmsPrefetch;

    @Mock
    private TemporaryQueue temporaryQueue;

    @Mock
    private TemporaryTopic temporaryTopic;
    private StompJmsSession stompJmsSession;

    @Before
    public void setUp() throws Exception {
        when(stompJmsConnection.createChannel(any(StompJmsSession.class))).thenReturn(stompChannel);
        stompJmsConnection.prefetch = stompJmsPrefetch;

        when(stompChannel.getServerAdaptor()).thenReturn(stompServerAdaptor);
        when(stompServerAdaptor.createTemporaryQueue(any(StompJmsSession.class))).thenReturn(temporaryQueue);
        when(stompServerAdaptor.createTemporaryTopic(any(StompJmsSession.class))).thenReturn(temporaryTopic);
        stompJmsSession = new StompJmsSession(stompJmsConnection, Session.AUTO_ACKNOWLEDGE, false);

    }

    @Test
    public void testCreateTemporaryQueue() throws Exception {
        assertSame(temporaryQueue, stompJmsSession.createTemporaryQueue());
        verify(stompJmsConnection).createChannel(any(StompJmsSession.class));
        verify(stompChannel).getServerAdaptor();
        verify(stompServerAdaptor).createTemporaryQueue(same(stompJmsSession));

    }

    @Test
    public void testCreateTemporaryTopic() throws Exception {
        assertSame(temporaryTopic, stompJmsSession.createTemporaryTopic());
        verify(stompJmsConnection).createChannel(any(StompJmsSession.class));
        verify(stompChannel).getServerAdaptor();
        verify(stompServerAdaptor).createTemporaryTopic(same(stompJmsSession));
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(stompJmsConnection, stompChannel, stompServerAdaptor);

    }
}
