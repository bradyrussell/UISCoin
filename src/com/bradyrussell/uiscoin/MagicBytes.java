package com.bradyrussell.uiscoin;

public enum MagicBytes {
    BlockHeader(0x01),
    AddressHeader(0x50),
    AddressType(0x84),
    AddressVersion(0xb1),

    ;

    public final byte Value;

    MagicBytes(int value) {
        Value = (byte)value;
    }
}
