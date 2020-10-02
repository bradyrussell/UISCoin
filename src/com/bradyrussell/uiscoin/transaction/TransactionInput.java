package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.*;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.script.ScriptExecution;

import java.nio.ByteBuffer;

public class TransactionInput  implements IBinaryData, IVerifiable {
    public byte[] InputHash; // 64 // the UTXO hash // also txOutpoint??
    public int IndexNumber; // 4  // the UTXO index
    //public int SignatureScriptLength; // 4

    public byte[] UnlockingScript; // response script // aka scriptSig

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
        return 64+4+4+4+UnlockingScript.length;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }

    @Override
    public boolean Verify() {
        System.out.println("Verifying input "+ Util.Base64Encode(getHash()));

        if(UnlockingScript.length > MagicNumbers.MaxUnlockingScriptLength.Value) return false;

        //TransactionOutput unspentTransactionOutput = BlockChain.get().getUnspentTransactionOutput(InputHash, IndexNumber);
        TransactionOutput unspentTransactionOutput = BlockChain.get().getTransactionOutput(InputHash, IndexNumber);
        if(unspentTransactionOutput == null) {
            System.out.println("Verification failed! No UTXO");
            return false;
        }

        ScriptExecution UnlockingScriptEx = new ScriptExecution();
        UnlockingScriptEx.Initialize(UnlockingScript);

        while(UnlockingScriptEx.Step());

        if(UnlockingScriptEx.bScriptFailed) {
            System.out.println("Verification failed! Unlocking script failed!");
            return false;
        }

        ScriptExecution LockingScriptEx = new ScriptExecution();
        LockingScriptEx.setSignatureVerificationMessage(unspentTransactionOutput.getHash());
        LockingScriptEx.Initialize(unspentTransactionOutput.LockingScript, UnlockingScriptEx.Stack.elements());

        while(LockingScriptEx.Step());

        if(LockingScriptEx.bScriptFailed) System.out.println("Verification failed! Locking script failed!");
        return !LockingScriptEx.bScriptFailed;
    }
}
