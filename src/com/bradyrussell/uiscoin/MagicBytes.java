package com.bradyrussell.uiscoin;

public enum MagicBytes {
    BlockHeader(0x01),
    AddressHeader(0x04),
    ;

    public final byte Value;

    MagicBytes(int value) {
        Value = (byte)value;
    }
}
