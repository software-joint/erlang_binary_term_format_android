package com.softwarejoint.bert;

import android.text.TextUtils;

import java.io.InvalidObjectException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class BertDecoder extends Bert implements DistributionHeader {

    private static final BigInteger MAX_LONG_VAL = BigInteger.valueOf(Long.MAX_VALUE);

    private boolean atomAsString;
    private boolean propListsAsMap;
    private boolean mapKeysAsString;
    private boolean shortOrByteAsInt;
    private ArrayList<Object> keys = new ArrayList<>();

    private ByteBuffer buffer;

    public BertDecoder setDecodeAtomAsString(boolean enabled) {
        atomAsString = enabled;
        return this;
    }

    public BertDecoder setDecodePropListsAsMap(boolean enabled) {
        propListsAsMap = enabled;
        return this;
    }

    public BertDecoder setDecodeMapKeysAsString(boolean enabled) {
        mapKeysAsString = enabled;
        return this;
    }

    public BertDecoder setDecodeShortOrByteAsInt(boolean enabled) {
        shortOrByteAsInt = enabled;
        return this;
    }

    public BertDecoder addBinaryValuesAsStringForKey(Object key) {
        keys.add(key);
        return this;
    }

    private boolean shouldDecodeBinaryAsStringForKey(Object key) {
        return keys.contains(key);
    }

    public Object decodeAny(byte[] data) throws InvalidObjectException {
        buffer = ByteBuffer.wrap(data);

        switch (buffer.get()) {
            case MAGIC:
                break;
            default:
                throw new InvalidObjectException("Invalid Format");
        }

        return decode();
    }

    private Object decode() throws InvalidObjectException {
        buffer.mark();
        switch (buffer.get()) {
            case NIL_EXT:
                return new ArrayList<>();
            case SMALL_INTEGER_EXT:
                return decodeShortOrByte();
            case INTEGER_EXT:
                return decodeInteger();
            case FLOAT_EXT:
                return decodeDoubleOrFloat(ExternalFormat.OLD);
            case NEW_FLOAT_EXT:
                return decodeDoubleOrFloat(ExternalFormat.NEW);
            case SMALL_BIG_EXT:
                return decodeLongOrBigInteger(SMALL_BIG_EXT);
            case LARGE_BIG_EXT:
                return decodeLongOrBigInteger(LARGE_BIG_EXT);
            case ATOM_EXT:
                return decodeAtom(ATOM_EXT);
            case SMALL_ATOM_EXT:
                return decodeAtom(SMALL_ATOM_EXT);
            case STRING_EXT:
                return decodeString();
            case BINARY_EXT:
                return decodeBinary();
            case LIST_EXT:
                return decodeList();
            case SMALL_TUPLE_EXT:
                return decodeTuple(SMALL_TUPLE_EXT);
            case LARGE_TUPLE_EXT:
                return decodeTuple(LARGE_TUPLE_EXT);
            case MAP_EXT:
                return decodeMap();
            default:
                buffer.reset();
                throw new InvalidObjectException("Invalid Type " + buffer.get());
        }
    }

    private Object decodeShortOrByte() {
        Short s = getUnsignedByte();
        if (Byte.MIN_VALUE <= s && s <= Byte.MAX_VALUE) {
            return shortOrByteAsInt ? s.intValue() : s.byteValue();
        }

        return shortOrByteAsInt ? s.intValue() : s;
    }

    private Integer decodeInteger() {
        return buffer.getInt();
    }

    private Object decodeDoubleOrFloat(@ExternalFormat int minorVersion) {
        Double d = null;

        switch (minorVersion) {
            case ExternalFormat.OLD:
                d = Double.valueOf(decodeString(FLOAT_LENGTH));
                break;
            case ExternalFormat.NEW:
                d = buffer.getDouble();
                break;
        }

        if (d != null && Float.MIN_VALUE <= d && d <= Float.MAX_VALUE) {
            return d.floatValue();
        }

        return d;
    }

    private Object decodeLongOrBigInteger(int tag) throws InvalidObjectException {
        int byteCount = 0;

        switch (tag) {
            case SMALL_BIG_EXT:
                byteCount = getUnsignedByte();
                break;
            case LARGE_BIG_EXT:
                long count = getUnsignedInt();
                if (count > Integer.MAX_VALUE) {
                    throw new InvalidObjectException("Max byte array size exceeded");
                }
                byteCount = (int) count;
                break;
        }

        int signum = (buffer.get() == 0) ? 1 : -1;
        byte[] value = new byte[byteCount];
        for (int i = byteCount; i > 0; i--) {
            value[i - 1] = buffer.get();
        }

        BigInteger bigInteger = new BigInteger(signum, value);

        if (bigInteger.compareTo(MAX_LONG_VAL) == 1) return bigInteger;
        return bigInteger.longValue();
    }

    private Object decodeAtom(int tag) {
        int atomLength = 0;
        String atom = null;

        switch (tag) {
            case ATOM_EXT:
                atomLength = getUnsignedShort();
                break;
            case SMALL_ATOM_EXT:
                atomLength = getUnsignedByte();
                break;
        }

        if (atomLength > 0) {
            atom = decodeString(atomLength);
        }

        if (TextUtils.isEmpty(atom)) return null;

        if ("true".equalsIgnoreCase(atom)) return Boolean.TRUE;
        else if ("false".equalsIgnoreCase(atom)) return Boolean.FALSE;

        if (atomAsString) return atom;
        return new BertAtom(atom);
    }

    private String decodeString() {
        int byteCount = getUnsignedShort();
        return decodeString(byteCount);
    }

    private byte[] decodeBinary() {
        long byteCount = getUnsignedInt();
        byte[] data = new byte[(int) byteCount];
        buffer.get(data);
        return data;
    }

    private Object decodeList() throws InvalidObjectException {
        long numElements = getUnsignedInt();
        boolean canDecodeAsMap = propListsAsMap;

        ArrayList<Object> list = new ArrayList<>();

        for (int i = 0; i < numElements; i++) {
            Object decoded = decode();
            canDecodeAsMap =
                    canDecodeAsMap &&
                            decoded instanceof BertTuple && ((BertTuple) decoded).isKV();
            list.add(decoded);
        }

        buffer.mark();
        if (buffer.get() != NIL_EXT) buffer.reset();

        if (canDecodeAsMap) {
            Map<Object, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < list.size(); i++) {
                BertTuple tuple = (BertTuple) list.get(i);
                map.put(tuple.get(0), tuple.get(1));
            }

            return map;
        }
        return list;
    }

    private BertTuple decodeTuple(int tag) throws InvalidObjectException {
        long elements = 0;

        switch (tag) {
            case SMALL_TUPLE_EXT:
                elements = getUnsignedByte();
                break;
            case LARGE_TUPLE_EXT:
                elements = getUnsignedInt();
                break;
        }

        BertTuple tuple = new BertTuple();

        for (long i = 0; i < elements; i++) {
            tuple.add(decode());
        }

        return tuple;
    }

    private Map<Object, Object> decodeMap() throws InvalidObjectException {
        Map<Object, Object> map = new LinkedHashMap<>();
        long elements = getUnsignedInt();

        for (long i = 0; i < elements; i++) {
            Object key = decode();
            Object value = decode();

            if (key == null) continue;

            key = getMapKey(key);
            value = getMapValue(key, value);
            map.put(key, value);
        }

        return map;
    }

    private Object getMapKey(Object key) {
        if (!mapKeysAsString) return key;

        if (key instanceof String) return key;
        if (key instanceof BertAtom) return ((BertAtom) key).get();
        if (key instanceof byte[]) new String((byte[]) key, utf8Charset);
        return key.toString();
    }

    private Object getMapValue(Object key, Object value) {
        if (value == null) return null;

        if (shouldDecodeBinaryAsStringForKey(key)) {
            if (value instanceof byte[]) return new String((byte[]) value, utf8Charset);
            if (value instanceof String) return value;
            if (value instanceof ArrayList) {
                ArrayList<String> parsedList = new ArrayList<>();
                ArrayList list = (ArrayList) value;
                for (Object obj : list) {
                    if (obj instanceof byte[]) {
                        String parsed = new String((byte[]) obj, utf8Charset);
                        parsedList.add(parsed);
                    } else if (obj instanceof String) {
                        parsedList.add((String) obj);
                    } else {
                        parsedList.add(obj.toString());
                    }
                }

                return parsedList;
            }
            return value.toString();
        }
        return value;
    }

    private String decodeString(int byteCount) {
        int position = buffer.position();
        buffer.position(buffer.position() + byteCount);
        return new String(buffer.array(), position, byteCount);
    }

    private short getUnsignedByte() {
        return ((short) (buffer.get() & 0xff));
    }

    private int getUnsignedShort() {
        return (buffer.getShort() & 0xffff);
    }

    private long getUnsignedInt() {
        return ((long) buffer.getInt() & 0xffffffffL);
    }
}
