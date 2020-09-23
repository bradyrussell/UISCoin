package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;

import java.nio.ByteBuffer;

public class TransactionOutput  implements IBinaryData {
    public long Amount;
    public byte[] LockingScript;

    public TransactionOutput() {
    }

    public TransactionOutput(long amount, byte[] lockingScript) {
        Amount = amount;
        LockingScript = lockingScript;
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buffer = ByteBuffer.allocate(getSize());
        buffer.putLong(Amount);
        buffer.putInt(LockingScript.length);
        buffer.put(LockingScript);
        return buffer.array();
    }

    @Override
    public void setBinaryData(byte[] Data) {
        ByteBuffer buffer = ByteBuffer.wrap(Data);

        Amount = buffer.getLong();
        int LockingScriptLength = buffer.getInt();
        LockingScript = new byte[LockingScriptLength];
        buffer.get(LockingScript, 0, LockingScriptLength);
    }

    @Override
    public int getSize() {
        return 12+LockingScript.length;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
