/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms.message;

import org.fusesource.stompjms.*;

import javax.jms.*;
import java.util.Enumeration;

/**
 * A helper class for converting normal JMS interfaces into StompJms specific
 * ones.
 *
 * @version $Revision: 1.1 $
 */
public final class StompJmsMessageTransformation {

    private StompJmsMessageTransformation() {
    }

    /**
     * Creates a an available JMS message from another provider.
     *
     * @param destination - Destination to be converted into StompJms's
     *                    implementation.
     * @return StompJmsDestination - StompJms's implementation of the
     *         destination.
     * @throws JMSException
     * @throws JMSException if an error occurs
     */
    public static StompJmsDestination transformDestination(Destination destination) throws JMSException {
        StompJmsDestination result = null;

        if (destination != null) {
            if (destination instanceof StompJmsDestination) {
                return (StompJmsDestination) destination;

            } else {
                if (destination instanceof TemporaryQueue) {
                    result = new StompJmsTempQueue(((Queue) destination).getQueueName());
                } else if (destination instanceof TemporaryTopic) {
                    result = new StompJmsTempTopic(((Topic) destination).getTopicName());
                } else if (destination instanceof Queue) {
                    result = new StompJmsQueue(((Queue) destination).getQueueName());
                } else if (destination instanceof Topic) {
                    result = new StompJmsTopic(((Topic) destination).getTopicName());
                }
            }
        }

        return result;
    }

    /**
     * Creates a fast shallow copy of the current StompJmsMessage or creates a
     * whole new message instance from an available JMS message from another
     * provider.
     *
     * @param message    - Message to be converted into StompJms's implementation.
     * @param connection
     * @return StompJmsMessage - StompJms's implementation object of the
     *         message.
     * @throws JMSException if an error occurs
     */
    public static StompJmsMessage transformMessage(Message message)
            throws JMSException {
        if (message instanceof StompJmsMessage) {
            return (StompJmsMessage) message;

        } else {
            StompJmsMessage activeMessage = null;

            if (message instanceof BytesMessage) {
                BytesMessage bytesMsg = (BytesMessage) message;
                bytesMsg.reset();
                StompJmsBytesMessage msg = new StompJmsBytesMessage();
                try {
                    for (; ;) {
                        // Reads a byte from the message stream until the stream
                        // is empty
                        msg.writeByte(bytesMsg.readByte());
                    }
                } catch (MessageEOFException e) {
                    // if an end of message stream as expected
                } catch (JMSException e) {
                }

                activeMessage = msg;
            } else if (message instanceof MapMessage) {
                MapMessage mapMsg = (MapMessage) message;
                StompJmsMapMessage msg = new StompJmsMapMessage();
                Enumeration iter = mapMsg.getMapNames();

                while (iter.hasMoreElements()) {
                    String name = iter.nextElement().toString();
                    msg.setObject(name, mapMsg.getObject(name));
                }

                activeMessage = msg;
            } else if (message instanceof ObjectMessage) {
                ObjectMessage objMsg = (ObjectMessage) message;
                StompJmsObjectMessage msg = new StompJmsObjectMessage();
                msg.setObject(objMsg.getObject());
                msg.storeContent();
                activeMessage = msg;
            } else if (message instanceof StreamMessage) {
                StreamMessage streamMessage = (StreamMessage) message;
                streamMessage.reset();
                StompJmsStreamMessage msg = new StompJmsStreamMessage();
                Object obj = null;

                try {
                    while ((obj = streamMessage.readObject()) != null) {
                        msg.writeObject(obj);
                    }
                } catch (MessageEOFException e) {
                    // if an end of message stream as expected
                } catch (JMSException e) {
                }

                activeMessage = msg;
            } else if (message instanceof TextMessage) {
                TextMessage textMsg = (TextMessage) message;
                StompJmsTextMessage msg = new StompJmsTextMessage();
                msg.setText(textMsg.getText());
                activeMessage = msg;
            } else {
                activeMessage = new StompJmsMessage();
            }

            copyProperties(message, activeMessage);

            return activeMessage;
        }
    }

    /**
     * Copies the standard JMS and user defined properties from the givem
     * message to the specified message
     *
     * @param fromMessage the message to take the properties from
     * @param toMessage   the message to add the properties to
     * @throws JMSException
     */
    public static void copyProperties(Message fromMessage, Message toMessage) throws JMSException {
        toMessage.setJMSMessageID(fromMessage.getJMSMessageID());
        toMessage.setJMSCorrelationID(fromMessage.getJMSCorrelationID());
        toMessage.setJMSReplyTo(transformDestination(fromMessage.getJMSReplyTo()));
        toMessage.setJMSDestination(transformDestination(fromMessage.getJMSDestination()));
        toMessage.setJMSDeliveryMode(fromMessage.getJMSDeliveryMode());
        toMessage.setJMSRedelivered(fromMessage.getJMSRedelivered());
        toMessage.setJMSType(fromMessage.getJMSType());
        toMessage.setJMSExpiration(fromMessage.getJMSExpiration());
        toMessage.setJMSPriority(fromMessage.getJMSPriority());
        toMessage.setJMSTimestamp(fromMessage.getJMSTimestamp());

        Enumeration propertyNames = fromMessage.getPropertyNames();

        while (propertyNames.hasMoreElements()) {
            String name = propertyNames.nextElement().toString();
            Object obj = fromMessage.getObjectProperty(name);
            toMessage.setObjectProperty(name, obj);
        }
    }
}
