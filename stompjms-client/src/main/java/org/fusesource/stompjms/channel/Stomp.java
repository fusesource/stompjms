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

public interface Stomp {
    String NULL = "\u0000";
    String NEWLINE = "\n";

    public static interface Commands {
        String CONNECT = "CONNECT";
        String SEND = "SEND";
        String DISCONNECT = "DISCONNECT";
        String SUBSCRIBE = "SUBSCRIBE";
        String UNSUBSCRIBE = "UNSUBSCRIBE";
        String MESSAGE = "MESSAGE";

        String BEGIN_TRANSACTION = "BEGIN";
        String COMMIT_TRANSACTION = "COMMIT";
        String ABORT_TRANSACTION = "ABORT";
        String BEGIN = "BEGIN";
        String COMMIT = "COMMIT";
        String ABORT = "ABORT";
        String ACK = "ACK";
    }

    public interface Responses {
        String CONNECTED = "CONNECTED";
        String ERROR = "ERROR";
        String MESSAGE = "MESSAGE";
        String RECEIPT = "RECEIPT";
    }

    public interface Headers {
        String SEPERATOR = ":";
        String RECEIPT_REQUESTED = "receipt";
        String TRANSACTION = "transaction";
        String CONTENT_LENGTH = "content-length";
        String CONTENT_TYPE = "content-type";
        String TRANSFORMATION = "transformation";
        String TRANSFORMATION_ERROR = "transformation-error";
        /**
         * This header is used to instruct ActiveMQ to construct the message
         * based with a specific type.
         */
        String AMQ_MESSAGE_TYPE = "amq-msg-type";

        public interface Response {
            String RECEIPT_ID = "receipt-id";
        }

        public interface Send {
            String DESTINATION = "destination";
            String CORRELATION_ID = "correlation-id";
            String REPLY_TO = "reply-to";
            String EXPIRATION_TIME = "expires";
            String PRIORITY = "priority";
            String TYPE = "type";
            String PERSISTENT = "persistent";
        }

        public interface Message {
            String MESSAGE_ID = "message-id";
            String DESTINATION = "destination";
            String CORRELATION_ID = "correlation-id";
            String EXPIRATION_TIME = "expires";
            String REPLY_TO = "reply-to";
            String PRIORITY = "priority";
            String REDELIVERED = "redelivered";
            String TIMESTAMP = "timestamp";
            String TYPE = "type";
            String SUBSCRIPTION = "subscription";
            String USERID = "JMSXUserID";
            String PROPERTIES = "JMSXProperties";
        }

        public interface Subscribe {
            String DESTINATION = "destination";
            String ACK_MODE = "ack";
            String ID = "id";
            String SELECTOR = "selector";
            String PERSISTENT = "persistent";
            String BROWSER = "browser";

            public interface AckModeValues {
                String AUTO = "auto";
                String CLIENT = "client";
                String INDIVIDUAL = "client-individual";
            }
        }

        public interface Unsubscribe {
            String DESTINATION = "destination";
            String ID = "id";
        }

        public interface Connect {
            String LOGIN = "login";
            String PASSCODE = "passcode";
            String CLIENT_ID = "client-id";
            String REQUEST_ID = "request-id";
        }

        public interface Error {
            String MESSAGE = "message";
        }

        public interface Connected {
            String SESSION = "session";
            String RESPONSE_ID = "response-id";
        }

        public interface Ack {
            String SUBSCRIPTION = "subscription";
            String MESSAGE_ID = "message-id";
        }
    }

    public enum Transformations {
        JMS_BYTE,
        JMS_XML,
        JMS_JSON,
        JMS_OBJECT_XML,
        JMS_OBJECT_JSON,
        JMS_MAP_XML,
        JMS_MAP_JSON,
        JMS_ADVISORY_XML,
        JMS_ADVISORY_JSON;

        public String toString() {
            return name().replaceAll("_", "-").toLowerCase();
        }

        public static Transformations getValue(String value) {
            return valueOf(value.replaceAll("-", "_").toUpperCase());
        }
    }
}
