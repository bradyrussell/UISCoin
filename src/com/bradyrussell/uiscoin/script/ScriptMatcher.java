package com.bradyrussell.uiscoin.script;

import java.util.ArrayList;

public class ScriptMatcher {
    // null = 1 byte wildcard
    public ArrayList<ScriptOperator> scriptMatch = new ArrayList<>();
    public ArrayList<byte[]> PushContents = new ArrayList<>();

    public boolean match(byte[] Script) {
        PushContents.clear();
        int expected_i = 0;
        for (int i = 0; i < Script.length && i < scriptMatch.size(); i++) {
            ScriptOperator expected = scriptMatch.get(expected_i++);
            if(expected == null) continue;

            if (Script[i] == ScriptOperator.PUSH.OPCode && expected.equals(ScriptOperator.PUSH)) {
                byte amount = Script[++i];
                byte[] outBytes = new byte[amount];
                System.arraycopy(Script, ++i, outBytes,0,amount);
                PushContents.add(outBytes);
                i += amount;
                continue;
            }

            if(expected.OPCode != Script[i]) {
                System.out.println("Matcher expected "+expected+" found "+ScriptOperator.getByOpCode(Script[i])+" at script index "+i);
                return false;
            }
        }
        return true;
    }

    public byte[] getPushData(int Index) {
        return PushContents.get(Index);
    }

    // push 0: public key
    public static ScriptMatcher getMatcherP2PK() {
        return new ScriptMatcherBuilder()
                .push()
                .op(ScriptOperator.VERIFYSIG)
                .get();
    }

    // push 0: public key hash
    public static ScriptMatcher getMatcherP2PKH() {
        return new ScriptMatcherBuilder()
                .op(ScriptOperator.DUP) // dup the public key
                .op(ScriptOperator.SHA512) // hash it
                .push() // push the address
                .op(ScriptOperator.BYTESEQUAL) // equal to pubkey hash?
                .op(ScriptOperator.VERIFY)
                .op(ScriptOperator.VERIFYSIG)
                .get();
    }

    //push 0: double hashed known password
    public static ScriptMatcher getMatcherP2Password() {
        return new ScriptMatcherBuilder()
                .op(ScriptOperator.SHA512) // hash plaintext input password
                .op(ScriptOperator.SHA512) // double hash plaintext input password
                .push() // push the double hashed known Password
                .op(ScriptOperator.BYTESEQUAL) // equal to provided input password hash?
                .op(ScriptOperator.VERIFY)
                .get();
    }

    //push 0: hashed script
    public static ScriptMatcher getMatcherP2SH() {
        return new ScriptMatcherBuilder()
                .op(ScriptOperator.DUP) //          copy the script block for execution [uscript][script][script]
                .op(ScriptOperator.SHA512) //       hash script block [uscript][script][hash]
                .push() //                push the hashed script [uscript][script][hash][hash]
                .op(ScriptOperator.BYTESEQUAL) //   equal to provided input script? [uscript][script][1 / 0]
                .op(ScriptOperator.VERIFY) //[uscript][script]

                .op(ScriptOperator.SWAP) // [script][uscript]

                .op(ScriptOperator.FALSE)//.pushByte(0)  // [script][uscript][0]
                .op(ScriptOperator.SWAP)  // [script][0][uscript]

                .op(ScriptOperator.CALL) //  [script][results1][results2]..[1/0] // run unlocking script

                .op(ScriptOperator.VERIFY) // [script][results1][results2]..

                .op(ScriptOperator.SHIFTDOWN) // [results1][results2].. [script]
                // rotate

                .op(ScriptOperator.DEPTH) //        get results amount [results1][results2].. [script][>=1]
                .op(ScriptOperator.TRUE)//.pushByte(1) //                     [results1][results2].. [script][>=1][1]
                .op(ScriptOperator.SUBTRACTBYTES)// [results1][results2].. [script][>=0]
                .op(ScriptOperator.SWAP)//          [results1][results2].. [>=0][script]
                .op(ScriptOperator.CALL)// [resulting stack][1 / 0]
                .op(ScriptOperator.VERIFY)
                .get();
    }
}
