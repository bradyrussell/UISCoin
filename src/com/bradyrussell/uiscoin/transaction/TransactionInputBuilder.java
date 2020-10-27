package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.script.ScriptBuilder;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.List;

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

    public TransactionInputBuilder setInputTransaction(byte[] Hash, int Index){
        input.InputHash = Hash;
        input.IndexNumber = Index;
        return this;
    }

    public TransactionInputBuilder setInputTransaction(TransactionOutput transactionOutput, int Index){
        input.InputHash = transactionOutput.getHash();
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
    public TransactionInputBuilder setUnlockPayToPublicKey(UISCoinKeypair Keypair, TransactionOutput outputToSpend){
        try {
            Keys.SignedData signedData = Keys.SignData(Keypair.Keys, outputToSpend.getHash());
            input.UnlockingScript = new ScriptBuilder(256).push(signedData.Signature).get();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

        return this;
    }

    //https://learnmeabitcoin.com/technical/p2pkh
    public TransactionInputBuilder setUnlockPayToPublicKeyHash(UISCoinKeypair Keypair, TransactionOutput outputToSpend) {
        try {
            Keys.SignedData signedData = Keys.SignData(Keypair.Keys, outputToSpend.getHash());
            input.UnlockingScript = new ScriptBuilder(256).push(signedData.Signature).push(Keypair.Keys.getPublic().getEncoded()).get();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

        return this;
    }

    public TransactionInputBuilder setUnlockPayToPassword(String Password) {
        byte[] dataToPush = (Password + Util.getConstantSalt()).getBytes(Charset.defaultCharset());
        input.UnlockingScript = new ScriptBuilder(dataToPush.length+2).push(dataToPush).get();
        return this;
    }

    public TransactionInputBuilder setUnlockPayToScriptHash(byte[] RedeemScript, byte[] RedeemUnlockScript) {
        ScriptBuilder unlockingScript = new ScriptBuilder(MagicNumbers.MaxUnlockingScriptLength.Value);

        unlockingScript.push(RedeemUnlockScript);
        unlockingScript.push(RedeemScript);

        input.UnlockingScript = unlockingScript.get();
        return this;
    }
    //
}
