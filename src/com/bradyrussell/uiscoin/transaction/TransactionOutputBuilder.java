package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptOperator;

public class TransactionOutputBuilder {
    TransactionOutput output = new TransactionOutput();

    public TransactionOutputBuilder setAmount(long Amount){
        output.Amount = Amount;
        return this;
    }

    // https://learnmeabitcoin.com/technical/p2pk
    public TransactionOutputBuilder setPayToPublicKey(byte[] PublicKey){
        output.LockingScript = new ScriptBuilder(4+PublicKey.length).push(PublicKey).op(ScriptOperator.VERIFYSIG).get();
        return this;
    }

    // https://learnmeabitcoin.com/technical/p2pkh
    public TransactionOutputBuilder setPayToPublicKeyHash(byte[] PublicKeyHash){
        output.LockingScript = new ScriptBuilder(128)
                .op(ScriptOperator.DUP) // dup the public key
                .op(ScriptOperator.SHA512) // hash it
                .push(PublicKeyHash) // push the address
                .op(ScriptOperator.BYTESEQUAL) // equal to pubkey hash?
                .op(ScriptOperator.VERIFY)
                .op(ScriptOperator.VERIFYSIG)
                .get();
        return this;
    }

    // https://learnmeabitcoin.com/technical/p2pkh
    public TransactionOutputBuilder setPayToPublicKeyHashWithChecksum(byte[] PublicKeyHash){
        output.LockingScript = new ScriptBuilder(128)
                .op(ScriptOperator.DUP) // dup the public key
                .op(ScriptOperator.SHA512) // hash it
                .push(PublicKeyHash) // push the address
                .op(ScriptOperator.LEN) // take its length
                .pushInt(4) // push 4
                .op(ScriptOperator.SWAP) // make length the top stack element, then 4
                .op(ScriptOperator.SUBTRACT) // do length - 4
                .op(ScriptOperator.LIMIT) // limit the address to length - 4 (remove checksum)
                .op(ScriptOperator.BYTESEQUAL) // equal to pubkey hash?
                .op(ScriptOperator.VERIFY)
                .op(ScriptOperator.VERIFYSIG)
                .get();

        //new ScriptBuilder(128).fromText("dup sha512").push(PublicKeyHash).fromText("len push 4 swap subtract limit bytesequal verify verifysig").get();
        return this;
    }

    public TransactionOutput get(){
        return output;
    }
}
