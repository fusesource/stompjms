/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stomp.jms;

import static org.junit.Assert.*;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public class Assert {

    /**
     * Tests if firstSet and secondSet are equal.
     *
     * @param messsage - string to be displayed when the assertion fails.
     * @param firstSet[] - set of messages to be compared with its counterpart
     *                in the secondset.
     * @param secondSet[] - set of messages to be compared with its counterpart
     *                in the firstset.
     * @throws JMSException
     */
	public static void assertTextMessagesEqual(Message[] firstSet, Message[] secondSet) throws JMSException {
        assertTextMessagesEqual("", firstSet, secondSet);
    }

    /**
     * Tests if firstSet and secondSet are equal.
     *
     * @param messsage - string to be displayed when the assertion fails.
     * @param firstSet[] - set of messages to be compared with its counterpart
     *                in the secondset.
     * @param secondSet[] - set of messages to be compared with its counterpart
     *                in the firstset.
     */
    public static void assertTextMessagesEqual(String messsage, Message[] firstSet, Message[] secondSet) throws JMSException {
        assertEquals("Message count does not match: " + messsage, firstSet.length, secondSet.length);

        for (int i = 0; i < secondSet.length; i++) {
            TextMessage m1 = (TextMessage)firstSet[i];
            TextMessage m2 = (TextMessage)secondSet[i];
            assertTextMessageEquals("Message " + (i + 1) + " did not match : ", m1, m2);
        }
    }

    /**
     * Tests if m1 and m2 are equal.
     *
     * @param m1 - message to be compared with m2.
     * @param m2 - message to be compared with m1.
     * @throws JMSException
     */
    public static void assertTextMessageEquals(TextMessage m1, TextMessage m2) throws JMSException {
        assertEquals("", m1, m2);
    }

    /**
     * Tests if m1 and m2 are equal.
     *
     * @param message - string to be displayed when the assertion fails.
     * @param m1 - message to be compared with m2.
     * @param m2 - message to be compared with m1.
     */
    public static void assertTextMessageEquals(String message, TextMessage m1, TextMessage m2) throws JMSException {
        assertFalse(message + ": expected {" + m1 + "}, but was {" + m2 + "}", m1 == null ^ m2 == null);

        if (m1 == null) {
            return;
        }

        assertEquals(message, m1.getText(), m2.getText());
    }

    /**
     * Tests if m1 and m2 are equal.
     *
     * @param m1 - message to be compared with m2.
     * @param m2 - message to be compared with m1.
     * @throws JMSException
     */
    public static void assertMessageEquals(Message m1, Message m2) throws JMSException {
        assertMessageEquals("", m1, m2);
    }

    /**
     * Tests if m1 and m2 are equal.
     *
     * @param message - error message.
     * @param m1 - message to be compared with m2.
     * @param m2 -- message to be compared with m1.
     */
    public static void assertMessageEquals(String message, Message m1, Message m2) throws JMSException {
        assertFalse(message + ": expected {" + m1 + "}, but was {" + m2 + "}", m1 == null ^ m2 == null);

        if (m1 == null) {
            return;
        }

        assertTrue(message + ": expected {" + m1 + "}, but was {" + m2 + "}", m1.getClass() == m2.getClass());

        if (m1 instanceof TextMessage) {
            assertTextMessageEquals(message, (TextMessage)m1, (TextMessage)m2);
        } else {
            assertEquals(message, m1, m2);
        }
    }

}
