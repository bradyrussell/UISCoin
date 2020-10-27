import com.bradyrussell.uiscoin.*;
import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import com.bradyrussell.uiscoin.script.ScriptOperator;
import com.bradyrussell.uiscoin.transaction.TransactionInputBuilder;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class ScriptTest {
    @Test
    @DisplayName("Script No Duplicate OpCode Values")
    void TestScriptNoDupOps() {
        ArrayList<Byte> values = new ArrayList<>();
        for (ScriptOperator value : ScriptOperator.values()) {
            if(values.contains(value.OPCode)) {
                System.out.println("Duplicate opcode values: ");
                Util.printBytesHex(new byte[]{value.OPCode});
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
                Util.printBytesHex(new byte[]{value.OPCode});
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
            Util.printBytesHex(new byte[]{unusedValue});
        }

        System.out.println("There are "+values.size()+" used OPCode values.");
        System.out.println("There are "+unusedValues.size()+" unused OPCode values.");
        assertTrue(true);
    }

    @Test
    @DisplayName("Script Append")
    void TestScriptAppend() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Boolean Logic")
    void TestScriptBoolLogic() {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb.fromText("true true true false true false true true false false false true and or or and xor xor xor xor xor xor xor verify");
        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Shift Array")
    void TestScriptRotate() {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb.fromText("push [1, 2, 3, 4] shiftelementsright push [4, 1, 2, 3] bytesequal verify");
        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Shift Array Back")
    void TestScriptRotate2() {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb.fromText("push [4, 1, 2, 3] shiftelementsleft push [1, 2, 3, 4] bytesequal verify");
        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Zip Unzip")
    void TestScriptZip() {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb.fromText("push [1, 2, 3, 4] sha512 zip push [1, 2, 3, 4] sha512 swap unzip bytesequal verify");
        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script Time")
    void TestScriptTime() {
        ScriptBuilder sb = new ScriptBuilder(64);
        sb
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

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertTrue(scriptExecution.bScriptFailed);
    }

    @Test
    @DisplayName("Script BigPush")
    void TestScriptBigPush() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(5000)
    @DisplayName("Invalid Script Terminates Cleanly")
    void TestInvalidScript() {
        try {
            byte[] A = new byte[ThreadLocalRandom.current().nextInt(16, 33)];
            byte[] B = new byte[ThreadLocalRandom.current().nextInt(2048, 2049)];
            byte[] C = new byte[ThreadLocalRandom.current().nextInt(2048, 2049)];
            ThreadLocalRandom.current().nextBytes(A);
            ThreadLocalRandom.current().nextBytes(B);
            ThreadLocalRandom.current().nextBytes(C);

            ScriptExecution scriptExecution = new ScriptExecution();
            scriptExecution.LogScriptExecution = true;

            ArrayList<byte[]> fakeStack = new ArrayList<>();
            fakeStack.add(B);
            fakeStack.add(C);
            //fakeStack.add(A);

            scriptExecution.Initialize(A, Collections.enumeration(fakeStack));

            while (scriptExecution.Step()) {
                System.out.println("Stack: \n"+scriptExecution.getStackContents());
            }

            System.out.println("Script returned: " + !scriptExecution.bScriptFailed);

            System.out.println("Finished: " + scriptExecution.InstructionCounter + " / " + scriptExecution.Script.length);
            //assertTrue(scriptExecution.bScriptFailed);
        }catch (Exception e){
            fail(e);
        }

    }

    @RepeatedTest(1000)
    @DisplayName("Script VirtualScript")
    void TestVirtualScript() {
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
                .virtualScript(virtualSB.get());

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(1000)
    @DisplayName("Script VirtualScript Pass In Stack")
    void TestVirtualScriptInStack() {
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
                .op(ScriptOperator.VIRTUALSCRIPT);

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(1000)
    @DisplayName("Script VirtualScript Pass Out Stack")
    void TestVirtualScriptOutStack() {
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
                .op(ScriptOperator.VIRTUALSCRIPT)
                .op(ScriptOperator.DROP)
                .op(ScriptOperator.DROP)
                .push(A)
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(1000)
    @DisplayName("Script VirtualScript Pass In/Out Stack")
    void TestVirtualScriptInOutStack() {
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
                .op(ScriptOperator.VIRTUALSCRIPT)
                .op(ScriptOperator.DROP)
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Enc/Dec AES")
    void TestScriptAES() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script AES Parity")
    void TestScriptAESParity() throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        byte[] A = new byte[ThreadLocalRandom.current().nextInt(16,64)];
        byte[] B = new byte[ThreadLocalRandom.current().nextInt(16,64)];

        ThreadLocalRandom.current().nextBytes(A);
        ThreadLocalRandom.current().nextBytes(B);

        ScriptBuilder sb = new ScriptBuilder(4096);
        sb
                .push(B)
                .push(A)
                .op(ScriptOperator.ENCRYPTAES)
                .push(Encryption.Encrypt(A,B))
                .op(ScriptOperator.BYTESEQUAL)
                .op(ScriptOperator.VERIFY);

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Addition")
    void TestScriptAddition() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Subtraction")
    void TestScriptSubtraction() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Multiplication")
    void TestScriptMultiplication() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Division")
    void TestScriptDivision() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script SHA512")
    void TestScriptSHA512() {
        byte[] A = new byte[64];
        byte[] B = new byte[64];

        ThreadLocalRandom.current().nextBytes(A);
        ThreadLocalRandom.current().nextBytes(B);

        A = Hash.getSHA512Bytes(A);

        ScriptBuilder sb = new ScriptBuilder(256);
        sb.push(A).push(B).fromText("sha512 swap sha512 lenequal verify");

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
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
                .flagData(Util.NumberToByteArray32(123))
                .get();

        byte[] b= new ScriptBuilder(128).fromText("flag 2 flagdata 123").get();

        Util.printBytesReadable(a);
        Util.printBytesReadable(b);

        assertTrue(Arrays.equals(a,b));
    }

    @Test @DisplayName("Script Code Block")
    void TestScriptCodeBlock(){
        ScriptBuilder sb = new ScriptBuilder(128).fromText("push 0x00 push {push 0x01020304 return} virtualscript drop push 0x01020304 bytesequal verify");

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test @DisplayName("Script Code If Block")
    void TestScriptCodeIfBlock(){
        ScriptBuilder sb = new ScriptBuilder(128).fromText("push {push 'Hello world!' true push 0x02 push {verify sha512} virtualscript drop push 'Hello world!' sha512 bytesequal verify}");

        System.out.println(Arrays.toString(sb.get()));

        ScriptExecution scriptExecution = new ScriptExecution();

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
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

        Util.printBytesReadable(a);
        Util.printBytesReadable(b);

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

        Util.printBytesReadable(a);
        Util.printBytesReadable(b);

        assertTrue(Arrays.equals(a,b));
    }

    @RepeatedTest(100) @DisplayName("Pay to Public Key Lock & Unlock")
    void TestPayToPublicKey(){
        UISCoinKeypair coinKeypairRecipient = UISCoinKeypair.Create();

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.CoinsToSatoshis(1.0)).setPayToPublicKey(coinKeypairRecipient.Keys.getPublic().getEncoded()).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToPublicKey(coinKeypairRecipient, transactionOutput).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.Initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.Step()) {
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.Initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.setSignatureVerificationMessage(transactionOutput.getHash());

        System.out.println("Running locking script...");

        while(lockingScript.Step()) {
            System.out.println("Stack: \n"+lockingScript.getStackContents());
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }

    @Test @DisplayName("Pay to Public Key Hash Lock & Unlock")
    void TestPayToPublicKeyHash(){
        UISCoinKeypair coinKeypairRecipient = UISCoinKeypair.Create();

        byte[] addressv1 = UISCoinAddress.fromPublicKey((ECPublicKey) coinKeypairRecipient.Keys.getPublic());

        UISCoinAddress.DecodedAddress decodedAddress = UISCoinAddress.decodeAddress(addressv1);

        assertTrue(UISCoinAddress.verifyAddressChecksum(addressv1));

        byte[] pubkey_sha512Bytes = Hash.getSHA512Bytes(coinKeypairRecipient.Keys.getPublic().getEncoded());

        Util.printBytesReadable(decodedAddress.HashData);
        Util.printBytesReadable(pubkey_sha512Bytes);
        assertTrue(Arrays.equals(decodedAddress.HashData, pubkey_sha512Bytes));

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.CoinsToSatoshis(1.0)).setPayToPublicKeyHash(decodedAddress.HashData).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToPublicKeyHash(coinKeypairRecipient, transactionOutput).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.Initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.Step()) {
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.Initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.setSignatureVerificationMessage(transactionOutput.getHash());

        System.out.println("Running locking script...");

        while(lockingScript.Step()) {
            System.out.println("Stack: \n"+lockingScript.getStackContents());
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }

    @RepeatedTest(100) @DisplayName("Pay to Password Lock & Unlock")
    void TestPayToPassword(){

        byte[] randomPassword = new byte[64];
        ThreadLocalRandom.current().nextBytes(randomPassword);

        String password = Util.Base64Encode(randomPassword);

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.CoinsToSatoshis(1.0)).setPayToPassword(password).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToPassword(password).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.LogScriptExecution  =true;
        unlockingScript.Initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.Step()) {
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.Initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.LogScriptExecution  =true;
        lockingScript.setSignatureVerificationMessage(transactionOutput.getHash());

        System.out.println("Running locking script...");

        while(lockingScript.Step()) {
            System.out.println("Stack: \n"+lockingScript.getStackContents());
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }

    @RepeatedTest(100) @DisplayName("Pay to Script Hash Lock & Unlock")
    void TestPayToScriptHash(){

        byte[] randomPassword = new byte[64];
        ThreadLocalRandom.current().nextBytes(randomPassword);

        String password = Util.Base64Encode(randomPassword);

        byte[] CustomScript = new TransactionOutputBuilder().setPayToPassword(password).get().LockingScript;
        byte[] CustomScriptUnlock = new TransactionInputBuilder().setUnlockPayToPassword(password).get().UnlockingScript;

        /////////////////////////////////////////////////////////////
        byte[] scriptHashAddress = UISCoinAddress.fromScriptHash(Hash.getSHA512Bytes(CustomScript));

        System.out.println("Pays to address: "+Util.Base64Encode(scriptHashAddress));

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.CoinsToSatoshis(1.0)).setPayToAddress(scriptHashAddress).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;

        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToScriptHash(CustomScript, CustomScriptUnlock).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.LogScriptExecution  =true;
        unlockingScript.Initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.Step()) {
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.Initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.LogScriptExecution  =true;
        lockingScript.setSignatureVerificationMessage(transactionOutput.getHash());

        System.out.println("Running locking script...");

        while(lockingScript.Step()) {
            System.out.println("Stack: \n"+lockingScript.getStackContents());
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }

    @RepeatedTest(100) @DisplayName("Pay to Script Hash Signature Verification Lock & Unlock")
    void TestPayToScriptHashMultiStack(){
        UISCoinKeypair coinKeypairRecipient = UISCoinKeypair.Create();

        byte[] addressv1 = UISCoinAddress.fromPublicKey((ECPublicKey) coinKeypairRecipient.Keys.getPublic());

        UISCoinAddress.DecodedAddress decodedAddress = UISCoinAddress.decodeAddress(addressv1);

        assertTrue(UISCoinAddress.verifyAddressChecksum(addressv1));

        byte[] pubkey_sha512Bytes = Hash.getSHA512Bytes(coinKeypairRecipient.Keys.getPublic().getEncoded());

        assertTrue(Arrays.equals(decodedAddress.HashData, pubkey_sha512Bytes));

        TransactionOutput transactionOutput1 = new TransactionOutputBuilder().setAmount(Conversions.CoinsToSatoshis(1.0)).setPayToPublicKeyHash(decodedAddress.HashData).get();
        byte[] CustomScript = transactionOutput1.LockingScript;
        byte[] CustomScriptUnlock = new TransactionInputBuilder().setUnlockPayToPublicKeyHash(coinKeypairRecipient, transactionOutput1).get().UnlockingScript;

        /////////////////////////////////////////////////////////////

        TransactionOutput transactionOutput = new TransactionOutputBuilder().setAmount(Conversions.CoinsToSatoshis(1.0)).setPayToScriptHash(Hash.getSHA512Bytes(CustomScript)).get();
        byte[] lockingScriptBytes = transactionOutput.LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToScriptHash(CustomScript, CustomScriptUnlock).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.LogScriptExecution  =true;
        unlockingScript.Initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.Step()) {
            System.out.println("Stack: \n"+unlockingScript.getStackContents());
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.Initialize(lockingScriptBytes, unlockingScript.Stack.elements());
        lockingScript.LogScriptExecution  =true;
        lockingScript.setSignatureVerificationMessage(transactionOutput1.getHash());

        System.out.println("Running locking script...");

        while(lockingScript.Step()) {
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
    void TestScriptFromTextToText() {
        int A = ThreadLocalRandom.current().nextInt();
        int B = ThreadLocalRandom.current().nextInt();
        int C = A + B;

        ScriptBuilder sb = new ScriptBuilder(256);
        sb.push(Util.NumberToByteArray32(A)).push(Util.NumberToByteArray32(B)).fromText("sha512 swap sha512 lenequal verify");

        ScriptExecution scriptExecution = new ScriptExecution();

        String toText = sb.toText();
        System.out.println(toText);

        scriptExecution.Initialize(new ScriptBuilder(1024).fromText(toText).get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Float Addition")
    void TestScriptFAddition() {
        float A = ThreadLocalRandom.current().nextFloat();
        float B = ThreadLocalRandom.current().nextFloat();
        float C = A + B;

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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Float Subtraction")
    void TestScriptFSubtraction() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Float Multiplication")
    void TestScriptFMultiplication() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Float Division")
    void TestScriptFDivision() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Long Addition")
    void TestScriptLAddition() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Long Subtraction")
    void TestScriptLSubtraction() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Long Multiplication")
    void TestScriptLMultiplication() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @RepeatedTest(100)
    @DisplayName("Script Long Division")
    void TestScriptLDivision() {
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

        scriptExecution.Initialize(sb.get());

        while (scriptExecution.Step()){
            System.out.println("Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }
}