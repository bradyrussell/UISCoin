package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.*;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.blockchain.storage.Blockchain;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import com.bradyrussell.uiscoin.script.exception.ScriptEmptyStackException;
import com.bradyrussell.uiscoin.script.exception.ScriptInvalidException;
import com.bradyrussell.uiscoin.script.exception.ScriptInvalidParameterException;
import com.bradyrussell.uiscoin.script.exception.ScriptUnsupportedOperationException;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.logging.Logger;

public class TransactionInput  implements IBinaryData, IVerifiable {
    private static final Logger Log = Logger.getLogger(TransactionInput.class.getName());
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
    public boolean verify() {
        if(UnlockingScript.length > MagicNumbers.MaxUnlockingScriptLength.Value) {
            Log.info("Verification failed! Unlocking script is too long! Maximum length is "+MagicNumbers.MaxUnlockingScriptLength.Value+" bytes.");
            return false;
        }

        Transaction transaction;
        try {
            transaction = Blockchain.get().getTransaction(InputHash);
            if(Instant.now().getEpochSecond() < transaction.TimeStamp) {
                Log.info("Verification failed! Input is locked until "+transaction.TimeStamp+" ("+(transaction.TimeStamp-Instant.now().getEpochSecond())+" seconds from now)!");
                return false;
            }
        } catch (NoSuchTransactionException | NoSuchBlockException e) {
            e.printStackTrace();
            Log.info("Verification failed! No input transaction!");
            return false;
        }

        TransactionOutput transactionOutput = transaction.Outputs.get(IndexNumber);

        ScriptExecution UnlockingScriptEx = new ScriptExecution();
        UnlockingScriptEx.initialize(UnlockingScript);

        try{
            while(UnlockingScriptEx.step());
        } catch (ScriptInvalidException | ScriptEmptyStackException | ScriptInvalidParameterException | ScriptUnsupportedOperationException e) {
            e.printStackTrace();
        }

        if(UnlockingScriptEx.bScriptFailed) {
            Log.info("Verification failed! Unlocking script failed!");
            return false;
        }

        ScriptExecution LockingScriptEx = new ScriptExecution();
        LockingScriptEx.setSignatureVerificationMessage(transactionOutput.getHash());
        LockingScriptEx.initialize(transactionOutput.LockingScript, UnlockingScriptEx.Stack.elements());

        try{
            while(LockingScriptEx.step());
        } catch (ScriptInvalidException | ScriptEmptyStackException | ScriptInvalidParameterException | ScriptUnsupportedOperationException e) {
            e.printStackTrace();
        }

        if(LockingScriptEx.bScriptFailed) Log.info("Verification failed! Locking script failed!");
        return !LockingScriptEx.bScriptFailed;
    }
}
