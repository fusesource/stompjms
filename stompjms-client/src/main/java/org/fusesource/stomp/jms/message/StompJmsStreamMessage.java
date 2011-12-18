/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stomp.jms.message;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.stomp.jms.StompJmsExceptionSupport;
import org.fusesource.stomp.jms.util.MarshallingSupport;

import javax.jms.*;
import java.io.EOFException;
import java.io.IOException;

/**
 * A <CODE>StreamMessage</CODE> object is used to send a stream of primitive
 * types in the Java programming language. It is filled and read sequentially.
 * It inherits from the <CODE>Message</CODE> interface and adds a stream message
 * body. Its methods are based largely on those found in
 * <CODE>java.io.DataInputStream</CODE> and
 * <CODE>java.io.DataOutputStream</CODE>.
 * <p/>
 * <p/>
 * The primitive types can be read or written explicitly using methods for each
 * type. They may also be read or written generically as objects. For instance,
 * a call to <CODE>StreamMessage.writeInt(6)</CODE> is equivalent to
 * <CODE>StreamMessage.writeObject(new
 * Integer(6))</CODE>. Both forms are provided, because the explicit form is
 * convenient for static programming, and the object form is needed when types
 * are not known at compile time.
 * <p/>
 * <p/>
 * When the message is first created, and when <CODE>clearBody</CODE> is called,
 * the body of the message is in write-only mode. After the first call to
 * <CODE>reset</CODE> has been made, the message body is in read-only mode.
 * After a message has been sent, the client that sent it can retain and modify
 * it without affecting the message that has been sent. The same message object
 * can be sent multiple times. When a message has been received, the provider
 * has called <CODE>reset</CODE> so that the message body is in read-only mode
 * for the client.
 * <p/>
 * <p/>
 * If <CODE>clearBody</CODE> is called on a message in read-only mode, the
 * message body is cleared and the message body is in write-only mode.
 * <p/>
 * <p/>
 * If a client attempts to read a message in write-only mode, a
 * <CODE>MessageNotReadableException</CODE> is thrown.
 * <p/>
 * <p/>
 * If a client attempts to write a message in read-only mode, a
 * <CODE>MessageNotWriteableException</CODE> is thrown.
 * <p/>
 * <p/>
 * <CODE>StreamMessage</CODE> objects support the following conversion table.
 * The marked cases must be supported. The unmarked cases must throw a
 * <CODE>JMSException</CODE>. The <CODE>String</CODE>-to-primitive conversions
 * may throw a runtime exception if the primitive's <CODE>valueOf()</CODE>
 * method does not accept it as a valid <CODE>String</CODE> representation of
 * the primitive.
 * <p/>
 * <p/>
 * A value written as the row type can be read as the column type.
 * <p/>
 * <p/>
 * <PRE>
 * | | boolean byte short char int long float double String byte[]
 * |----------------------------------------------------------------------
 * |boolean | X X |byte | X X X X X |short | X X X X |char | X X |int | X X X
 * |long | X X |float | X X X |double | X X |String | X X X X X X X X |byte[] |
 * X |----------------------------------------------------------------------
 * <p/>
 * </PRE>
 * <p/>
 * <p/>
 * <p/>
 * Attempting to read a null value as a primitive type must be treated as
 * calling the primitive's corresponding <code>valueOf(String)</code> conversion
 * method with a null value. Since <code>char</code> does not support a
 * <code>String</code> conversion, attempting to read a null value as a
 * <code>char</code> must throw a <code>NullPointerException</code>.
 *
 * @openwire:marshaller code="27"
 * @see javax.jms.Session#createStreamMessage()
 * @see javax.jms.BytesMessage
 * @see javax.jms.MapMessage
 * @see javax.jms.Message
 * @see javax.jms.ObjectMessage
 * @see javax.jms.TextMessage
 */
public class StompJmsStreamMessage extends StompJmsMessage implements StreamMessage {

    protected transient DataByteArrayOutputStream dataOut;
    protected transient DataByteArrayInputStream dataIn;
    protected transient int remainingBytes = -1;

    public JmsMsgType getMsgType() {
        return JmsMsgType.STREAM;
    }

    public StompJmsMessage copy() throws JMSException {
        StompJmsStreamMessage copy = new StompJmsStreamMessage();
        copy(copy);
        return copy;
    }

    public void copy(StompJmsStreamMessage copy) throws JMSException {
        storeContent();
        super.copy(copy);
        copy.dataOut = null;
        copy.dataIn = null;
    }

    public void onSend() throws JMSException {
        super.onSend();
        storeContent();
    }

    public void storeContent() throws JMSException {
        if (dataOut != null) {
            try {
                dataOut.close();
                Buffer bs = dataOut.toBuffer();
                setContent(bs);
                dataOut = null;
            } catch (IOException ioe) {
                throw StompJmsExceptionSupport.create(ioe.getMessage(), ioe);
            }
        }
    }

    /**
     * Clears out the message body. Clearing a message's body does not clear its
     * header values or property entries.
     * <p/>
     * <p/>
     * If this message body was read-only, calling this method leaves the
     * message body in the same state as an empty body in a newly created
     * message.
     *
     * @throws JMSException if the JMS provider fails to clear the message body due to
     *                      some internal error.
     */

    public void clearBody() throws JMSException {
        super.clearBody();
        this.dataOut = null;
        this.dataIn = null;
        this.remainingBytes = -1;
    }

    /**
     * Reads a <code>boolean</code> from the stream message.
     *
     * @return the <code>boolean</code> value read
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */

    public boolean readBoolean() throws JMSException {
        initializeReading();
        try {

            this.dataIn.mark(10);
            int dataType = this.dataIn.read();
            if (dataType == -1) {
                throw new MessageEOFException("reached end of data");
            }
            if (dataType == MarshallingSupport.BOOLEAN_TYPE) {
                return this.dataIn.readBoolean();
            }
            if (dataType == MarshallingSupport.STRING_TYPE) {
                return Boolean.valueOf(this.dataIn.readUTF()).booleanValue();
            }
            if (dataType == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to boolean.");
            } else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a boolean type");
            }
        } catch (EOFException e) {
            throw StompJmsExceptionSupport.createMessageEOFException(e);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.createMessageFormatException(e);
        }
    }

    /**
     * Reads a <code>byte</code> value from the stream message.
     *
     * @return the next byte from the stream message as a 8-bit
     *         <code>byte</code>
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */

    public byte readByte() throws JMSException {
        initializeReading();
        try {

            this.dataIn.mark(10);
            int dataType = this.dataIn.read();
            if (dataType == -1) {
                throw new MessageEOFException("reached end of data");
            }
            if (dataType == MarshallingSupport.BYTE_TYPE) {
                return this.dataIn.readByte();
            }
            if (dataType == MarshallingSupport.STRING_TYPE) {
                return Byte.valueOf(this.dataIn.readUTF()).byteValue();
            }
            if (dataType == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to byte.");
            } else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a byte type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException e) {
                throw StompJmsExceptionSupport.createMessageFormatException(e);
            }
            throw mfe;

        } catch (EOFException e) {
            throw StompJmsExceptionSupport.createMessageEOFException(e);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.createMessageFormatException(e);
        }
    }

    /**
     * Reads a 16-bit integer from the stream message.
     *
     * @return a 16-bit integer from the stream message
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */

    public short readShort() throws JMSException {
        initializeReading();
        try {

            this.dataIn.mark(17);
            int dataType = this.dataIn.read();
            if (dataType == -1) {
                throw new MessageEOFException("reached end of data");
            }
            if (dataType == MarshallingSupport.SHORT_TYPE) {
                return this.dataIn.readShort();
            }
            if (dataType == MarshallingSupport.BYTE_TYPE) {
                return this.dataIn.readByte();
            }
            if (dataType == MarshallingSupport.STRING_TYPE) {
                return Short.valueOf(this.dataIn.readUTF()).shortValue();
            }
            if (dataType == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to short.");
            } else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a short type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException e) {
                throw StompJmsExceptionSupport.createMessageFormatException(e);
            }
            throw mfe;

        } catch (EOFException e) {
            throw StompJmsExceptionSupport.createMessageEOFException(e);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.createMessageFormatException(e);
        }

    }

    /**
     * Reads a Unicode character value from the stream message.
     *
     * @return a Unicode character from the stream message
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid
     * @throws MessageNotReadableException if the message is in write-only mode.
     */

    public char readChar() throws JMSException {
        initializeReading();
        try {

            this.dataIn.mark(17);
            int dataType = this.dataIn.read();
            if (dataType == -1) {
                throw new MessageEOFException("reached end of data");
            }
            if (dataType == MarshallingSupport.CHAR_TYPE) {
                return this.dataIn.readChar();
            }
            if (dataType == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to char.");
            } else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a char type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException e) {
                throw StompJmsExceptionSupport.createMessageFormatException(e);
            }
            throw mfe;

        } catch (EOFException e) {
            throw StompJmsExceptionSupport.createMessageEOFException(e);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.createMessageFormatException(e);
        }
    }

    /**
     * Reads a 32-bit integer from the stream message.
     *
     * @return a 32-bit integer value from the stream message, interpreted as an
     *         <code>int</code>
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */

    public int readInt() throws JMSException {
        initializeReading();
        try {

            this.dataIn.mark(33);
            int dataType = this.dataIn.read();
            if (dataType == -1) {
                throw new MessageEOFException("reached end of data");
            }
            if (dataType == MarshallingSupport.INTEGER_TYPE) {
                return this.dataIn.readInt();
            }
            if (dataType == MarshallingSupport.SHORT_TYPE) {
                return this.dataIn.readShort();
            }
            if (dataType == MarshallingSupport.BYTE_TYPE) {
                return this.dataIn.readByte();
            }
            if (dataType == MarshallingSupport.STRING_TYPE) {
                return Integer.valueOf(this.dataIn.readUTF()).intValue();
            }
            if (dataType == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to int.");
            } else {
                this.dataIn.reset();
                throw new MessageFormatException(" not an int type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException e) {
                throw StompJmsExceptionSupport.createMessageFormatException(e);
            }
            throw mfe;

        } catch (EOFException e) {
            throw StompJmsExceptionSupport.createMessageEOFException(e);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.createMessageFormatException(e);
        }
    }

    /**
     * Reads a 64-bit integer from the stream message.
     *
     * @return a 64-bit integer value from the stream message, interpreted as a
     *         <code>long</code>
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */

    public long readLong() throws JMSException {
        initializeReading();
        try {

            this.dataIn.mark(65);
            int dataType = this.dataIn.read();
            if (dataType == -1) {
                throw new MessageEOFException("reached end of data");
            }
            if (dataType == MarshallingSupport.LONG_TYPE) {
                return this.dataIn.readLong();
            }
            if (dataType == MarshallingSupport.INTEGER_TYPE) {
                return this.dataIn.readInt();
            }
            if (dataType == MarshallingSupport.SHORT_TYPE) {
                return this.dataIn.readShort();
            }
            if (dataType == MarshallingSupport.BYTE_TYPE) {
                return this.dataIn.readByte();
            }
            if (dataType == MarshallingSupport.STRING_TYPE) {
                return Long.valueOf(this.dataIn.readUTF()).longValue();
            }
            if (dataType == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to long.");
            } else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a long type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException e) {
                throw StompJmsExceptionSupport.createMessageFormatException(e);
            }
            throw mfe;

        } catch (EOFException e) {
            throw StompJmsExceptionSupport.createMessageEOFException(e);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.createMessageFormatException(e);
        }
    }

    /**
     * Reads a <code>float</code> from the stream message.
     *
     * @return a <code>float</code> value from the stream message
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */

    public float readFloat() throws JMSException {
        initializeReading();
        try {
            this.dataIn.mark(33);
            int dataType = this.dataIn.read();
            if (dataType == -1) {
                throw new MessageEOFException("reached end of data");
            }
            if (dataType == MarshallingSupport.FLOAT_TYPE) {
                return this.dataIn.readFloat();
            }
            if (dataType == MarshallingSupport.STRING_TYPE) {
                return Float.valueOf(this.dataIn.readUTF()).floatValue();
            }
            if (dataType == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to float.");
            } else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a float type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException e) {
                throw StompJmsExceptionSupport.createMessageFormatException(e);
            }
            throw mfe;

        } catch (EOFException e) {
            throw StompJmsExceptionSupport.createMessageEOFException(e);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.createMessageFormatException(e);
        }
    }

    /**
     * Reads a <code>double</code> from the stream message.
     *
     * @return a <code>double</code> value from the stream message
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */

    public double readDouble() throws JMSException {
        initializeReading();
        try {

            this.dataIn.mark(65);
            int dataType = this.dataIn.read();
            if (dataType == -1) {
                throw new MessageEOFException("reached end of data");
            }
            if (dataType == MarshallingSupport.DOUBLE_TYPE) {
                return this.dataIn.readDouble();
            }
            if (dataType == MarshallingSupport.FLOAT_TYPE) {
                return this.dataIn.readFloat();
            }
            if (dataType == MarshallingSupport.STRING_TYPE) {
                return Double.valueOf(this.dataIn.readUTF()).doubleValue();
            }
            if (dataType == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to double.");
            } else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a double type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException e) {
                throw StompJmsExceptionSupport.createMessageFormatException(e);
            }
            throw mfe;

        } catch (EOFException e) {
            throw StompJmsExceptionSupport.createMessageEOFException(e);
        } catch (IOException e) {
            throw StompJmsExceptionSupport.createMessageFormatException(e);
        }
    }

    /**
     * Reads a <CODE>String</CODE> from the stream message.
     *
     * @return a Unicode string from the stream message
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */

    public String readString() throws JMSException {
        initializeReading();
        try {

            this.dataIn.mark(65);
            int dataType = this.dataIn.read();
            if (dataType == -1) {
                throw new MessageEOFException("reached end of data");
            }
            if (dataType == MarshallingSupport.NULL) {
                return null;
            }
            if (dataType == MarshallingSupport.BIG_STRING_TYPE) {
                return dataIn.readUTF();
            }
            if (dataType == MarshallingSupport.STRING_TYPE) {
                return this.dataIn.readUTF();
            }
            if (dataType == MarshallingSupport.LONG_TYPE) {
                return new Long(this.dataIn.readLong()).toString();
            }
            if (dataType == MarshallingSupport.INTEGER_TYPE) {
                return new Integer(this.dataIn.readInt()).toString();
            }
            if (dataType == MarshallingSupport.SHORT_TYPE) {
                return new Short(this.dataIn.readShort()).toString();
            }
            if (dataType == MarshallingSupport.BYTE_TYPE) {
                return new Byte(this.dataIn.readByte()).toString();
            }
            if (dataType == MarshallingSupport.FLOAT_TYPE) {
                return new Float(this.dataIn.readFloat()).toString();
            }
            if (dataType == MarshallingSupport.DOUBLE_TYPE) {
                return new Double(this.dataIn.readDouble()).toString();
            }
            if (dataType == MarshallingSupport.BOOLEAN_TYPE) {
                return (this.dataIn.readBoolean() ? Boolean.TRUE : Boolean.FALSE).toString();
            }
            if (dataType == MarshallingSupport.CHAR_TYPE) {
                return new Character(this.dataIn.readChar()).toString();
            } else {
                this.dataIn.reset();
                throw new MessageFormatException(" not a String type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException e) {
                throw StompJmsExceptionSupport.createMessageFormatException(e);
            }
            throw mfe;

        } catch (IOException e) {
            throw StompJmsExceptionSupport.createMessageFormatException(e);
        }
    }

    /**
     * Reads a byte array field from the stream message into the specified
     * <CODE>byte[]</CODE> object (the read buffer).
     * <p/>
     * <p/>
     * To read the field value, <CODE>readBytes</CODE> should be successively
     * called until it returns a value less than the length of the read buffer.
     * The value of the bytes in the buffer following the last byte read is
     * undefined.
     * <p/>
     * <p/>
     * If <CODE>readBytes</CODE> returns a value equal to the length of the
     * buffer, a subsequent <CODE>readBytes</CODE> call must be made. If there
     * are no more bytes to be read, this call returns -1.
     * <p/>
     * <p/>
     * If the byte array field value is null, <CODE>readBytes</CODE> returns -1.
     * <p/>
     * <p/>
     * If the byte array field value is empty, <CODE>readBytes</CODE> returns 0.
     * <p/>
     * <p/>
     * Once the first <CODE>readBytes</CODE> call on a <CODE>byte[]</CODE> field
     * value has been made, the full value of the field must be read before it
     * is valid to read the next field. An attempt to read the next field before
     * that has been done will throw a <CODE>MessageFormatException</CODE>.
     * <p/>
     * <p/>
     * To read the byte field value into a new <CODE>byte[]</CODE> object, use
     * the <CODE>readObject</CODE> method.
     *
     * @param value the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the byte field has been reached
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     * @see #readObject()
     */

    public int readBytes(byte[] value) throws JMSException {

        initializeReading();
        try {
            if (value == null) {
                throw new NullPointerException();
            }

            if (remainingBytes == -1) {
                this.dataIn.mark(value.length + 1);
                int dataType = this.dataIn.read();
                if (dataType == -1) {
                    throw new MessageEOFException("reached end of data");
                }
                if (dataType != MarshallingSupport.BYTE_ARRAY_TYPE) {
                    throw new MessageFormatException("Not a byte array");
                }
                remainingBytes = this.dataIn.readInt();
            } else if (remainingBytes == 0) {
                remainingBytes = -1;
                return -1;
            }

            if (value.length <= remainingBytes) {
                // small buffer
                remainingBytes -= value.length;
                this.dataIn.readFully(value);
                return value.length;
            } else {
                // big buffer
                int rc = this.dataIn.read(value, 0, remainingBytes);
                remainingBytes = 0;
                return rc;
            }

        } catch (Throwable e) {
            throw StompJmsExceptionSupport.createMessageFormatException(e);
        }
    }

    /**
     * Reads an object from the stream message.
     * <p/>
     * <p/>
     * This method can be used to return, in objectified format, an object in
     * the Java programming language ("Java object") that has been written to
     * the stream with the equivalent <CODE>writeObject</CODE> method call, or
     * its equivalent primitive <CODE>write<I>type</I></CODE> method.
     * <p/>
     * <p/>
     * Note that byte values are returned as <CODE>byte[]</CODE>, not
     * <CODE>Byte[]</CODE>.
     * <p/>
     * <p/>
     * An attempt to call <CODE>readObject</CODE> to read a byte field value
     * into a new <CODE>byte[]</CODE> object before the full value of the byte
     * field has been read will throw a <CODE>MessageFormatException</CODE>.
     *
     * @return a Java object from the stream message, in objectified format (for
     *         example, if the object was written as an <CODE>int</CODE>, an
     *         <CODE>Integer</CODE> is returned)
     * @throws JMSException                if the JMS provider fails to read the message due to some
     *                                     internal error.
     * @throws MessageEOFException         if unexpected end of message stream has been reached.
     * @throws MessageFormatException      if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     * @see #readBytes(byte[] value)
     */

    public Object readObject() throws JMSException {
        initializeReading();
        try {
            this.dataIn.mark(65);
            int dataType = this.dataIn.read();
            if (dataType == -1) {
                throw new MessageEOFException("reached end of data");
            }
            if (dataType == MarshallingSupport.NULL) {
                return null;
            }
            if (dataType == MarshallingSupport.BIG_STRING_TYPE) {
                return dataIn.readUTF();
            }
            if (dataType == MarshallingSupport.STRING_TYPE) {
                return this.dataIn.readUTF();
            }
            if (dataType == MarshallingSupport.LONG_TYPE) {
                return Long.valueOf(this.dataIn.readLong());
            }
            if (dataType == MarshallingSupport.INTEGER_TYPE) {
                return Integer.valueOf(this.dataIn.readInt());
            }
            if (dataType == MarshallingSupport.SHORT_TYPE) {
                return Short.valueOf(this.dataIn.readShort());
            }
            if (dataType == MarshallingSupport.BYTE_TYPE) {
                return Byte.valueOf(this.dataIn.readByte());
            }
            if (dataType == MarshallingSupport.FLOAT_TYPE) {
                return new Float(this.dataIn.readFloat());
            }
            if (dataType == MarshallingSupport.DOUBLE_TYPE) {
                return new Double(this.dataIn.readDouble());
            }
            if (dataType == MarshallingSupport.BOOLEAN_TYPE) {
                return this.dataIn.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
            }
            if (dataType == MarshallingSupport.CHAR_TYPE) {
                return Character.valueOf(this.dataIn.readChar());
            }
            if (dataType == MarshallingSupport.BYTE_ARRAY_TYPE) {
                int len = this.dataIn.readInt();
                byte[] value = new byte[len];
                this.dataIn.readFully(value);
                return value;
            } else {
                this.dataIn.reset();
                throw new MessageFormatException("unknown type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException e) {
                throw StompJmsExceptionSupport.createMessageFormatException(e);
            }
            throw mfe;

        } catch (IOException e) {
            JMSException jmsEx = new MessageFormatException(e.getMessage());
            jmsEx.setLinkedException(e);
            throw jmsEx;
        }
    }

    /**
     * Writes a <code>boolean</code> to the stream message. The value
     * <code>true</code> is written as the value <code>(byte)1</code>; the value
     * <code>false</code> is written as the value <code>(byte)0</code>.
     *
     * @param value the <code>boolean</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeBoolean(boolean value) throws JMSException {
        initializeWriting();
        try {
            MarshallingSupport.marshalBoolean(dataOut, value);
        } catch (IOException ioe) {
            throw StompJmsExceptionSupport.create(ioe);
        }
    }

    /**
     * Writes a <code>byte</code> to the stream message.
     *
     * @param value the <code>byte</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeByte(byte value) throws JMSException {
        initializeWriting();
        try {
            MarshallingSupport.marshalByte(dataOut, value);
        } catch (IOException ioe) {
            throw StompJmsExceptionSupport.create(ioe);
        }
    }

    /**
     * Writes a <code>short</code> to the stream message.
     *
     * @param value the <code>short</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeShort(short value) throws JMSException {
        initializeWriting();
        try {
            MarshallingSupport.marshalShort(dataOut, value);
        } catch (IOException ioe) {
            throw StompJmsExceptionSupport.create(ioe);
        }
    }

    /**
     * Writes a <code>char</code> to the stream message.
     *
     * @param value the <code>char</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeChar(char value) throws JMSException {
        initializeWriting();
        try {
            MarshallingSupport.marshalChar(dataOut, value);
        } catch (IOException ioe) {
            throw StompJmsExceptionSupport.create(ioe);
        }
    }

    /**
     * Writes an <code>int</code> to the stream message.
     *
     * @param value the <code>int</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeInt(int value) throws JMSException {
        initializeWriting();
        try {
            MarshallingSupport.marshalInt(dataOut, value);
        } catch (IOException ioe) {
            throw StompJmsExceptionSupport.create(ioe);
        }
    }

    /**
     * Writes a <code>long</code> to the stream message.
     *
     * @param value the <code>long</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeLong(long value) throws JMSException {
        initializeWriting();
        try {
            MarshallingSupport.marshalLong(dataOut, value);
        } catch (IOException ioe) {
            throw StompJmsExceptionSupport.create(ioe);
        }
    }

    /**
     * Writes a <code>float</code> to the stream message.
     *
     * @param value the <code>float</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeFloat(float value) throws JMSException {
        initializeWriting();
        try {
            MarshallingSupport.marshalFloat(dataOut, value);
        } catch (IOException ioe) {
            throw StompJmsExceptionSupport.create(ioe);
        }
    }

    /**
     * Writes a <code>double</code> to the stream message.
     *
     * @param value the <code>double</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeDouble(double value) throws JMSException {
        initializeWriting();
        try {
            MarshallingSupport.marshalDouble(dataOut, value);
        } catch (IOException ioe) {
            throw StompJmsExceptionSupport.create(ioe);
        }
    }

    /**
     * Writes a <code>String</code> to the stream message.
     *
     * @param value the <code>String</code> value to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeString(String value) throws JMSException {
        initializeWriting();
        try {
            if (value == null) {
                MarshallingSupport.marshalNull(dataOut);
            } else {
                MarshallingSupport.marshalString(dataOut, value);
            }
        } catch (IOException ioe) {
            throw StompJmsExceptionSupport.create(ioe);
        }
    }

    /**
     * Writes a byte array field to the stream message.
     * <p/>
     * <p/>
     * The byte array <code>value</code> is written to the message as a byte
     * array field. Consecutively written byte array fields are treated as two
     * distinct fields when the fields are read.
     *
     * @param value the byte array value to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeBytes(byte[] value) throws JMSException {
        writeBytes(value, 0, value.length);
    }

    /**
     * Writes a portion of a byte array as a byte array field to the stream
     * message.
     * <p/>
     * <p/>
     * The a portion of the byte array <code>value</code> is written to the
     * message as a byte array field. Consecutively written byte array fields
     * are treated as two distinct fields when the fields are read.
     *
     * @param value  the byte array value to be written
     * @param offset the initial offset within the byte array
     * @param length the number of bytes to use
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeBytes(byte[] value, int offset, int length) throws JMSException {
        initializeWriting();
        try {
            MarshallingSupport.marshalByteArray(dataOut, value, offset, length);
        } catch (IOException ioe) {
            throw StompJmsExceptionSupport.create(ioe);
        }
    }

    /**
     * Writes an object to the stream message.
     * <p/>
     * <p/>
     * This method works only for the objectified primitive object types (
     * <code>Integer</code>, <code>Double</code>, <code>Long</code>&nbsp;...),
     * <code>String</code> objects, and byte arrays.
     *
     * @param value the Java object to be written
     * @throws JMSException                 if the JMS provider fails to write the message due to some
     *                                      internal error.
     * @throws MessageFormatException       if the object is invalid.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */

    public void writeObject(Object value) throws JMSException {
        initializeWriting();
        if (value == null) {
            try {
                MarshallingSupport.marshalNull(dataOut);
            } catch (IOException ioe) {
                throw StompJmsExceptionSupport.create(ioe);
            }
        } else if (value instanceof String) {
            writeString(value.toString());
        } else if (value instanceof Character) {
            writeChar(((Character) value).charValue());
        } else if (value instanceof Boolean) {
            writeBoolean(((Boolean) value).booleanValue());
        } else if (value instanceof Byte) {
            writeByte(((Byte) value).byteValue());
        } else if (value instanceof Short) {
            writeShort(((Short) value).shortValue());
        } else if (value instanceof Integer) {
            writeInt(((Integer) value).intValue());
        } else if (value instanceof Float) {
            writeFloat(((Float) value).floatValue());
        } else if (value instanceof Double) {
            writeDouble(((Double) value).doubleValue());
        } else if (value instanceof byte[]) {
            writeBytes((byte[]) value);
        } else if (value instanceof Long) {
            writeLong(((Long) value).longValue());
        } else {
            throw new MessageFormatException("Unsupported Object type: " + value.getClass());
        }
    }

    /**
     * Puts the message body in read-only mode and repositions the stream of
     * bytes to the beginning.
     *
     * @throws JMSException if an internal error occurs
     */

    public void reset() throws JMSException {
        storeContent();
        this.dataIn = null;
        this.dataOut = null;
        this.remainingBytes = -1;
        setReadOnlyBody(true);
    }

    private void initializeWriting() throws MessageNotWriteableException {
        checkReadOnlyBody();
        if (this.dataOut == null) {
            this.dataOut = new DataByteArrayOutputStream();
        }
    }

    protected void checkWriteOnlyBody() throws MessageNotReadableException {
        if (!readOnlyBody) {
            throw new MessageNotReadableException("Message body is write-only");
        }
    }

    private void initializeReading() throws MessageNotReadableException {
        checkWriteOnlyBody();
        if (this.dataIn == null) {
            Buffer buffer = getContent();
            if (buffer == null) {
                buffer = new Buffer(new byte[]{}, 0, 0);
            }
            this.dataIn = new DataByteArrayInputStream(buffer);
        }
    }

    public String toString() {
        return super.toString() + " StompJmsStreamMessage{ dataOut = " + dataOut
                + ", dataIn = " + dataIn + " }";
    }
}
