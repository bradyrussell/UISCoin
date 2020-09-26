package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.script.ScriptBuilder;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public class TransactionBuilder {
    Transaction transaction = new Transaction();

    public TransactionBuilder setVersion(int Version){
        transaction.Version = Version;
        return this;
    }

    public TransactionBuilder setLockTime(long LockTime){
        transaction.TimeStamp = LockTime;
        return this;
    }

    public TransactionBuilder addInput(TransactionInput transactionInput){
        transaction.addInput(transactionInput);
        return this;
    }

    public TransactionBuilder addOutput(TransactionOutput transactionOutput){
        transaction.addOutput(transactionOutput);
        return this;
    }

    public TransactionBuilder signTransaction(UISCoinKeypair keypair){

        for(TransactionInput input:transaction.Inputs){
            input.UnlockingScript = keypair.Keys.getPublic().getEncoded();//todo this is meant to be the scriptPubKey or Locking Script of the output to redeem
            // todo this means on input builder it needs to take that as a param that is retrieved from the blockchain  / utxo set
        }

        try {
            Keys.SignedData signedData = Keys.SignData(keypair.Keys, Hash.getSHA512Bytes(transaction.getBinaryData()));

            for(TransactionInput input:transaction.Inputs){
                input.UnlockingScript = new ScriptBuilder(256).push(signedData.Signature).push(signedData.Pubkey).get();//Util.ConcatArray(signedData.Signature, signedData.Pubkey);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

        return this;
    }

    public Transaction get() {
        return transaction;
    }
}
