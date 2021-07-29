package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;
import com.bradyrussell.uiscoin.IVerifiable;
import com.bradyrussell.uiscoin.MagicNumbers;

import java.nio.ByteBuffer;

public class TransactionOutput  implements IBinaryData, IVerifiable {
    public long Amount;
    public byte[] LockingScript; // aka scriptPubkey

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
    public int setBinaryData(byte[] Data) {
        ByteBuffer buffer = ByteBuffer.wrap(Data);

        Amount = buffer.getLong();
        int LockingScriptLength = buffer.getInt();
        LockingScript = new byte[LockingScriptLength];
        buffer.get(LockingScript, 0, LockingScriptLength);
        return buffer.position();
    }

    @Override
    public int getSize() {
        return 12+LockingScript.length;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }

    @Override
    public boolean verify() {
        return LockingScript.length < MagicNumbers.MaxLockingScriptLength.Value && Amount > 0;
    }
}
