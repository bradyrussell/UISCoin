package com.bradyrussell.uiscoin;

public enum MagicBytes {
    BlockHeader(0x01),
    AddressHeader(0x50),
    AddressType(0x84),
    AddressVersion(0xb1),
    AddressVersion2(0xb2),
    AddressVersion3(0xb3),

    ;

    public final byte Value;

    MagicBytes(int value) {
        Value = (byte)value;
    }
}
