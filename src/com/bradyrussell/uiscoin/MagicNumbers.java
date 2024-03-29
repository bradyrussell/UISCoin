/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin;

public enum MagicNumbers {
    NodeP2PPort(25920),
    NodeP2PTimeout(120),
    NodeP2PPingInterval(60),
    MaxTransactionSize(1024*5),
    MaxBlockSize(1024*1024*10),
    MinSatPerByte(1),
    MaxLockingScriptLength(256),
    MaxUnlockingScriptLength(256),
    TimeVarianceAllowedSeconds(30),
    MaximumUnzipLength(32768),

    TargetSecondsPerBlock(300), // difficulty will adjust to meet this
    ;

    public final int Value;

    MagicNumbers(int value) {
        Value = value;
    }
}
