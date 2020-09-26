import com.bradyrussell.uiscoin.Conversions;
import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import com.bradyrussell.uiscoin.script.ScriptOperator;
import com.bradyrussell.uiscoin.transaction.TransactionInputBuilder;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class ScriptTest {
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
            scriptExecution.dumpStack();
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
            scriptExecution.dumpStack();
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
            scriptExecution.dumpStack();
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
            scriptExecution.dumpStack();
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
            scriptExecution.dumpStack();
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
            scriptExecution.dumpStack();
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
            scriptExecution.dumpStack();
        }

        System.out.println("Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);
    }

    @Test @DisplayName("Script Builder & Text Parser Parity")
    void TestBuilderAndTextParity(){
        byte[] A = new byte[64];

        ThreadLocalRandom.current().nextBytes(A);

        byte[] a  = new ScriptBuilder(128)
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

        byte[] b= new ScriptBuilder(128).fromText("dup sha512").push(A).fromText("len push 4 swap subtract limit bytesequal verify verifysig").get();

        Util.printBytesReadable(a);
        Util.printBytesReadable(b);

        assertTrue(Arrays.equals(a,b));
    }

    @Test @DisplayName("Pay to Public Key Lock & Unlock")
    void TestPayToPublicKey(){
        UISCoinKeypair coinKeypairRecipient = UISCoinKeypair.Create();

        byte[] lockingScriptBytes = new TransactionOutputBuilder().setAmount(Conversions.CoinsToSatoshis(1.0)).setPayToPublicKey(coinKeypairRecipient.Keys.getPublic().getEncoded()).get().LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToPublicKey(coinKeypairRecipient, lockingScriptBytes).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.Initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.Step()) {
            unlockingScript.dumpStackReadable();
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.Initialize(lockingScriptBytes, unlockingScript.Stack.elements());

        System.out.println("Running locking script...");

        while(lockingScript.Step()) {
            lockingScript.dumpStackReadable();
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

        Util.printBytesReadable(decodedAddress.PublicKeyHash);
        Util.printBytesReadable(pubkey_sha512Bytes);
        assertTrue(Arrays.equals(decodedAddress.PublicKeyHash, pubkey_sha512Bytes));

        byte[] lockingScriptBytes = new TransactionOutputBuilder().setAmount(Conversions.CoinsToSatoshis(1.0)).setPayToPublicKeyHash(decodedAddress.PublicKeyHash).get().LockingScript;
        byte[] unlockingScriptBytes = new TransactionInputBuilder().setUnlockPayToPublicKeyHash(coinKeypairRecipient, lockingScriptBytes).get().UnlockingScript;

        ScriptExecution unlockingScript = new ScriptExecution();
        unlockingScript.Initialize(unlockingScriptBytes);

        System.out.println("Running unlocking script...");

        while(unlockingScript.Step()) {
            unlockingScript.dumpStackReadable();
        }

        System.out.println("Unlocking script has ended.");

        if(unlockingScript.bScriptFailed) {
            System.out.println("Unlocking script failed! Terminating...");
            fail("Unlocking script failed!");
        }

        ScriptExecution lockingScript = new ScriptExecution();
        lockingScript.Initialize(lockingScriptBytes, unlockingScript.Stack.elements());

        System.out.println("Running locking script...");

        while(lockingScript.Step()) {
            lockingScript.dumpStackReadable();
        }

        System.out.println("Locking script has ended.");

        if(lockingScript.bScriptFailed) {
            System.out.println("Locking script failed! Terminating...");

        }

        assertTrue(!lockingScript.bScriptFailed && !unlockingScript.bScriptFailed);
    }
}