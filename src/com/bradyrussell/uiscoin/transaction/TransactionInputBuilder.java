/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.transaction;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.List;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.script.ScriptBuilder;

public class TransactionInputBuilder {
    final TransactionInput input = new TransactionInput();

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
            Keys.SignedData signedData = Keys.signData(Keypair.Keys, outputToSpend.getHash());
            input.UnlockingScript = new ScriptBuilder(256).push(signedData.Signature).get();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

        return this;
    }

    //https://learnmeabitcoin.com/technical/p2pkh
    public TransactionInputBuilder setUnlockPayToPublicKeyHash(UISCoinKeypair Keypair, TransactionOutput outputToSpend) {
        try {
            Keys.SignedData signedData = Keys.signData(Keypair.Keys, outputToSpend.getHash());
            input.UnlockingScript = new ScriptBuilder(256).push(signedData.Signature).push(Keypair.Keys.getPublic().getEncoded()).get();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

        return this;
    }

    public TransactionInputBuilder setUnlockPayToPassword(String Password) {
        byte[] dataToPush = (Password + BytesUtil.getConstantSalt()).getBytes(Charset.defaultCharset());
        input.UnlockingScript = new ScriptBuilder(dataToPush.length+2).push(dataToPush).get();
        return this;
    }

    @Deprecated
    public TransactionInputBuilder setUnlockPayToMultiSig(List<UISCoinKeypair> keypairs, TransactionOutput outputToSpend) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        ScriptBuilder scriptBuilder = new ScriptBuilder(128+ keypairs.size()*128);

        for (UISCoinKeypair keypair : keypairs) {
            scriptBuilder.push(Keys.signData(keypair.Keys, outputToSpend.getHash()).Signature);
        }

        input.UnlockingScript = scriptBuilder.get();
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
