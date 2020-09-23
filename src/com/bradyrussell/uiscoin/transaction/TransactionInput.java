package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;

import java.nio.ByteBuffer;

public class TransactionInput  implements IBinaryData {
    public byte[] HashKey; // 64
    public int IndexNumber; // 4
    //public int SignatureScriptLength; // 4

    public byte[] ResponseScript;

    public int InputSequenceNumber; // 4 // not used, we can make this something else

    public TransactionInput() {
    }

    public TransactionInput(byte[] hashKey, int indexNumber, byte[] responseScript, int inputSequenceNumber) {
        HashKey = hashKey;
        IndexNumber = indexNumber;
        //SignatureScriptLength = signatureScriptLength;
        ResponseScript = responseScript;
        InputSequenceNumber = inputSequenceNumber;
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buf = ByteBuffer.allocate(getSize());

        buf.put(HashKey);
        buf.putInt(IndexNumber);
        buf.putInt(ResponseScript.length);
        buf.put(ResponseScript);
        buf.putInt(InputSequenceNumber);

        return buf.array();
    }

    @Override
    public void setBinaryData(byte[] Data) {
        ByteBuffer buffer = ByteBuffer.wrap(Data);

        HashKey = new byte[64];

        buffer.get(HashKey, 0, 64);
        IndexNumber = buffer.getInt();
        int SignatureScriptLength = buffer.getInt();
        ResponseScript = new byte[SignatureScriptLength];
        buffer.get(ResponseScript, 0, SignatureScriptLength);
        InputSequenceNumber = buffer.getInt();
    }

    @Override
    public int getSize() {
        return 64+4+4+4+ResponseScript.length;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
