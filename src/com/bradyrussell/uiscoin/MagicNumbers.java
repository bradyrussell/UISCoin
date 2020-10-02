package com.bradyrussell.uiscoin;

public enum MagicNumbers {
    NodeP2PPort(25920),
    NodeP2PTimeout(300)   ,
    MaxTransactionSize(1024),
    MinSatPerByte(1),
    MaxLockingScriptLength(256),
    MaxUnlockingScriptLength(256),

    TargetSecondsPerBlock(300), // difficulty will adjust to meet this
    ;

    public final int Value;

    MagicNumbers(int value) {
        Value = value;
    }
}
