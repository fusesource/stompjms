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
import static org.fusesource.hawtbuf.Buffer.*;

public interface Stomp {
    final AsciiBuffer NULL = ascii("\u0000");
    final byte NULL_BYTE = 0;

    final AsciiBuffer NEWLINE = ascii("\n");
    final byte NEWLINE_BYTE = '\n';

    final AsciiBuffer COLON = ascii(":");
    final byte COLON_BYTE = ':';

    final byte ESCAPE_BYTE = '\\';

    final AsciiBuffer ESCAPE_ESCAPE_SEQ = ascii("\\\\");
    final AsciiBuffer COLON_ESCAPE_SEQ = ascii("\\c");
    final AsciiBuffer NEWLINE_ESCAPE_SEQ = ascii("\\n");

    // Commands
    final AsciiBuffer CONNECT = ascii("CONNECT");
    final AsciiBuffer SEND = ascii("SEND");
    final AsciiBuffer DISCONNECT = ascii("DISCONNECT");
    final AsciiBuffer SUBSCRIBE = ascii("SUBSCRIBE");
    final AsciiBuffer UNSUBSCRIBE = ascii("UNSUBSCRIBE");
    final AsciiBuffer MESSAGE = ascii("MESSAGE");

    final AsciiBuffer BEGIN_TRANSACTION = ascii("BEGIN");
    final AsciiBuffer COMMIT_TRANSACTION = ascii("COMMIT");
    final AsciiBuffer ABORT_TRANSACTION = ascii("ABORT");
    final AsciiBuffer BEGIN = ascii("BEGIN");
    final AsciiBuffer COMMIT = ascii("COMMIT");
    final AsciiBuffer ABORT = ascii("ABORT");
    final AsciiBuffer ACK = ascii("ACK");

    // Responses
    final AsciiBuffer CONNECTED = ascii("CONNECTED");
    final AsciiBuffer ERROR = ascii("ERROR");
    final AsciiBuffer RECEIPT = ascii("RECEIPT");

    // Headers
    final AsciiBuffer RECEIPT_REQUESTED = ascii("receipt");
    final AsciiBuffer TRANSACTION = ascii("transaction");
    final AsciiBuffer CONTENT_LENGTH = ascii("content-length");
    final AsciiBuffer CONTENT_TYPE = ascii("content-type");
    final AsciiBuffer TRANSFORMATION = ascii("transformation");
    final AsciiBuffer TRANSFORMATION_ERROR = ascii("transformation-error");

    /**
     * This header is used to instruct ActiveMQ to construct the message
     * based with a specific type.
     */
    final AsciiBuffer AMQ_MESSAGE_TYPE = ascii("amq-msg-type");
    final AsciiBuffer RECEIPT_ID = ascii("receipt-id");
    final AsciiBuffer PERSISTENT = ascii("persistent");
    final AsciiBuffer MESSAGE_HEADER = ascii("message");
    final AsciiBuffer MESSAGE_ID = ascii("message-id");
    final AsciiBuffer CORRELATION_ID = ascii("correlation-id");
    final AsciiBuffer EXPIRATION_TIME = ascii("expires");
    final AsciiBuffer REPLY_TO = ascii("reply-to");
    final AsciiBuffer PRIORITY = ascii("priority");
    final AsciiBuffer REDELIVERED = ascii("redelivered");
    final AsciiBuffer TIMESTAMP = ascii("timestamp");
    final AsciiBuffer TYPE = ascii("type");
    final AsciiBuffer SUBSCRIPTION = ascii("subscription");
    final AsciiBuffer USERID = ascii("JMSXUserID");
    final AsciiBuffer PROPERTIES = ascii("JMSXProperties");
    final AsciiBuffer ACK_MODE = ascii("ack");
    final AsciiBuffer ID = ascii("id");
    final AsciiBuffer SELECTOR = ascii("selector");
    final AsciiBuffer BROWSER = ascii("browser");
    final AsciiBuffer AUTO = ascii("auto");
    final AsciiBuffer CLIENT = ascii("client");
    final AsciiBuffer INDIVIDUAL = ascii("client-individual");
    final AsciiBuffer DESTINATION = ascii("destination");
    final AsciiBuffer LOGIN = ascii("login");
    final AsciiBuffer PASSCODE = ascii("passcode");
    final AsciiBuffer CLIENT_ID = ascii("client-id");
    final AsciiBuffer REQUEST_ID = ascii("request-id");
    final AsciiBuffer SESSION = ascii("session");
    final AsciiBuffer RESPONSE_ID = ascii("response-id");
    final AsciiBuffer ACCEPT_VERSION = ascii("accept-version");
    final AsciiBuffer V1_1 = ascii("1.1");
    final AsciiBuffer V1_0 = ascii("1.0");
    final AsciiBuffer HOST = ascii("host");
    final AsciiBuffer TRUE = ascii("true");
    final AsciiBuffer FALSE = ascii("false");
    final AsciiBuffer END = ascii("end");


//    public enum Transformations {
//        JMS_BYTE,
//        JMS_XML,
//        JMS_JSON,
//        JMS_OBJECT_XML,
//        JMS_OBJECT_JSON,
//        JMS_MAP_XML,
//        JMS_MAP_JSON,
//        JMS_ADVISORY_XML,
//        JMS_ADVISORY_JSON;
//
//        public AsciiBuffer toAsciiBuffer() {
//            return name().replaceAll("_", "-").toLowerCase();
//        }
//
//        public static Transformations getValue(AsciiBuffer value) {
//            return valueOf(value.replaceAll("-", "_").toUpperCase());
//        }
//    }
}
