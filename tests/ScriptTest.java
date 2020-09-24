import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import com.bradyrussell.uiscoin.script.ScriptOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ScriptTest {
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
        sb
                .push(A)
                .op(ScriptOperator.SHA512)
                .push(B)
                .op(ScriptOperator.SHA512EQUAL)
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
}