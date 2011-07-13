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
    AsciiBuffer NULL = ascii("\u0000");
    byte NULL_BYTE = NULL.get(0);

    AsciiBuffer NEWLINE = ascii("\n");
    byte NEWLINE_BYTE = NEWLINE.get(0);

    // Commands
    AsciiBuffer CONNECT = ascii("CONNECT");
    AsciiBuffer SEND = ascii("SEND");
    AsciiBuffer DISCONNECT = ascii("DISCONNECT");
    AsciiBuffer SUBSCRIBE = ascii("SUBSCRIBE");
    AsciiBuffer UNSUBSCRIBE = ascii("UNSUBSCRIBE");
    AsciiBuffer MESSAGE = ascii("MESSAGE");

    AsciiBuffer BEGIN_TRANSACTION = ascii("BEGIN");
    AsciiBuffer COMMIT_TRANSACTION = ascii("COMMIT");
    AsciiBuffer ABORT_TRANSACTION = ascii("ABORT");
    AsciiBuffer BEGIN = ascii("BEGIN");
    AsciiBuffer COMMIT = ascii("COMMIT");
    AsciiBuffer ABORT = ascii("ABORT");
    AsciiBuffer ACK = ascii("ACK");

    // Responses
    AsciiBuffer CONNECTED = ascii("CONNECTED");
    AsciiBuffer ERROR = ascii("ERROR");
    AsciiBuffer RECEIPT = ascii("RECEIPT");

    // Headers
    AsciiBuffer SEPERATOR = ascii(":");
    byte SEPERATOR_BYTE = SEPERATOR.get(0);
    AsciiBuffer RECEIPT_REQUESTED = ascii("receipt");
    AsciiBuffer TRANSACTION = ascii("transaction");
    AsciiBuffer CONTENT_LENGTH = ascii("content-length");
    AsciiBuffer CONTENT_TYPE = ascii("content-type");
    AsciiBuffer TRANSFORMATION = ascii("transformation");
    AsciiBuffer TRANSFORMATION_ERROR = ascii("transformation-error");

    /**
     * This header is used to instruct ActiveMQ to construct the message
     * based with a specific type.
     */
    AsciiBuffer AMQ_MESSAGE_TYPE = ascii("amq-msg-type");
    AsciiBuffer RECEIPT_ID = ascii("receipt-id");
    AsciiBuffer PERSISTENT = ascii("persistent");

    AsciiBuffer MESSAGE_HEADER = ascii("message");
    AsciiBuffer MESSAGE_ID = ascii("message-id");
    AsciiBuffer CORRELATION_ID = ascii("correlation-id");
    AsciiBuffer EXPIRATION_TIME = ascii("expires");
    AsciiBuffer REPLY_TO = ascii("reply-to");
    AsciiBuffer PRIORITY = ascii("priority");
    AsciiBuffer REDELIVERED = ascii("redelivered");
    AsciiBuffer TIMESTAMP = ascii("timestamp");
    AsciiBuffer TYPE = ascii("type");
    AsciiBuffer SUBSCRIPTION = ascii("subscription");
    AsciiBuffer USERID = ascii("JMSXUserID");
    AsciiBuffer PROPERTIES = ascii("JMSXProperties");

    AsciiBuffer ACK_MODE = ascii("ack");
    AsciiBuffer ID = ascii("id");
    AsciiBuffer SELECTOR = ascii("selector");
    AsciiBuffer BROWSER = ascii("browser");

    AsciiBuffer AUTO = ascii("auto");
    AsciiBuffer CLIENT = ascii("client");
    AsciiBuffer INDIVIDUAL = ascii("client-individual");

    AsciiBuffer DESTINATION = ascii("destination");
    AsciiBuffer LOGIN = ascii("login");
    AsciiBuffer PASSCODE = ascii("passcode");
    AsciiBuffer CLIENT_ID = ascii("client-id");
    AsciiBuffer REQUEST_ID = ascii("request-id");

    AsciiBuffer SESSION = ascii("session");
    AsciiBuffer RESPONSE_ID = ascii("response-id");

    AsciiBuffer ACCEPT_VERSION = ascii("accept-version");
    AsciiBuffer V1_1 = ascii("1.1");
    AsciiBuffer V1_0 = ascii("1.0");
    AsciiBuffer HOST = ascii("host");

    AsciiBuffer TRUE = ascii("true");
    AsciiBuffer FALSE = ascii("false");
    AsciiBuffer END = ascii("end");

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
