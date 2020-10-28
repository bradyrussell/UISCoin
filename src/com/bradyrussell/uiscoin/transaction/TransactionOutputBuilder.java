package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptOperator;

import java.util.Arrays;

public class TransactionOutputBuilder {
    TransactionOutput output = new TransactionOutput();

    public TransactionOutputBuilder setAmount(long Amount){
        output.Amount = Amount;
        return this;
    }

    /**
     * Helper function decides what type of address is provided and writes the appropriate script.
     * @param Address The full UISCoin address to pay to, including headers and checksum.
     * @return
     */
    public TransactionOutputBuilder setPayToAddress(byte[] Address){
        UISCoinAddress.DecodedAddress decodedAddress = UISCoinAddress.decodeAddress(Address);

        if(!UISCoinAddress.verifyAddressChecksum(Address)) throw new IllegalStateException("Checksum validation failed: " + Arrays.toString(Address));

        MagicBytes type = null;
        for (MagicBytes value : MagicBytes.values()) {
            if(decodedAddress.Type == value.Value) {
                type = value;
                break;
            }
        }

        if(type == null) throw new IllegalStateException("Unexpected value: " + decodedAddress.Type);

        switch (type){
            case AddressVersion -> {
                return setPayToPublicKeyHash(decodedAddress.HashData);
            }

            case AddressVersionP2SH -> {
                return setPayToScriptHash(decodedAddress.HashData);
            }

            default -> throw new IllegalStateException("Unexpected value: " + type);
        }

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

    public TransactionOutputBuilder setPayToPassword(String Password){
        output.LockingScript = new ScriptBuilder(128)
                .op(ScriptOperator.SHA512) // hash plaintext input password
                .op(ScriptOperator.SHA512) // double hash plaintext input password
                .push(Hash.getSHA512Bytes(Hash.getSHA512Bytes(Password + Util.getConstantSalt()))) // push the double hashed known Password
                .op(ScriptOperator.BYTESEQUAL) // equal to provided input password hash?
                .op(ScriptOperator.VERIFY)
                .get();
        return this;
    }

    public TransactionOutputBuilder setPayToScriptHash(byte[] ScriptHash){
        output.LockingScript = new ScriptBuilder(128)
                .op(ScriptOperator.DUP) //          copy the script block for execution [uscript][script][script]
                .op(ScriptOperator.SHA512) //       hash script block [uscript][script][hash]
                .push(ScriptHash) //                push the hashed script [uscript][script][hash][hash]
                .op(ScriptOperator.BYTESEQUAL) //   equal to provided input script? [uscript][script][1 / 0]
                .op(ScriptOperator.VERIFY) //[uscript][script]

                .op(ScriptOperator.SWAP) // [script][uscript]

                .pushByte(0)  // [script][uscript][0]
                .op(ScriptOperator.SWAP)  // [script][0][uscript]

                .op(ScriptOperator.VIRTUALSCRIPT) //  [script][results1][results2]..[1/0] // run unlocking script

                .op(ScriptOperator.VERIFY) // [script][results1][results2]..

                .op(ScriptOperator.SHIFTDOWN) // [results1][results2].. [script]
                // rotate

                .op(ScriptOperator.DEPTH) //        get results amount [results1][results2].. [script][>=1]
                .pushByte(1) //                     [results1][results2].. [script][>=1][1]
                .op(ScriptOperator.SUBTRACTBYTES)// [results1][results2].. [script][>=0]
                .op(ScriptOperator.SWAP)//          [results1][results2].. [>=0][script]
                .op(ScriptOperator.VIRTUALSCRIPT)// [resulting stack][1 / 0]
                .op(ScriptOperator.VERIFY)
                .get();
        return this;
    }

    public TransactionOutput get(){
        return output;
    }
}
