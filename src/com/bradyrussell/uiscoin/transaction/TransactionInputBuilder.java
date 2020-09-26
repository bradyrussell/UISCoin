package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.script.ScriptBuilder;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public class TransactionInputBuilder {
    TransactionInput input = new TransactionInput();

    public TransactionInputBuilder setInputTransactionHash(byte[] Hash){
        input.InputHash = Hash;
        return this;
    }

    public TransactionInputBuilder setInputTransactionIndex(int Index){
        input.IndexNumber = Index;
        return this;
    }

    public TransactionInputBuilder setSequenceNumber(int Sequence){
        input.InputSequenceNumber = Sequence;
        return this;
    }

    public TransactionInputBuilder setUnlockingScript(byte[] UnlockingScript){
        input.UnlockingScript = UnlockingScript;
        return this;
    }

    public TransactionInput get(){
        return input;
    }

    // https://learnmeabitcoin.com/technical/p2pk
    public TransactionInputBuilder setUnlockPayToPublicKey(UISCoinKeypair Keypair, byte[] LockingScript){
        try {
            Keys.SignedData signedData = Keys.SignData(Keypair.Keys, LockingScript);
            input.UnlockingScript = new ScriptBuilder(256).push(signedData.Signature).get();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

        return this;
    }

    //https://learnmeabitcoin.com/technical/p2pkh
    public TransactionInputBuilder setUnlockPayToPublicKeyHash(UISCoinKeypair Keypair, byte[] LockingScript) {
        try {
            Keys.SignedData signedData = Keys.SignData(Keypair.Keys, LockingScript);
            input.UnlockingScript = new ScriptBuilder(256).push(signedData.Signature).push(Keypair.Keys.getPublic().getEncoded()).get();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

        return this;
    }
}
