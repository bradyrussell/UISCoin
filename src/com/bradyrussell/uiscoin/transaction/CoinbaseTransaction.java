package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;

import java.nio.ByteBuffer;

public class CoinbaseTransaction implements IBinaryData {
    byte[] HashKey; // 64
    int Index; //4
    //int CoinbaseScriptLength; // 4
    byte[] CoinbaseScript; // 64
    int InputSequenceNumber; //4

    public CoinbaseTransaction() {
    }

    public CoinbaseTransaction(byte[] hashKey, int index, byte[] coinbaseScript, int inputSequenceNumber) {
        HashKey = hashKey;
        Index = index;
        CoinbaseScript = coinbaseScript;
        InputSequenceNumber = inputSequenceNumber;
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buf = ByteBuffer.allocate(getSize());

        buf.put(HashKey);
        buf.putInt(Index);
        buf.putInt(CoinbaseScript.length);
        buf.put(CoinbaseScript);
        buf.putInt(InputSequenceNumber);

        return buf.array();
    }

    @Override
    public void setBinaryData(byte[] Data) {
        ByteBuffer buffer = ByteBuffer.wrap(Data);
        HashKey = new byte[64];

        buffer.get(HashKey, 0, 64);
        Index = buffer.getInt();
        int ScriptLength = buffer.getInt();
        CoinbaseScript = new byte[ScriptLength];
        buffer.get(CoinbaseScript);
        InputSequenceNumber = buffer.getInt();
    }

    @Override
    public int getSize() {
        return 64+4+4+64+4;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
