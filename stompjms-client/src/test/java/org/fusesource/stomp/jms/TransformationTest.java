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

import junit.framework.TestCase;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.stomp.codec.StompFrame;
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.fusesource.stomp.jms.message.StompJmsTextMessage;
import org.fusesource.stomp.jms.util.StompTranslator;

import static org.fusesource.stomp.client.Constants.TRANSFORMATION;

public class TransformationTest extends TestCase {

     public void testTextMessageType() throws Exception {
         StompJmsTextMessage msg = new StompJmsTextMessage();
         msg.setText("test");
         msg.onSend();
         StompFrame frame = msg.getFrame();
         assertEquals(StompJmsMessage.JmsMsgType.TEXT.toString(), frame.getHeader(TRANSFORMATION).toString());
     }

    public void testParse() throws Exception {

        StompFrame frame = new StompFrame();
        frame.addHeader(new AsciiBuffer("content-type"), new AsciiBuffer("application/json"));
        frame.content(new Buffer("test".getBytes("UTF-8")));

        StompJmsMessage msg = StompTranslator.convert(frame);
        msg.setFrame(frame);

        assertEquals("test", ((StompJmsTextMessage)msg).getText());

    }

}
