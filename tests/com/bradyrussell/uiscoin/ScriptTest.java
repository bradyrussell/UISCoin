/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.bradyrussell.uiscoin.*;
import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.script.*;
import com.bradyrussell.uiscoin.script.exception.ScriptEmptyStackException;
import com.bradyrussell.uiscoin.script.exception.ScriptInvalidException;
import com.bradyrussell.uiscoin.script.exception.ScriptInvalidParameterException;
import com.bradyrussell.uiscoin.script.exception.ScriptUnsupportedOperationException;
import com.bradyrussell.uiscoin.transaction.TransactionInputBuilder;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class ScriptTest {
    @Test
    @DisplayName("Script No Duplicate OpCode Values")
    void TestScriptNoDupOps() {
        ArrayList<Byte> values = new ArrayList<>();
        for (ScriptOperator value : ScriptOperator.values()) {
            if(values.contains(value.OPCode)) {
                System.out.println("Duplicate opcode values: ");
                BytesUtil.printBytesHex(new byte[]{value.OPCode});
                fail();
            }
            values.add(value.OPCode);
        }
    }

    @Test
    @DisplayName("Script Unused Operators")
    void TestScriptUnusedOps() {
        ArrayList<Byte> values = new ArrayList<>();
        for (ScriptOperator value : ScriptOperator.values()) {
            if(values.contains(value.OPCode)) {
                System.out.println("Duplicate opcode values: ");
                BytesUtil.printBytesHex(new byte[]{value.OPCode});
                fail();
            }
            values.add(value.OPCode);
        }

        ArrayList<Byte> unusedValues = new ArrayList<>();
        for(int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++){
            if(!values.contains((byte)i)) unusedValues.add((byte)i);
        }

        System.out.println("Unused opcode values: ");
        for (Byte unusedValue : unusedValues) {
            BytesUtil.printBytesHex(new byte[]{unusedValue});
        }

        System.out.println("There are "+values.size()+" used OPCode values.");
        System.out.println("There are "+unusedValues.size()+" unused OPCode values.");
        assertTrue(true);
    }

    @Test
    @DisplayName("Script Append")
    void TestScriptAppend() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb
                .pushASCIIString("Hello, ")
                .pushASCIIString("world!")
                .op(ScriptOperator.APPEND)
                .op(ScriptOperator.SHA512)
                .pushASCIIString("Hello, world!")
                .op(ScriptOperator.SHA512EQUAL)
                //.op(ScriptOperator.SHA512)
                //.op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script DupN")
    void TestScriptDUPN() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb
                .fromText("push 0 push 1 push 2 push 3 push 4 push 0x04 dupn");

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Boolean Logic")
    void TestScriptBoolLogic() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb.fromText("true true true false true false true true false false false true and or or and xor xor xor xor xor xor xor verify");
        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Shift Array")
    void TestScriptRotate() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb.fromText("push [1, 2, 3, 4] shiftelementsright push [4, 1, 2, 3] bytesequal verify");
        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Shift Array Back")
    void TestScriptRotate2() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb.fromText("push [4, 1, 2, 3] shiftelementsleft push [1, 2, 3, 4] bytesequal verify");
        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Zip Unzip")
    void TestScriptZip() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb.fromText("push [1, 2, 3, 4] sha512 zip push [1, 2, 3, 4] sha512 swap unzip bytesequal verify");
        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Shift N Except")
    void TestScriptShiftNExcept() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb.fromText("push 0x01020304050607080900 split shiftnexcept([-1], 0x04)");
        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Time")
    void TestScriptTime() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException {
/*        ScriptBuilder sb = new ScriptBuilder(64);
        sb
                .pushASCIIString("garbage")
                .pushInt64(1)
                .pushByte(1)
                .op(ScriptOperator.CONVERT8TO32)
                .op(ScriptOperator.CONVERT32TO64)
                .op(ScriptOperator.BYTESEQUAL)
                .pushInt64(Instant.now().getEpochSecond())
                .op(ScriptOperator.TIME)
                .op(ScriptOperator.GREATERTHANEQUAL)
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.Initialize(sb.get());

        while (true){
            try {
                if (!scriptExecution.Step()) break;
            } catch (ScriptUnsupportedOperationException e) {
                e.printStackTrace();
            }
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertTrue(scriptExecution.bScriptFailed);*/
    }

    @Test
    @DisplayName("Script Get/Set")
    void TestScriptGetSet() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] C = new byte[16];
        ThreadLocalRandom.current().nextBytes(C);

        byte[] B = new byte[8];

        System.arraycopy(C,8,B,0,8);

        ScriptBuilder sb = new ScriptBuilder(2048);
        sb
                .pushASCIIString("garbage")
                .push(C)
                .pushInt(1) // stack element index
                .pushInt(8) // begin index
                .pushInt(8) // length
                .op(ScriptOperator.GET)
                .push(B)
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY)
                .pushInt(1) // stack element index
                .pushInt(0) // begin index
                .pushInt(8) // length
                .op(ScriptOperator.GET) // source
                .pushInt(1) // stack element index
                .pushInt(8) // begin index
                .pushInt(8) // length
                .op(ScriptOperator.SET) // source
                .pushInt(8)
                .op(ScriptOperator.SPLIT);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Copy")
    void TestScriptCopy() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] C = new byte[16];
        ThreadLocalRandom.current().nextBytes(C);

        byte[] B = new byte[16];
        ThreadLocalRandom.current().nextBytes(B);

        ScriptBuilder sb = new ScriptBuilder(2048);
        sb
                .push(C)
                .push(B)
                .pushInt(0)
                .pushInt(0)
                .pushInt(1)
                .pushInt(0)
                .pushInt(16)
                .op(ScriptOperator.COPY)
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script This")
    void TestScriptThis() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] C = new byte[2048];
        ThreadLocalRandom.current().nextBytes(C);

        ScriptBuilder sb = new ScriptBuilder(4098);
        sb
                .push(C)
                .pushInt64(1)
                .pushByte(1)
                .op(ScriptOperator.THIS);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.bExtendedFlowControl = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }


    @Test
    @DisplayName("Script BigPush")
    void TestScriptBigPush() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] C = new byte[2048];
        ThreadLocalRandom.current().nextBytes(C);

        ScriptBuilder sb = new ScriptBuilder(4098);
        sb
                .push(C)
                .pushInt64(1)
                .pushByte(1)
                .op(ScriptOperator.CONVERT8TO32)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(1)
    @DisplayName("Script Tokenize String")
    void TestTokenize() {
        ArrayList<String> strings = ScriptParser.GetTokensFromString("$a = 0; $b = 100; $c = $a + $b; $d[2] = $e[$c]\n", true);
        for (int i = 0; i < strings.size(); i++) {
            String string = strings.get(i);
            System.out.println(i+": "+string);
        }
    }

    @RepeatedTest(1)
    @DisplayName("Script Tokenize String 2")
    void TestTokenize2() {
        ArrayList<String> strings = ScriptParser.GetTokensFromString("//Decompiled from gYGQkpcD_wEEP4AAAAEBAQgBBD8AAAABAQIIAQEBBwEBAgdkAQRBIAAAZC4BBAAAAAUQoAEEAAAAAQEEAAAAAgEEAAAAAwEEAAAABAEBAQEBAAcBAQIknwEBAQEBAAcBAQIknwP_mAEBAhGgkgEBAhGgng==#\n" +
                "NULL\n" +
                "NULL\n" +
                "NULL\n" +
                "DEPTH\n" +
                "DUP\n" +
                "SHIFTUP\n" +
                "FLAG 0xFF\n" +
                "PUSH [63, -128, 0, 0]\n" +
                "PUSH [1]\n" +
                "REPLACE\n" +
                "PUSH [63, 0, 0, 0]\n" +
                "PUSH [2]\n" +
                "REPLACE\n" +
                "PUSH [1]\n" +
                "PICK\n" +
                "PUSH [2]\n" +
                "PICK\n" +
                "MULTIPLYFLOAT\n" +
                "PUSH [65, 32, 0, 0]\n" +
                "MULTIPLYFLOAT\n" +
                "CONVERTFLOATTO32\n" +
                "PUSH [0, 0, 0, 5]\n" +
                "NUMEQUAL\n" +
                "VERIFY\n" +
                "PUSH [0, 0, 0, 1]\n" +
                "PUSH [0, 0, 0, 2]\n" +
                "PUSH [0, 0, 0, 3]\n" +
                "PUSH [0, 0, 0, 4]\n" +
                "PUSH [1]\n" +
                "PUSH [0]\n" +
                "PICK\n" +
                "PUSH [2]\n" +
                "ADDBYTES\n" +
                "SHIFTNEXCEPT\n" +
                "PUSH [1]\n" +
                "PUSH [0]\n" +
                "PICK\n" +
                "PUSH [2]\n" +
                "ADDBYTES\n" +
                "SHIFTNEXCEPT\n" +
                "FLAG 0xFF\n" +
                "flip // puts var stack at top\n" +
                "dup // copy var count\n" +
                "push [-1] // shift amt\n" +
                "swap\n" +
                "addbytes([4]) // add stack cookies plus the dup and shift amt\n" +
                "depth \n" +
                "swap\n" +
                "subtractbytes // gives us the number of stack elems besides vars\n" +
                "shiftnexcept\n" +
                "//SHIFTDOWN\n" +
                "PUSH [3]\n" +
                "BYTESEQUAL\n" +
                "VERIFY\n" +
                "DUP\n" +
                "PUSH [3]\n" +
                "BYTESEQUAL\n" +
                "VERIFY\n" +
                "DROPN\n\n", false);
        for (int i = 0; i < strings.size(); i++) {
            String string = strings.get(i);
            System.out.println(i+": "+string);
        }
    }

    @RepeatedTest(500)
    @DisplayName("Invalid Script Terminates Cleanly")
    void TestInvalidScript() {
        ScriptExecution scriptExecution = new ScriptExecution();

        try {
            byte[] A = new byte[ThreadLocalRandom.current().nextInt(16, 33)];
            byte[] B = new byte[ThreadLocalRandom.current().nextInt(2048, 2049)];
            byte[] C = new byte[ThreadLocalRandom.current().nextInt(2048, 2049)];
            ThreadLocalRandom.current().nextBytes(A);
            ThreadLocalRandom.current().nextBytes(B);
            ThreadLocalRandom.current().nextBytes(C);


            scriptExecution.LogScriptExecution = true;

            ArrayList<byte[]> fakeStack = new ArrayList<>();
            fakeStack.add(B);
            fakeStack.add(C);
            //fakeStack.add(A);

            scriptExecution.initialize(A, Collections.enumeration(fakeStack));

            while (scriptExecution.step()) {
                System.out.println("Stack: \n"+scriptExecution.getStackContents());
            }

            System.out.println("Script returned: " + !scriptExecution.bScriptFailed);

            System.out.println("Finished: " + scriptExecution.InstructionCounter + " / " + scriptExecution.Script.length);
            //assertTrue(scriptExecution.bScriptFailed);
        } catch (ScriptInvalidException | ScriptInvalidParameterException | ScriptEmptyStackException | ScriptUnsupportedOperationException e) {
            e.printStackTrace();
            assertTrue(scriptExecution.bScriptFailed);
        } catch (Exception e){
            e.printStackTrace();
            fail();
        }

    }

    @RepeatedTest(100)
    @DisplayName("Script Call")
    void TestCall() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] A = new byte[ThreadLocalRandom.current().nextInt(16,127)];
        byte[] B = new byte[ThreadLocalRandom.current().nextInt(16,127)];

        ThreadLocalRandom.current().nextBytes(A);
        ThreadLocalRandom.current().nextBytes(B);

        ScriptBuilder virtualSB = new ScriptBuilder(4096);
        virtualSB
                .push(B)
                .push(A)
                .op(ScriptOperator.ENCRYPTAES)
                .push(B)
                .op(ScriptOperator.SWAP)
                .op(ScriptOperator.DECRYPTAES)
                .push(A)
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println("Virtual script: "+Arrays.toString(virtualSB.get()));

        ScriptBuilder sb = new ScriptBuilder(1000);

        sb
                .flag((byte)1)
                .flagData(Hash.getSHA512Bytes("flag"))
                .call(virtualSB.get());

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script call Pass In Stack")
    void TestcallInStack() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] A = new byte[ThreadLocalRandom.current().nextInt(16,127)];
        byte[] B = new byte[ThreadLocalRandom.current().nextInt(16,127)];

        ThreadLocalRandom.current().nextBytes(A);
        ThreadLocalRandom.current().nextBytes(B);

        ScriptBuilder virtualSB = new ScriptBuilder(4096);
        virtualSB
                .op(ScriptOperator.ENCRYPTAES)
                .push(B)
                .op(ScriptOperator.SWAP)
                .op(ScriptOperator.DECRYPTAES)
                .push(A)
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println("Virtual script: "+Arrays.toString(virtualSB.get()));

        ScriptBuilder sb = new ScriptBuilder(1000);

        sb
                .push(B)
                .push(A)
                .pushByte(2)
                .push(virtualSB.get())
                .op(ScriptOperator.CALL);

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(10)
    @DisplayName("Script Tokenizer and fromText Parity")
    void TestTokenizerAndfromTextParity(){
        int bufferLength = 1024;
        ScriptBuilder sb = new ScriptBuilder(bufferLength);
        String OriginalString = "push 'Hello world.'\n" +
                "len\n" +
                "convert32to8\n" +
                "push 0x06\n" +
                "swap\n" +
                "//Begin Loaded script as function\n" +
                "(3) {//function substring(string, startIndex, endIndex)#\n" +
                "shiftup\n" +
                "shiftup\n" +
                "swap\n" +
                "limit\n" +
                "reverse\n" +
                "len\n" +
                "convert32to8\n" +
                "shiftdown\n" +
                "subtractbytes\n" +
                "limit\n" +
                "reverse }\n" +
                "//#End Loaded script as function#\n" +
                "call\n" +
                "verify";
        sb.fromText(OriginalString);

        byte[] Original = sb.get();
        byte[] Upgrade = ScriptParser.CompileScriptTokensToBytecode(ScriptParser.GetTokensFromString(OriginalString.replace("#", "//"), true));

        assertTrue(Arrays.equals(Original, Upgrade));

        ScriptBuilder sb2 = new ScriptBuilder(bufferLength);

        String decompiledUpgrade = sb2.data(Upgrade).toText();

        sb2 = new ScriptBuilder(bufferLength).fromText(decompiledUpgrade);

        assertTrue(Arrays.equals(Original, sb2.get()));
    }

    @RepeatedTest(100)
    @DisplayName("Script call Pass Out Stack")
    void TestcallOutStack() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] A = new byte[ThreadLocalRandom.current().nextInt(16,127)];
        byte[] B = new byte[ThreadLocalRandom.current().nextInt(16,127)];

        ThreadLocalRandom.current().nextBytes(A);
        ThreadLocalRandom.current().nextBytes(B);

        ScriptBuilder virtualSB = new ScriptBuilder(4096);
        virtualSB
                .push(B)
                .push(A)
                .op(ScriptOperator.ENCRYPTAES)
                .push(B)
                .op(ScriptOperator.SWAP)
                .op(ScriptOperator.DECRYPTAES)
                .pushInt64(10);

        System.out.println("Virtual script: "+Arrays.toString(virtualSB.get()));

        ScriptBuilder sb = new ScriptBuilder(1000);

        sb
                .pushByte(0)
                .push(virtualSB.get())
                .op(ScriptOperator.CALL)
                .op(ScriptOperator.DROP)
                .op(ScriptOperator.DROP)
                .push(A)
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script call Pass In/Out Stack")
    void TestcallInOutStack() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] A = new byte[ThreadLocalRandom.current().nextInt(16,127)];

        ThreadLocalRandom.current().nextBytes(A);

        ScriptBuilder virtualSB = new ScriptBuilder(4096);
        virtualSB
                .op(ScriptOperator.DEPTH)
                .op(ScriptOperator.COMBINE);

        System.out.println("Virtual script: "+Arrays.toString(virtualSB.get()));

        ScriptBuilder sb = new ScriptBuilder(1000);

        sb
                .push(A)
                .push(A)
                .op(ScriptOperator.SPLIT)
                .op(ScriptOperator.DEPTH)
                .pushByte(1)
                .op(ScriptOperator.SUBTRACTBYTES)
                .push(virtualSB.get())
                .op(ScriptOperator.CALL)
                .op(ScriptOperator.DROP)
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Enc/Dec AES")
    void TestScriptAES() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] A = new byte[ThreadLocalRandom.current().nextInt(16,127)];
        byte[] B = new byte[ThreadLocalRandom.current().nextInt(16,127)];

        ThreadLocalRandom.current().nextBytes(A);
        ThreadLocalRandom.current().nextBytes(B);

        ScriptBuilder sb = new ScriptBuilder(4096);
        sb
                .push(B)
                .push(A)
                .op(ScriptOperator.ENCRYPTAES)
                .push(B)
                .op(ScriptOperator.SWAP)
                .op(ScriptOperator.DECRYPTAES)
                .push(A)
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script AES Parity")
    void TestScriptAESParity() throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] A = new byte[ThreadLocalRandom.current().nextInt(16,64)];
        byte[] B = new byte[ThreadLocalRandom.current().nextInt(16,64)];

        ThreadLocalRandom.current().nextBytes(A);
        ThreadLocalRandom.current().nextBytes(B);

        ScriptBuilder sb = new ScriptBuilder(4096);
        sb
                .push(B)
                .push(A)
                .op(ScriptOperator.ENCRYPTAES)
                .push(Encryption.encrypt(A,B))
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Addition")
    void TestScriptAddition() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        int A = ThreadLocalRandom.current().nextInt();
        int B = ThreadLocalRandom.current().nextInt();
        int C = A + B;

        ScriptBuilder sb = new ScriptBuilder(32);
        sb
                .pushInt(A)
                .pushInt(B)
                .op(ScriptOperator.ADD)
                .pushInt(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Subtraction")
    void TestScriptSubtraction() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        int A = ThreadLocalRandom.current().nextInt();
        int B = ThreadLocalRandom.current().nextInt();
        int C = A - B;

        ScriptBuilder sb = new ScriptBuilder(32);
                sb
                .pushInt(A)
                .pushInt(B)
                .op(ScriptOperator.SUBTRACT)
                .pushInt(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Multiplication")
    void TestScriptMultiplication() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        int A = ThreadLocalRandom.current().nextInt();
        int B = ThreadLocalRandom.current().nextInt();
        int C = A * B;

        ScriptBuilder sb = new ScriptBuilder(32);
        sb
                .pushInt(A)
                .pushInt(B)
                .op(ScriptOperator.MULTIPLY)
                .pushInt(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Division")
    void TestScriptDivision() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        int A = ThreadLocalRandom.current().nextInt();
        int B = ThreadLocalRandom.current().nextInt();
        int C = A / B;

        ScriptBuilder sb = new ScriptBuilder(32);
        sb
                .pushInt(A)
                .pushInt(B)
                .op(ScriptOperator.DIVIDE)
                .pushInt(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script SHA512")
    void TestScriptSHA512() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] A = new byte[64];
        byte[] B = new byte[64];

        ThreadLocalRandom.current().nextBytes(A);
        ThreadLocalRandom.current().nextBytes(B);

        A = Hash.getSHA512Bytes(A);

        ScriptBuilder sb = new ScriptBuilder(256);
        sb.push(A).push(B).fromText("sha512 swap sha512 lenequal verify");

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test @DisplayName("Script Builder & Text Parser Parity2")
    void TestBuilderAndTextParity2(){
        byte[] A = new byte[64];

        ThreadLocalRandom.current().nextBytes(A);

        byte[] a  = new ScriptBuilder(128)
                .flag((byte)2)
                .flagData(BytesUtil.numberToByteArray32(1234))
                .get();

        byte[] b= new ScriptBuilder(128).fromText("flag 2 flagdata 1234").get();

        BytesUtil.printBytesReadable(a);
        BytesUtil.printBytesReadable(b);

        assertTrue(Arrays.equals(a,b));
    }

    @Test @DisplayName("Script Code Block")
    void TestScriptCodeBlock() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        ScriptBuilder sb = new ScriptBuilder(128).fromText("push 0x00 push {push 0x01020304 return} call drop push 0x01020304 bytesequal verify");

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test @DisplayName("Script Code If Block")
    void TestScriptCodeIfBlock() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        ScriptBuilder sb = new ScriptBuilder(128).fromText("push {push 'Hello world!' true push 0x02 push {verify sha512} call drop push 'Hello world!' sha512 bytesequal verify}");

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test @DisplayName("Script Text Parser Push 64 Bit Integer")
    void TestText64Bit(){
        long A = ThreadLocalRandom.current().nextLong(((long)(Integer.MAX_VALUE))+1, Long.MAX_VALUE);

        byte[] a  = new ScriptBuilder(128)
                .pushInt64(A)
                .get();

        byte[] b= new ScriptBuilder(128).fromText("push "+A).get();

        BytesUtil.printBytesReadable(a);
        BytesUtil.printBytesReadable(b);

        assertTrue(Arrays.equals(a,b));
    }

    @RepeatedTest(10) @DisplayName("Script Text Comment Syntax")
    void TestTextCommentSyntax() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] a  = new ScriptBuilder(128).fromText("//line comment\n/* multiline\ncomment\n*/add(1, 2) add(5) ").get();

        byte[] b= new ScriptBuilder(128).fromText(new ScriptBuilder(128).data(a).toText()).get();

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(a);

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);

        System.out.println();

        BytesUtil.printBytesReadable(a);
        BytesUtil.printBytesReadable(b);

        assertTrue(Arrays.equals(a,b));
    }

    @RepeatedTest(10) @DisplayName("Script Text Parameter Syntax")
    void TestTextParameterSyntax() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] a  = new ScriptBuilder(128).fromText("add(1, 2) add(5)").get();

        byte[] b= new ScriptBuilder(128).fromText(new ScriptBuilder(128).data(a).toText()).get();

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(a);

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);

        System.out.println();

        BytesUtil.printBytesReadable(a);
        BytesUtil.printBytesReadable(b);

        assertTrue(Arrays.equals(a,b));
    }

    @RepeatedTest(10) @DisplayName("Script Text Function Syntax")
    void TestTextFxnSyntax() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        byte[] a  = new ScriptBuilder(128).fromText("push 2 push 1 (*) {push 3 add} call swap push 4 numequal verify verify").get();

        byte[] b= new ScriptBuilder(128).fromText(new ScriptBuilder(128).data(a).toText()).get();

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(a);

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);

        System.out.println();

        BytesUtil.printBytesReadable(a);
        BytesUtil.printBytesReadable(b);

        assertTrue(Arrays.equals(a,b));
    }

    @Test @DisplayName("Script Builder & Text Parser Parity")
    void TestBuilderAndTextParity(){
        byte[] A = new byte[64];

        ThreadLocalRandom.current().nextBytes(A);

        byte[] a  = new ScriptBuilder(128)
                //.flag((byte)2)
                //.flagData(Util.NumberToByteArray(123))
                .op(ScriptOperator.DUP) // dup the public key
                .op(ScriptOperator.SHA512) // hash it
                .push(A) // push the address
                .op(ScriptOperator.LEN) // take its length
                .pushInt(4) // push 4
                .op(ScriptOperator.SWAP) // make length the top stack element, then 4
                .op(ScriptOperator.SUBTRACT) // do length - 4
                .op(ScriptOperator.LIMIT) // limit the address to length - 4 (remove checksum)
                .op(ScriptOperator.BYTESEQUAL) // equal to pubkey hash?
                .op(ScriptOperator.VERIFY)
                .op(ScriptOperator.VERIFYSIG)
                .get();

        byte[] b= new ScriptBuilder(128).fromText(/*"flag 2 flagdata 123*/ "dup sha512").push(A).fromText("len push 4 swap subtract limit bytesequal verify verifysig").get();

        BytesUtil.printBytesReadable(a);
        BytesUtil.printBytesReadable(b);

        assertTrue(Arrays.equals(a,b));
    }

    @RepeatedTest(100) @DisplayName("Pay to Public Key Lock & Unlock")
    void TestPayToPublicKey() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        UISCoinKeypair coinKeypairRecipient = UISCoinKeypair.create();

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.coinsToSatoshis(1.0)).setPayToPublicKey(coinKeypairRecipient.Keys.getPublic().getEncoded()).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToPublicKey(coinKeypairRecipient, transactionOutput).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.step()) {
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.setSignatureVerificationMessage(transactionOutput.getHash());

        System.out.println("Running locking script...");

        while(lockingScript.step()) {
            System.out.println("Stack: \n"+lockingScript.getStackContents());
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }

    @Test @DisplayName("Pay to Public Key Hash Lock & Unlock")
    void TestPayToPublicKeyHash() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        UISCoinKeypair coinKeypairRecipient = UISCoinKeypair.create();

        byte[] addressv1 = UISCoinAddress.fromPublicKey((ECPublicKey) coinKeypairRecipient.Keys.getPublic());

        UISCoinAddress.DecodedAddress decodedAddress = UISCoinAddress.decodeAddress(addressv1);

        assertTrue(UISCoinAddress.verifyAddressChecksum(addressv1));

        byte[] pubkey_sha512Bytes = Hash.getSHA512Bytes(coinKeypairRecipient.Keys.getPublic().getEncoded());

        BytesUtil.printBytesReadable(decodedAddress.HashData);
        BytesUtil.printBytesReadable(pubkey_sha512Bytes);
        assertTrue(Arrays.equals(decodedAddress.HashData, pubkey_sha512Bytes));

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.coinsToSatoshis(1.0)).setPayToPublicKeyHash(decodedAddress.HashData).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToPublicKeyHash(coinKeypairRecipient, transactionOutput).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.step()) {
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.setSignatureVerificationMessage(transactionOutput.getHash());

        System.out.println("Running locking script...");

        while(lockingScript.step()) {
            System.out.println("Stack: \n"+lockingScript.getStackContents());
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }

    @SuppressWarnings("deprecation")
    @RepeatedTest(100) @DisplayName("Pay to MultiSig Lock & Unlock")
    void TestPayToMultiSig() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        byte[] randomPassword = new byte[64];
        ThreadLocalRandom.current().nextBytes(randomPassword);

        //String password = BytesUtil.Base64Encode(randomPassword);

        UISCoinKeypair alice = UISCoinKeypair.create();
        UISCoinKeypair bob = UISCoinKeypair.create();
        UISCoinKeypair charlie = UISCoinKeypair.create();
        UISCoinKeypair daniel = UISCoinKeypair.create();

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.coinsToSatoshis(1.0)).setPayToMultiSig(2, List.of(alice.Keys.getPublic().getEncoded(),bob.Keys.getPublic().getEncoded(), charlie.Keys.getPublic().getEncoded())).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToMultiSig(List.of(charlie,bob),transactionOutput).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.LogScriptExecution  =true;
        unlockingScript.initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(true) {
            try {
                if (!unlockingScript.step()) break;
            } catch (ScriptEmptyStackException | ScriptInvalidParameterException | ScriptUnsupportedOperationException | ScriptInvalidException e) {
                e.printStackTrace();
            }
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.LogScriptExecution  =true;
        lockingScript.setSignatureVerificationMessage(transactionOutput.getHash());

        System.out.println("Running locking script...");

        while(true) {
            try {
                if (!lockingScript.step()) break;
            } catch (ScriptEmptyStackException | ScriptUnsupportedOperationException | ScriptInvalidException | ScriptInvalidParameterException e) {
                e.printStackTrace();
            }
            System.out.println("Stack: \n"+lockingScript.getStackContents());
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }

    @Test
    void TestSize(){
        TransactionOutput transactionOutput = new TransactionOutputBuilder().setPayToPublicKeyHash(new byte[64]).setAmount(123).get();
        System.out.println(transactionOutput.getSize());
        System.out.println(new TransactionInputBuilder().setInputTransaction(new byte[64],1).setUnlockPayToPublicKeyHash(UISCoinKeypair.create(),transactionOutput).get().getSize());
    }

    @RepeatedTest(100) @DisplayName("Pay to Password Lock & Unlock")
    void TestPayToPassword() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {

        byte[] randomPassword = new byte[64];
        ThreadLocalRandom.current().nextBytes(randomPassword);

        String password = BytesUtil.base64Encode(randomPassword);

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.coinsToSatoshis(1.0)).setPayToPassword(password).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToPassword(password).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.LogScriptExecution = true;
        unlockingScript.initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(true) {
            try {
                if (!unlockingScript.step()) break;
            } catch (ScriptEmptyStackException | ScriptInvalidParameterException | ScriptUnsupportedOperationException | ScriptInvalidException e) {
                e.printStackTrace();
            }
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.LogScriptExecution  =true;
        lockingScript.setSignatureVerificationMessage(transactionOutput.getHash());

        System.out.println("Running locking script...");

        while(true) {
            try {
                if (!lockingScript.step()) break;
            } catch (ScriptEmptyStackException | ScriptUnsupportedOperationException | ScriptInvalidException | ScriptInvalidParameterException e) {
                e.printStackTrace();
            }
            System.out.println("Stack: \n"+lockingScript.getStackContents());
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }

    @RepeatedTest(100) @DisplayName("Pay to Script Hash Lock & Unlock")
    void TestPayToScriptHash() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {

        byte[] randomPassword = new byte[64];
        ThreadLocalRandom.current().nextBytes(randomPassword);

        String password = BytesUtil.base64Encode(randomPassword);

        byte[] CustomScript = new TransactionOutputBuilder().setPayToPassword(password).get().LockingScript;
        byte[] CustomScriptUnlock = new TransactionInputBuilder().setUnlockPayToPassword(password).get().UnlockingScript;

        /////////////////////////////////////////////////////////////
        byte[] scriptHashAddress = UISCoinAddress.fromScriptHash(Hash.getSHA512Bytes(CustomScript));

        System.out.println("Pays to address: "+ BytesUtil.base64Encode(scriptHashAddress));

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.coinsToSatoshis(1.0)).setPayToAddress(scriptHashAddress).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;

        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToScriptHash(CustomScript, CustomScriptUnlock).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.LogScriptExecution  =true;
        unlockingScript.initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.step()) {
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.LogScriptExecution  =true;
        lockingScript.setSignatureVerificationMessage(transactionOutput.getHash());

        System.out.println("Running locking script...");

        while(lockingScript.step()) {
            System.out.println("Stack: \n"+lockingScript.getStackContents());
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }

    @RepeatedTest(100) @DisplayName("Pay to Script Hash Signature Verification Lock & Unlock")
    void TestPayToScriptHashMultiStack() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        UISCoinKeypair coinKeypairRecipient = UISCoinKeypair.create();

        byte[] addressv1 = UISCoinAddress.fromPublicKey((ECPublicKey) coinKeypairRecipient.Keys.getPublic());

        UISCoinAddress.DecodedAddress decodedAddress = UISCoinAddress.decodeAddress(addressv1);

        assertTrue(UISCoinAddress.verifyAddressChecksum(addressv1));

        byte[] pubkey_sha512Bytes = Hash.getSHA512Bytes(coinKeypairRecipient.Keys.getPublic().getEncoded());

        assertTrue(Arrays.equals(decodedAddress.HashData, pubkey_sha512Bytes));

        TransactionOutput transactionOutput1 = new TransactionOutputBuilder().setAmount(Conversions.coinsToSatoshis(1.0)).setPayToPublicKeyHash(decodedAddress.HashData).get();
        byte[] CustomScript = transactionOutput1.LockingScript;
        byte[] CustomScriptUnlock = new TransactionInputBuilder().setUnlockPayToPublicKeyHash(coinKeypairRecipient, transactionOutput1).get().UnlockingScript;

        /////////////////////////////////////////////////////////////

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.coinsToSatoshis(1.0)).setPayToScriptHash(Hash.getSHA512Bytes(CustomScript)).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToScriptHash(CustomScript, CustomScriptUnlock).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.LogScriptExecution  =true;
        unlockingScript.initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.step()) {
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.LogScriptExecution  =true;
        lockingScript.setSignatureVerificationMessage(transactionOutput1.getHash());

        System.out.println("Running locking script...");

        while(true) {
            try {
                if (!lockingScript.step()) break;
            } catch (ScriptEmptyStackException | ScriptUnsupportedOperationException | ScriptInvalidException | ScriptInvalidParameterException e) {
                e.printStackTrace();
            }
            System.out.println("Stack: \n"+lockingScript.getStackContents());
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script FromText From ToText")
    void TestScriptFromTextToText() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        Logger root = Logger.getLogger("");
        //root.setLevel(targetLevel);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(Level.FINEST);
        }

        int A = ThreadLocalRandom.current().nextInt();
        int B = ThreadLocalRandom.current().nextInt();
        int C = A + B;

        ScriptBuilder sb = new ScriptBuilder(256);
        sb.push(BytesUtil.numberToByteArray32(A)).push(BytesUtil.numberToByteArray32(B)).fromText("sha512 swap sha512 lenequal verify");

        ScriptExecution scriptExecution = new ScriptExecution();

        String toText = sb.toText();
        System.out.println(toText);

        scriptExecution.initialize(new ScriptBuilder(1024).fromText(toText).get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Float Addition")
    void TestScriptFAddition() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        float A = ThreadLocalRandom.current().nextFloat();
        float B = ThreadLocalRandom.current().nextFloat();
        float C = A + B;
        ScriptParser.GetTokensFromString("push "+A+" push "+B+" 1-1 addfloat push 1.0E-4 blah", true).forEach(System.out::println);
        ScriptBuilder sb = new ScriptBuilder(32);
        sb
                .fromText("push "+A+" push "+B)
                .op(ScriptOperator.ADDFLOAT)
                .pushFloat(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);



        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Float Subtraction")
    void TestScriptFSubtraction() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        float A = ThreadLocalRandom.current().nextFloat();
        float B = ThreadLocalRandom.current().nextFloat();
        float C = A - B;

        ScriptBuilder sb = new ScriptBuilder(32);
        sb
                .pushFloat(A)
                .pushFloat(B)
                .op(ScriptOperator.SUBTRACTFLOAT)
                .pushFloat(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Float Multiplication")
    void TestScriptFMultiplication() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        float A = ThreadLocalRandom.current().nextFloat();
        float B = ThreadLocalRandom.current().nextFloat();
        float C = A * B;

        ScriptBuilder sb = new ScriptBuilder(32);
        sb
                .pushFloat(A)
                .pushFloat(B)
                .op(ScriptOperator.MULTIPLYFLOAT)
                .pushFloat(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Float Division")
    void TestScriptFDivision() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        float A = ThreadLocalRandom.current().nextFloat();
        float B = ThreadLocalRandom.current().nextFloat();
        float C = A / B;

        ScriptBuilder sb = new ScriptBuilder(32);
        sb
                .pushFloat(A)
                .pushFloat(B)
                .op(ScriptOperator.DIVIDEFLOAT)
                .pushFloat(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Long Addition")
    void TestScriptLAddition() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        long A = ThreadLocalRandom.current().nextInt();
        long B = ThreadLocalRandom.current().nextInt();
        long C = A + B;

        ScriptBuilder sb = new ScriptBuilder(64);
        sb
                .pushInt64(A)
                .pushInt64(B)
                .op(ScriptOperator.ADD)
                .pushInt64(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Long Subtraction")
    void TestScriptLSubtraction() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        long A = ThreadLocalRandom.current().nextInt();
        long B = ThreadLocalRandom.current().nextInt();
        long C = A - B;

        ScriptBuilder sb = new ScriptBuilder(64);
        sb
                .pushInt64(A)
                .pushInt64(B)
                .op(ScriptOperator.SUBTRACT)
                .pushInt64(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Exceptions")
    void TestScriptExceptions() {
        ScriptExecution scriptExecution = new ScriptExecution();
        try{
            long A = ThreadLocalRandom.current().nextInt();
            long B = ThreadLocalRandom.current().nextInt();
            long C = A * B;

            ScriptBuilder sb = new ScriptBuilder(64);
            sb.fromText("push 0x00 push 0x01 push 0x02 add");

            System.out.println(Arrays.toString(sb.get()));

            scriptExecution.initialize(sb.get());

            while (scriptExecution.step()){
                System.out.println("Stack: \n"+scriptExecution.getStackContents());
            }

            System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

            System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

            assertFalse(scriptExecution.bScriptFailed);
        } catch (ScriptInvalidException | ScriptEmptyStackException | ScriptInvalidParameterException | ScriptUnsupportedOperationException e) {
            e.printStackTrace();
            assertTrue(scriptExecution.bScriptFailed);
        }

    }

    @RepeatedTest(100)
    @DisplayName("Script Long Multiplication")
    void TestScriptLMultiplication() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        long A = ThreadLocalRandom.current().nextInt();
        long B = ThreadLocalRandom.current().nextInt();
        long C = A * B;

        ScriptBuilder sb = new ScriptBuilder(64);
        sb
                .pushInt64(A)
                .pushInt64(B)
                .op(ScriptOperator.MULTIPLY)
                .pushInt64(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Long Division")
    void TestScriptLDivision() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        long A = ThreadLocalRandom.current().nextInt();
        long B = ThreadLocalRandom.current().nextInt();
        long C = A / B;

        ScriptBuilder sb = new ScriptBuilder(64);
        sb
                .pushInt64(A)
                .pushInt64(B)
                .op(ScriptOperator.DIVIDE)
                .pushInt64(C)
                .op(ScriptOperator.NUMEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.initialize(sb.get());

        while (scriptExecution.step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(1)
    @DisplayName("Script Matcher True")
    void TestScriptMatcher()  {
        byte[] publicKeyHash = new byte[64];
        String memo = "memo";
        byte[] lockingScript = new TransactionOutputBuilder()
                .setAmount(123)
                .setMemo(memo)
                .setPayToPublicKeyHash(publicKeyHash).get().LockingScript;

        ScriptMatcher matcherP2PKH = ScriptMatcher.getMatcherP2PKH();
        assertTrue(matcherP2PKH.match(lockingScript));
        assertTrue(Arrays.equals(matcherP2PKH.getPushData(0),publicKeyHash));
        assertTrue(Arrays.equals(matcherP2PKH.getPushData(1),memo.getBytes(StandardCharsets.UTF_8)));
    }

    @RepeatedTest(1)
    @DisplayName("Script Matcher True Optional")
    void TestScriptMatcherMemo()  {
        byte[] publicKeyHash = new byte[64];
        byte[] lockingScript = new TransactionOutputBuilder()
                .setAmount(123)
                .setPayToPublicKeyHash(publicKeyHash).get().LockingScript;

        ScriptMatcher matcherP2PKH = ScriptMatcher.getMatcherP2PKH();
        assertTrue(matcherP2PKH.match(lockingScript));
        assertTrue(Arrays.equals(matcherP2PKH.getPushData(0),publicKeyHash));
    }

    @RepeatedTest(1)
    @DisplayName("Script Matcher False")
    void TestScriptMatcherFalse()  {
        ScriptMatcher scriptMatcherp2pkh = ScriptMatcher.getMatcherP2PKH();

        byte[] lockingScript = new TransactionOutputBuilder()
                .setAmount(123)
                .setMemo("hello")
                .setPayToPublicKey(new byte[64]).get().LockingScript;

        assertFalse(scriptMatcherp2pkh.match(lockingScript));
    }
}