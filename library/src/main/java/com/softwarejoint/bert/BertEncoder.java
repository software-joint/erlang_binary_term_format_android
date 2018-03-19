package com.softwarejoint.bert;

import android.annotation.SuppressLint;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class BertEncoder extends Bert implements DistributionHeader {

    private ByteBuffer buffer;

    private int MIN_BUFFER_SIZE = 1024 * 8;  //8kb default size
    private int bufferSize = MIN_BUFFER_SIZE;

    private boolean stringAsBinary;
    private boolean mapKeysAsAtom;
    private boolean mapKeysAsString;
    private boolean mapAsPropList;

    public BertEncoder setEncodeStringAsBinary(boolean enabled) {
        stringAsBinary = enabled;
        return this;
    }

    public BertEncoder setEncodeMapAsPropList(boolean enabled) {
        mapAsPropList = enabled;
        return this;
    }

    public BertEncoder setEncodeMapKeysAsAtom(boolean enabled) {
        mapKeysAsAtom = enabled;
        return this;
    }

    public BertEncoder setEncodeMapKeysAsString(boolean enabled) {
        mapKeysAsString = enabled;
        return this;
    }

    public BertEncoder setBufferSize(int bufferSize) {
        this.bufferSize = Math.max(bufferSize, MIN_BUFFER_SIZE);
        return this;
    }

    private void resetBuffer() {
        if (buffer != null) buffer.clear();
        int size = Math.max(bufferSize, MIN_BUFFER_SIZE);
        if (buffer == null || buffer.capacity() < size) {
            buffer = ByteBuffer.allocate(bufferSize);
        }
    }

    public byte[] encodeAny(Object any) {
        resetBuffer();
        buffer.put(MAGIC);
        encode(any);
        byte[] encodedData = new byte[buffer.position()];
        buffer.flip();
        buffer.get(encodedData);
        return encodedData;
    }

    @SuppressWarnings("unchecked")
    private void encode(Object any) {
        if (any == null) {
            encodeNull();
        } else if (any instanceof Byte) {
            encodeByte((Byte) any);
        } else if (any instanceof Short) {
            encodeInteger(((Short) any).intValue());
        } else if (any instanceof Integer) {
            encodeInteger((Integer) any);
        } else if (any instanceof Float) {
            encodeDouble(((Float) any).doubleValue());
        } else if (any instanceof Double) {
            encodeDouble((Double) any);
        } else if (any instanceof Long) {
            encodeLong((Long) any);
        } else if (any instanceof BigInteger) {
            encodeBigInteger((BigInteger) any);
        } else if (any instanceof Boolean) {
            encodeAtom((Boolean) any);
        } else if (any instanceof BertAtom) {
            encodeAtom((BertAtom) any);
        } else if (any instanceof String) {
            encodeString((String) any);
        } else if (any instanceof byte[]) {
            encodeBinary((byte[]) any);
        } else if (any instanceof BertTuple) {
            encodeTuple((BertTuple) any);
        } else if (any instanceof List) {
            encodeList((List) any);
        } else if (any instanceof Map) {
            encodeMap((Map) any);
        } else if (any.getClass().isArray()) {
            encodeArray(any);
        }
    }

    private void encodeByte(Byte i) {
        buffer.put(SMALL_INTEGER_EXT);
        putUnsignedByte(i);
    }

    private void encodeInteger(Integer i) {
        if (SMALL_INTEGER_EXT_MIN_VAL <= i && i <= SMALL_INTEGER_EXT_MAX_VAL) {
            encodeByte(i.byteValue());
        } else {
            buffer.put(INTEGER_EXT);
            buffer.putInt(i);
        }
    }

    private void encodeDouble(Double d) {
        switch (minorVersion) {
            case ExternalFormat.OLD:
                buffer.put(FLOAT_EXT);
                //noinspection MalformedFormatString
                @SuppressLint("DefaultLocale")
                String s = String.format("%0$-31.20e", d).replace(' ', '\0');
                buffer.put(s.getBytes(utf8Charset));
                break;
            case ExternalFormat.NEW:
                buffer.put(NEW_FLOAT_EXT);
                buffer.putDouble(d);
                break;
        }
    }

    private void encodeLong(Long l) {
        if (Integer.MIN_VALUE <= l && l <= Integer.MAX_VALUE) {
            encodeInteger(l.intValue());
            return;
        }

        encodeBigInteger(BigInteger.valueOf(l));
    }

    private void encodeBigInteger(BigInteger b) {
        byte[] bytes = b.toByteArray();

        if (bytes.length <= SMALL_INTEGER_EXT_MAX_VAL) {
            buffer.put(SMALL_BIG_EXT);
            putUnsignedByte(bytes.length);
        } else {
            buffer.put(LARGE_BIG_EXT);
            putUnsignedInt(bytes.length);
        }

        buffer.put((byte) ((b.signum() >= 0) ? 0 : 1));
        for (int i = bytes.length; i > 0; i--) {
            buffer.put(bytes[i - 1]);
        }
    }

    private void encodeAtom(Boolean b) {
        encodeAtom(b.toString());
    }

    private void encodeAtom(BertAtom atom) {
        encodeAtom(atom.get());
    }

    private void encodeAtom(String s) {
        if (s.length() <= SMALL_INTEGER_EXT_MAX_VAL) {
            buffer.put(SMALL_ATOM_EXT);
            putUnsignedByte(s.length());
        } else {
            buffer.put(ATOM_EXT);
            putUnsignedShort(s.length());
        }

        buffer.put(s.getBytes(utf8Charset));
    }

    private void encodeString(String s) {
        if (stringAsBinary) {
            encodeBinary(s.getBytes(utf8Charset));
        } else if (s.length() <= STRING_EXT_MAX_VAL) {
            buffer.put(STRING_EXT);
            putUnsignedShort(s.length());
            buffer.put(s.getBytes(utf8Charset));
        } else {
            encodeArray(s.getBytes(utf8Charset));
        }
    }

    private void encodeBinary(byte[] array) {
        buffer.put(BINARY_EXT);
        putUnsignedInt(array.length);
        buffer.put(array);
    }

    private void encodeNull() {
        buffer.put(NIL_EXT);
    }

    private void encodeTuple(BertTuple tuple) {

        if (tuple.size() <= SMALL_INTEGER_EXT_MAX_VAL) {
            buffer.put(SMALL_TUPLE_EXT);
            putUnsignedByte(tuple.size());
        } else {
            buffer.put(LARGE_TUPLE_EXT);
            putUnsignedInt(tuple.size());
        }

        for (Object o : tuple) {
            encode(o);
        }
    }

    private void encodeList(List list) {

        buffer.put(LIST_EXT);

        putUnsignedInt(list.size());
        for (Object o : list) {
            encode(o);
        }
        encodeNull();
    }

    private void encodeMap(Map<Object, Object> map) throws IllegalArgumentException {

        if (mapAsPropList) buffer.put(LIST_EXT);
        else buffer.put(MAP_EXT);

        putUnsignedInt(map.size());

        for (Map.Entry<Object, Object> entry : map.entrySet()) {

            if (mapAsPropList) {
                buffer.put(SMALL_TUPLE_EXT);
                putUnsignedByte(2);
            }

            if (mapKeysAsAtom) {
                encodeAtom(entry.getKey().toString());
            } else if (mapKeysAsString) {
                encodeString(entry.getKey().toString());
            } else {
                encode(entry.getKey());
            }

            encode(entry.getValue());
        }

        if (mapAsPropList) encodeNull();
    }

    private void encodeArray(Object array) {

        buffer.put(LIST_EXT);

        int elements = Array.getLength(array);
        putUnsignedInt(elements);
        for (int i = 0; i < elements; i++) {
            encode(Array.get(array, i));
        }

        encodeNull();
    }

    private void putUnsignedByte(int i) {
        buffer.put((byte) (i & 0xff));
    }

    private void putUnsignedShort(int i) {
        buffer.putShort((short) (i & 0xffff));
    }

    private void putUnsignedInt(long l) {
        buffer.putInt((int) (l & 0xffffffffL));
    }
}
