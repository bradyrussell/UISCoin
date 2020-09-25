package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;

import java.nio.ByteBuffer;

public class TransactionInput  implements IBinaryData {
    public byte[] InputHash; // 64 // the UTXO hash // also txOutpoint??
    public int IndexNumber; // 4  // the UTXO index
    //public int SignatureScriptLength; // 4

    public byte[] UnlockingScript; // response script

    public int InputSequenceNumber; // 4 // not used, we can make this something else

    public TransactionInput() {
    }

    public TransactionInput(byte[] inputHash, int indexNumber) {
        InputHash = inputHash;
        IndexNumber = indexNumber;
        //SignatureScriptLength = signatureScriptLength;
        InputSequenceNumber = 0;
    }

    public TransactionInput(byte[] inputHash, int indexNumber, int inputSequenceNumber) {
        InputHash = inputHash;
        IndexNumber = indexNumber;
        //SignatureScriptLength = signatureScriptLength;
        InputSequenceNumber = inputSequenceNumber;
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buf = ByteBuffer.allocate(getSize());

        buf.put(InputHash);
        buf.putInt(IndexNumber);

        buf.putInt(UnlockingScript.length);
        buf.put(UnlockingScript);
        buf.putInt(InputSequenceNumber);

        return buf.array();
    }

    @Override
    public int setBinaryData(byte[] Data) {
        ByteBuffer buffer = ByteBuffer.wrap(Data);

        InputHash = new byte[64];

        buffer.get(InputHash, 0, 64);
        IndexNumber = buffer.getInt();
        int SignatureScriptLength = buffer.getInt();
        UnlockingScript = new byte[SignatureScriptLength];
        buffer.get(UnlockingScript, 0, SignatureScriptLength);
        InputSequenceNumber = buffer.getInt();
        return buffer.position();
    }

    @Override
    public int getSize() {
        return 64+4+4+4+ UnlockingScript.length;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
