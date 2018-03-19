package com.softwarejoint.bert;

public interface DistributionHeader {

    byte MAGIC = (byte) 131;

    byte NIL_EXT = (byte) 106;

    int SMALL_INTEGER_EXT_MIN_VAL = 0;
    int SMALL_INTEGER_EXT_MAX_VAL = 255;
    byte SMALL_INTEGER_EXT = (byte) 97;            //DND: byte
    byte INTEGER_EXT = (byte) 98;

    int FLOAT_LENGTH = 31;
    byte FLOAT_EXT = (byte) 99;
    byte NEW_FLOAT_EXT = (byte) 70;

    byte SMALL_BIG_EXT = (byte) 110;
    byte LARGE_BIG_EXT = (byte) 111;

    byte ATOM_EXT = (byte) 100;                 //DND: max len: 255
    byte SMALL_ATOM_EXT = (byte) 115;

    int STRING_EXT_MAX_VAL = 65535;
    byte STRING_EXT = (byte) 107;               //DND: max size: 65535
    byte LIST_EXT = (byte) 108;

    byte BINARY_EXT = (byte) 109;

    byte SMALL_TUPLE_EXT = (byte) 104;
    byte LARGE_TUPLE_EXT = (byte) 105;

    byte MAP_EXT = (byte) 116;
}
