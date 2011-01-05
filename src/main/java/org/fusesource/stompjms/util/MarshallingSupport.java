/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.stompjms.util;


import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

/**
 * The fixed version of the UTF8 encoding function. Some older JVM's UTF8
 * encoding function breaks when handling large strings.
 * 
 * @version $Revision$
 */
public final class MarshallingSupport {

    public static final byte NULL = 0;
    public static final byte BOOLEAN_TYPE = 1;
    public static final byte BYTE_TYPE = 2;
    public static final byte CHAR_TYPE = 3;
    public static final byte SHORT_TYPE = 4;
    public static final byte INTEGER_TYPE = 5;
    public static final byte LONG_TYPE = 6;
    public static final byte DOUBLE_TYPE = 7;
    public static final byte FLOAT_TYPE = 8;
    public static final byte STRING_TYPE = 9;
    public static final byte BYTE_ARRAY_TYPE = 10;
    public static final byte MAP_TYPE = 11;
    public static final byte LIST_TYPE = 12;
    public static final byte BIG_STRING_TYPE = 13;

    private MarshallingSupport() {
    }
    
    public static void marshalPrimitiveMap(Map map, DataByteArrayOutputStream out) throws IOException {
        if (map == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(map.size());
            for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
                String name = (String)iter.next();
                out.writeUTF(name);
                Object value = map.get(name);
                marshalPrimitive(out, value);
            }
        }
    }

    public static Map<String, Object> unmarshalPrimitiveMap(DataInputStream in) throws IOException {
        return unmarshalPrimitiveMap(in, Integer.MAX_VALUE);
    }

    /**
     * @param in
     * @return
     * @throws IOException
     * @throws IOException
     */
    public static Map<String, Object> unmarshalPrimitiveMap(DataInputStream in, int maxPropertySize) throws IOException {
        int size = in.readInt();
        if (size > maxPropertySize) {
            throw new IOException("Primitive map is larger than the allowed size: " + size);
        }
        if (size < 0) {
            return null;
        } else {
            Map<String, Object> rc = new HashMap<String, Object>(size);
            for (int i = 0; i < size; i++) {
                String name = in.readUTF();
                rc.put(name, unmarshalPrimitive(in));
            }
            return rc;
        }

    }

    public static void marshalPrimitiveList(List list, DataByteArrayOutputStream out) throws IOException {
        out.writeInt(list.size());
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Object element = (Object)iter.next();
            marshalPrimitive(out, element);
        }
    }

    public static List<Object> unmarshalPrimitiveList(DataInputStream in) throws IOException {
        int size = in.readInt();
        List<Object> answer = new ArrayList<Object>(size);
        while (size-- > 0) {
            answer.add(unmarshalPrimitive(in));
        }
        return answer;
    }

    public static void marshalPrimitive(DataByteArrayOutputStream out, Object value) throws IOException {
        if (value == null) {
            marshalNull(out);
        } else if (value.getClass() == Boolean.class) {
            marshalBoolean(out, ((Boolean)value).booleanValue());
        } else if (value.getClass() == Byte.class) {
            marshalByte(out, ((Byte)value).byteValue());
        } else if (value.getClass() == Character.class) {
            marshalChar(out, ((Character)value).charValue());
        } else if (value.getClass() == Short.class) {
            marshalShort(out, ((Short)value).shortValue());
        } else if (value.getClass() == Integer.class) {
            marshalInt(out, ((Integer)value).intValue());
        } else if (value.getClass() == Long.class) {
            marshalLong(out, ((Long)value).longValue());
        } else if (value.getClass() == Float.class) {
            marshalFloat(out, ((Float)value).floatValue());
        } else if (value.getClass() == Double.class) {
            marshalDouble(out, ((Double)value).doubleValue());
        } else if (value.getClass() == byte[].class) {
            marshalByteArray(out, (byte[])value);
        } else if (value.getClass() == String.class) {
            marshalString(out, (String)value);
        } else if (value instanceof Map) {
            out.writeByte(MAP_TYPE);
            marshalPrimitiveMap((Map)value, out);
        } else if (value instanceof List) {
            out.writeByte(LIST_TYPE);
            marshalPrimitiveList((List)value, out);
        } else {
            throw new IOException("Object is not a primitive: " + value);
        }
    }

    public static Object unmarshalPrimitive(DataInputStream in) throws IOException {
        Object value = null;
        byte type = in.readByte();
        switch (type) {
        case BYTE_TYPE:
            value = Byte.valueOf(in.readByte());
            break;
        case BOOLEAN_TYPE:
            value = in.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
            break;
        case CHAR_TYPE:
            value = Character.valueOf(in.readChar());
            break;
        case SHORT_TYPE:
            value = Short.valueOf(in.readShort());
            break;
        case INTEGER_TYPE:
            value = Integer.valueOf(in.readInt());
            break;
        case LONG_TYPE:
            value = Long.valueOf(in.readLong());
            break;
        case FLOAT_TYPE:
            value = new Float(in.readFloat());
            break;
        case DOUBLE_TYPE:
            value = new Double(in.readDouble());
            break;
        case BYTE_ARRAY_TYPE:
            value = new byte[in.readInt()];
            in.readFully((byte[])value);
            break;
        case STRING_TYPE:
            value = in.readUTF();
            break;
        case BIG_STRING_TYPE:
            value = in.readUTF();
            break;
        case MAP_TYPE:
            value = unmarshalPrimitiveMap(in);
            break;
        case LIST_TYPE:
            value = unmarshalPrimitiveList(in);
            break;
        case NULL:
            value = null;
            break;
        default:
            throw new IOException("Unknown primitive type: " + type);
        }
        return value;
    }

    public static void marshalNull(DataByteArrayOutputStream out) throws IOException {
        out.writeByte(NULL);
    }

    public static void marshalBoolean(DataByteArrayOutputStream dataOut, boolean value) throws IOException {
        dataOut.writeByte(BOOLEAN_TYPE);
        dataOut.writeBoolean(value);
    }

    public static void marshalByte(DataByteArrayOutputStream out, byte value) throws IOException {
        out.writeByte(BYTE_TYPE);
        out.writeByte(value);
    }

    public static void marshalChar(DataByteArrayOutputStream out, char value) throws IOException {
        out.writeByte(CHAR_TYPE);
        out.writeChar(value);
    }

    public static void marshalShort(DataByteArrayOutputStream out, short value) throws IOException {
        out.writeByte(SHORT_TYPE);
        out.writeShort(value);
    }

    public static void marshalInt(DataByteArrayOutputStream out, int value) throws IOException {
        out.writeByte(INTEGER_TYPE);
        out.writeInt(value);
    }

    public static void marshalLong(DataByteArrayOutputStream out, long value) throws IOException {
        out.writeByte(LONG_TYPE);
        out.writeLong(value);
    }

    public static void marshalFloat(DataByteArrayOutputStream out, float value) throws IOException {
        out.writeByte(FLOAT_TYPE);
        out.writeFloat(value);
    }

    public static void marshalDouble(DataByteArrayOutputStream out, double value) throws IOException {
        out.writeByte(DOUBLE_TYPE);
        out.writeDouble(value);
    }

    public static void marshalByteArray(DataByteArrayOutputStream out, byte[] value) throws IOException {
        marshalByteArray(out, value, 0, value.length);
    }

    public static void marshalByteArray(DataByteArrayOutputStream out, byte[] value, int offset, int length) throws IOException {
        out.writeByte(BYTE_ARRAY_TYPE);
        out.writeInt(length);
        out.write(value, offset, length);
    }

    public static void marshalString(DataByteArrayOutputStream out, String s) throws IOException {
        // If it's too big, out.writeUTF may not able able to write it out.
        if (s.length() < Short.MAX_VALUE / 4) {
            out.writeByte(STRING_TYPE);
            out.writeUTF(s);
        } else {
            out.writeByte(BIG_STRING_TYPE);
            out.writeUTF(s);
        }
    }




    

}
