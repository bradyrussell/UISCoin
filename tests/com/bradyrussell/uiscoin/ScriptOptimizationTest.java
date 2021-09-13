/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import com.bradyrussell.uiscoin.script.*;
import com.bradyrussell.uiscoin.script.exception.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

public class ScriptOptimizationTest {
/*
    @RepeatedTest(100)
    @DisplayName("Script Optimization - All")
    void TestScriptOptimization_All() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException, ScriptFailedException {
        float A = ThreadLocalRandom.current().nextFloat();
        float B = ThreadLocalRandom.current().nextFloat();
        float C = A + B;

        ScriptBuilder sb = new ScriptBuilder(1024);

        sb.fromText("flag 0x05 flagdata 0x0505050505050505 push 0x01 push 0x02 push 0x03 push 0x00 bigpush 0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");


        byte[] unoptimized = sb.get();
        System.out.println(Arrays.toString(unoptimized));

        byte[] optimized = ScriptOptimizer.OptimizeScriptBytecode(unoptimized);
        System.out.println(Arrays.toString(optimized));

        //////////////////////////////////////////////////////////////////////////////////////

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(unoptimized);

        while (scriptExecution.step()){
            System.out.println("Unoptimized Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Unoptimized Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Unoptimized  Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);

        //////////////////////////////////////////////////////////////////////////////////

        ScriptExecution scriptExecutionO = new ScriptExecution();
        scriptExecutionO.LogScriptExecution = true;

        scriptExecutionO.initialize(optimized);

        while (scriptExecutionO.step()){
            System.out.println("Optimized Stack: \n"+scriptExecutionO.getStackContents());
        }

        System.out.println("Optimized Script returned: "+!scriptExecutionO.bScriptFailed);

        System.out.println("Optimized  Finished: "+scriptExecutionO.InstructionCounter+" / "+scriptExecutionO.Script.length);

        assertFalse(scriptExecutionO.bScriptFailed);
        ////////////////////////////////////////////////////////////////////////////////////

        assertEquals(scriptExecutionO.getStackContents(), scriptExecution.getStackContents());
        assertTrue(optimized.length < unoptimized.length);
    }

    @RepeatedTest(100)
    @DisplayName("Script Optimization - Push Boolean")
    void TestScriptOptimization_PushBoolean() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException, ScriptFailedException {
        float A = ThreadLocalRandom.current().nextFloat();
        float B = ThreadLocalRandom.current().nextFloat();
        float C = A + B;

        ScriptBuilder sb = new ScriptBuilder(1024);

        sb.fromText("flag 0x05 flagdata 0x0505050505050505 push 0x01 push 0x02 push 0x03 push 0x00 bigpush 0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");


        byte[] unoptimized = sb.get();
        System.out.println(Arrays.toString(unoptimized));

        byte[] optimized = ScriptOptimizer.BytecodeOptimization_BooleanPush(unoptimized);
        System.out.println(Arrays.toString(optimized));

        //////////////////////////////////////////////////////////////////////////////////////

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(unoptimized);

        while (scriptExecution.step()){
            System.out.println("Unoptimized Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Unoptimized Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Unoptimized  Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);

        //////////////////////////////////////////////////////////////////////////////////

        ScriptExecution scriptExecutionO = new ScriptExecution();
        scriptExecutionO.LogScriptExecution = true;

        scriptExecutionO.initialize(optimized);

        while (scriptExecutionO.step()){
            System.out.println("Optimized Stack: \n"+scriptExecutionO.getStackContents());
        }

        System.out.println("Optimized Script returned: "+!scriptExecutionO.bScriptFailed);

        System.out.println("Optimized  Finished: "+scriptExecutionO.InstructionCounter+" / "+scriptExecutionO.Script.length);

        assertFalse(scriptExecutionO.bScriptFailed);
        ////////////////////////////////////////////////////////////////////////////////////

        assertEquals(scriptExecutionO.getStackContents(), scriptExecution.getStackContents());
        assertTrue(optimized.length < unoptimized.length);
    }
*/

/*
    @RepeatedTest(100)
    @DisplayName("Script Optimization - Skip Unconditional Jump")
    void TestScriptOptimization_SkipUnconditionalJump() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException, ScriptFailedException {
        float A = ThreadLocalRandom.current().nextFloat();
        float B = ThreadLocalRandom.current().nextFloat();
        float C = A + B;

        ScriptBuilder sb = new ScriptBuilder(1024);

        sb.fromText("flag 0x05 push [13] jump flagdata 0x0505050505050505 flag 0x05 push 0x01 push 0x02 push 0x03 push 0x00 bigpush 0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");

        byte[] unoptimized = sb.get();
        System.out.println(Arrays.toString(unoptimized));

        byte[] optimized = ScriptOptimizer.BytecodeOptimization_SkipUnconditionalJump(unoptimized);
        System.out.println(Arrays.toString(optimized));

        //////////////////////////////////////////////////////////////////////////////////////

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(unoptimized);

        while (scriptExecution.step()){
            System.out.println("Unoptimized Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Unoptimized Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Unoptimized  Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);

        //////////////////////////////////////////////////////////////////////////////////

        ScriptExecution scriptExecutionO = new ScriptExecution();
        scriptExecutionO.LogScriptExecution = true;

        scriptExecutionO.initialize(optimized);

        while (scriptExecutionO.step()){
            System.out.println("Optimized Stack: \n"+scriptExecutionO.getStackContents());
        }

        System.out.println("Optimized Script returned: "+!scriptExecutionO.bScriptFailed);

        System.out.println("Optimized  Finished: "+scriptExecutionO.InstructionCounter+" / "+scriptExecutionO.Script.length);

        assertFalse(scriptExecutionO.bScriptFailed);
        ////////////////////////////////////////////////////////////////////////////////////

        assertEquals(scriptExecutionO.getStackContents(), scriptExecution.getStackContents());
        assertTrue(optimized.length <= unoptimized.length);
    }
*/

   /* @RepeatedTest(1)
    @DisplayName("Script Optimization - Skip NOP")
    void TestScriptOptimization_SkipNOP() throws ScriptInvalidException, ScriptEmptyStackException, ScriptInvalidParameterException, ScriptUnsupportedOperationException {
        float A = ThreadLocalRandom.current().nextFloat();
        float B = ThreadLocalRandom.current().nextFloat();
        float C = A + B;

        ScriptBuilder sb = new ScriptBuilder(1024);

        //todo this works if jump is +1 what i expect. im guessing jump indexing is off by one
        sb.fromText("nop nop nop nop nop nop push 0xFFFFFFFF push 0xFFFFFFFF add push [-1, -1, -1, -2] bytesequal push [9] jumpif nop nop nop nop nop nop nop nop push 0x01 push 0x02 push 0x03 push 0x04 true push [9] jumpif nop nop nop nop nop nop nop nop push 0x01 push 0x02 push 0x03 push 0x04");

*//*
        sb.fromText("nop nop nop nop\n" +
                "push 0x01\n" +
                "nop nop nop nop nop nop nop nop nop nop nop nop nop nop\n" +
                "push 0x07\n" +
                "jumpif \n" +
                "nop nop nop nop nop nop \n" +
                "push 0x03 nop nop nop nop nop nop nop");*//*

        byte[] unoptimized = sb.get();
        System.out.println(Arrays.toString(unoptimized));

        byte[] optimized = ScriptOptimizer.BytecodeOptimization_RemoveNOPs(unoptimized);
        System.out.println(Arrays.toString(optimized));

        //////////////////////////////////////////////////////////////////////////////////////

        ScriptExecution scriptExecution = new ScriptExecution();
        scriptExecution.LogScriptExecution = true;

        scriptExecution.initialize(unoptimized);

        while (scriptExecution.step()){
            System.out.println("Unoptimized Stack: \n"+scriptExecution.getStackContents());
        }

        System.out.println("Unoptimized Script returned: "+!scriptExecution.bScriptFailed);

        System.out.println("Unoptimized  Finished: "+scriptExecution.InstructionCounter+" / "+scriptExecution.Script.length);

        assertFalse(scriptExecution.bScriptFailed);

        //////////////////////////////////////////////////////////////////////////////////

        ScriptExecution scriptExecutionO = new ScriptExecution();
        scriptExecutionO.LogScriptExecution = true;

        scriptExecutionO.initialize(optimized);

        while (scriptExecutionO.step()){
            System.out.println("Optimized Stack: \n"+scriptExecutionO.getStackContents());
        }

        System.out.println("Optimized Script returned: "+!scriptExecutionO.bScriptFailed);

        System.out.println("Optimized  Finished: "+scriptExecutionO.InstructionCounter+" / "+scriptExecutionO.Script.length);

        assertFalse(scriptExecutionO.bScriptFailed);
        ////////////////////////////////////////////////////////////////////////////////////

        assertEquals(scriptExecutionO.getStackContents(), scriptExecution.getStackContents());
        //assertTrue(optimized.length <= unoptimized.length);
        System.out.println("Unoptimized length: "+ unoptimized.length);
        System.out.println("Optimized length: "+ optimized.length);
        if(optimized.length >= unoptimized.length) System.out.println("Warning: Optimization made script longer!");
    }*/
}