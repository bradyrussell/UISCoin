package com.bradyrussell.uiscoin;

public enum MagicNumbers {
    NodeP2PPort(25920),
    NodeP2PTimeout(300)   ,
    ;

    public final int Value;

    MagicNumbers(int value) {
        Value = (byte)value;
    }
}
