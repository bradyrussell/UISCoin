/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.script;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.bradyrussell.uiscoin.BytesUtil;
@Deprecated
public class ScriptOptimizer {
    public static String OptimizeScriptHighLevel(String UnoptimizedHighLevel) {
        return null;
    }

    public static String OptimizeScriptAssembly(String UnoptimizedAssembly) {
        return null;
    }

    public static byte[] OptimizeScriptBytecode(byte[] UnoptimizedBytecode) {
        return BytecodeOptimization_BooleanPush(BytecodeOptimization_SkipUnconditionalJump(UnoptimizedBytecode));
    }

    // todo remove jumps that get optimized to 1

    // todo if a push <number> exists see if it can be pushed shorter like push [0, 0, 0, 0, 0, 0, 0, 2] (9 bytes) could be push [2] convert8to32 convert32to64 (4 bytes)

    // todo if push number then immediately transform it , or push 2 numbers then immediately transform them, replace with result

    // todo if push boolean conditon then immediately not it, replace with inverse ?? like a < b not  replace with a >= b // this is a very minor optimization

    //todo REWRITE JUMPS
    //todo break into struct {
    /*
    OP
    MultibyteData[]

    position = number of ops + number of multibytes preceding
    link a jump node to another node
    do opts
    get resulting node's new location
     */

    /**
     *  detect JUMPs that will always happen and cut out anything skipped
     *  JUMPs that will always happen include :
     *      when push is called immediately before JUMP
     */
    public static byte[] BytecodeOptimization_SkipUnconditionalJump(byte[] UnoptimizedBytecode) {
        ByteBuffer optimizedBuffer = ByteBuffer.allocate(UnoptimizedBytecode.length);

        for (int InstructionCounter = 0; InstructionCounter < UnoptimizedBytecode.length; InstructionCounter++) {
            ScriptOperator currentOperator = ScriptOperator.getByOpCode(UnoptimizedBytecode[InstructionCounter]);

            if (currentOperator != null) {
                optimizedBuffer.put(UnoptimizedBytecode[InstructionCounter]);

                if (currentOperator == ScriptOperator.PUSH) {
                    byte bytesToPush = UnoptimizedBytecode[InstructionCounter + 1];
                    byte nextByte2 = UnoptimizedBytecode[InstructionCounter + 2];

                    // match a single byte push followed by JUMP
                    if (bytesToPush == 0x01 && UnoptimizedBytecode.length > InstructionCounter+3 && UnoptimizedBytecode[InstructionCounter + 3] == ScriptOperator.JUMP.OPCode) {
                        optimizedBuffer.position(optimizedBuffer.position() - 1); // overwrite the push
                        InstructionCounter += (3 + nextByte2-1); // skip to the end of the jump
                    } else {
                        optimizedBuffer.put(UnoptimizedBytecode[++InstructionCounter]);
                        for (int InstructionCounterStart = InstructionCounter; InstructionCounter < InstructionCounterStart + bytesToPush; InstructionCounter++) {
                            optimizedBuffer.put(UnoptimizedBytecode[InstructionCounter+1]);
                        }
                    }
                } else if (currentOperator == ScriptOperator.BIGPUSH) {
                    byte[] bytesToPushArr  = new byte[4];
                    System.arraycopy(UnoptimizedBytecode, InstructionCounter+1, bytesToPushArr, 0, 4);

                    int bytesToPush = BytesUtil.byteArrayToNumber32(bytesToPushArr);

                    InstructionCounter+=4;

                    optimizedBuffer.put(bytesToPushArr);
                    for (int InstructionCounterStart = InstructionCounter; InstructionCounter < InstructionCounterStart + bytesToPush; InstructionCounter++) {
                        optimizedBuffer.put(UnoptimizedBytecode[InstructionCounter+1]);
                    }
                } else if (currentOperator == ScriptOperator.FLAGDATA) {
                    byte bytesToPush = UnoptimizedBytecode[InstructionCounter + 1];
                    optimizedBuffer.put(UnoptimizedBytecode[++InstructionCounter]);
                    for (int InstructionCounterStart = InstructionCounter; InstructionCounter < InstructionCounterStart + bytesToPush; InstructionCounter++) {
                        optimizedBuffer.put(UnoptimizedBytecode[InstructionCounter+1]);
                    }
                } else if (currentOperator == ScriptOperator.FLAG) {
                    optimizedBuffer.put(UnoptimizedBytecode[++InstructionCounter]);
                }
            }
        }


        byte[] ret = new byte[optimizedBuffer.position()];
        System.arraycopy(optimizedBuffer.array(), 0, ret, 0, optimizedBuffer.position());
        return ret;
    }

    /**
     *  replace 010100 and 010101 with false and true respectively
     */
    public static byte[] BytecodeOptimization_BooleanPush(byte[] UnoptimizedBytecode) {
        ByteBuffer optimizedBuffer = ByteBuffer.allocate(UnoptimizedBytecode.length);

        for (int InstructionCounter = 0; InstructionCounter < UnoptimizedBytecode.length; InstructionCounter++) {
            ScriptOperator currentOperator = ScriptOperator.getByOpCode(UnoptimizedBytecode[InstructionCounter]);

            if (currentOperator != null) {
                optimizedBuffer.put(UnoptimizedBytecode[InstructionCounter]);

                if (currentOperator == ScriptOperator.PUSH) {
                    byte bytesToPush = UnoptimizedBytecode[InstructionCounter + 1];
                    byte nextByte2 = UnoptimizedBytecode[InstructionCounter + 2];

                    if (bytesToPush == 0x01 && (nextByte2 == 0x00 || nextByte2 == 0x01)) { // we have found somewhere to optimize
                        optimizedBuffer.position(optimizedBuffer.position() - 1); // overwrite the push
                        optimizedBuffer.put(nextByte2 == 0x01 ? ScriptOperator.TRUE.OPCode : ScriptOperator.FALSE.OPCode);
                        InstructionCounter += 2;
                    } else {
                        optimizedBuffer.put(UnoptimizedBytecode[++InstructionCounter]);
                        for (int InstructionCounterStart = InstructionCounter; InstructionCounter < InstructionCounterStart + bytesToPush; InstructionCounter++) {
                            optimizedBuffer.put(UnoptimizedBytecode[InstructionCounter+1]);
                        }
                    }
                } else if (currentOperator == ScriptOperator.BIGPUSH) {
                    byte[] bytesToPushArr  = new byte[4];
                    System.arraycopy(UnoptimizedBytecode, InstructionCounter+1, bytesToPushArr, 0, 4);

                    int bytesToPush = BytesUtil.byteArrayToNumber32(bytesToPushArr);

                    InstructionCounter+=4;

                    optimizedBuffer.put(bytesToPushArr);
                    for (int InstructionCounterStart = InstructionCounter; InstructionCounter < InstructionCounterStart + bytesToPush; InstructionCounter++) {
                        optimizedBuffer.put(UnoptimizedBytecode[InstructionCounter+1]);
                    }
                } else if (currentOperator == ScriptOperator.FLAGDATA) {
                    byte bytesToPush = UnoptimizedBytecode[InstructionCounter + 1];
                    optimizedBuffer.put(UnoptimizedBytecode[++InstructionCounter]);
                    for (int InstructionCounterStart = InstructionCounter; InstructionCounter < InstructionCounterStart + bytesToPush; InstructionCounter++) {
                        optimizedBuffer.put(UnoptimizedBytecode[InstructionCounter+1]);
                    }
                } else if (currentOperator == ScriptOperator.FLAG) {
                    optimizedBuffer.put(UnoptimizedBytecode[++InstructionCounter]);
                }
            }
        }


        byte[] ret = new byte[optimizedBuffer.position()];
        System.arraycopy(optimizedBuffer.array(), 0, ret, 0, optimizedBuffer.position());
        return ret;
    }

    public static byte[] BytecodeOptimization_RemoveNOPs(byte[] Script){
        ByteBuffer buffer = ByteBuffer.allocate(Script.length);

        ScriptOptimizationAlignment soa = new ScriptOptimizationAlignment();
        int NOPStartIndex = -1;
        int NumberNOPS = 0;

        for (int i = 0; i < Script.length; i++) {
            // todo skip over multibyte ops
            if(Script[i] != ScriptOperator.NOP.OPCode) {
                buffer.put(Script[i]);
                if(NOPStartIndex >= 0) {
                    soa.Add(NOPStartIndex, NumberNOPS);
                    NOPStartIndex = -1;
                    NumberNOPS = 0;
                }
            } else {
                if(NOPStartIndex < 0) {
                    NOPStartIndex = i;
                }
                NumberNOPS++;
            }
        }
        if(NOPStartIndex > 0) {
            soa.Add(NOPStartIndex, NumberNOPS);
        }

        System.out.println("Remove NOPs removed "+soa.get().size()+" NOP groups.");
        for (ScriptOptimizationAlignmentSection scriptOptimizationAlignmentSection : soa.get()) {
            System.out.println("From "+scriptOptimizationAlignmentSection.Index+" to "+(scriptOptimizationAlignmentSection.Index+scriptOptimizationAlignmentSection.Offset));
        }

        byte[] returnArray = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, returnArray, 0, buffer.position());

        for (ScriptOptimizationAlignmentSection scriptOptimizationAlignmentSection : soa.getReversed()) {
            System.out.println("Before: "+Arrays.toString(returnArray));
            returnArray = RewriteJumpsAfter(returnArray, scriptOptimizationAlignmentSection.Index, scriptOptimizationAlignmentSection.Offset);
            System.out.println("After: "+Arrays.toString(returnArray));
        }

        return returnArray;
    }

    // todo not working. needs to be called after each modification and not after all?? otherwise the indices recorded no longer make sense
    public static byte[] RewriteJumpsAfter(byte[] Script, int StartIndex, int BytesRemoved){
        if(StartIndex == 0) return Script;

        ByteBuffer buffer = ByteBuffer.allocate(Script.length+1024); // todo why is this getting overflowed when low

        System.out.println("Realigning JUMPs that land after "+StartIndex+" back "+BytesRemoved+" bytes!");

        for (int i = 0; i < Script.length; i++) {

            // todo skip over multibyte ops

            //if this code is before the cut, and is a jump
            if(i <= StartIndex && ScriptOperator.JUMP.OPCode == Script[i] || ScriptOperator.JUMPIF.OPCode == Script[i]) {
                // anything here is inserted right before the jump
                // adds 13 bytes
                buffer.put(ScriptOperator.DUP.OPCode);
                buffer.put(ScriptOperator.PUSH.OPCode);
                buffer.put((byte)0x01);
                buffer.put((byte)(StartIndex - i));  // todo test off by ones // todo this is not right? // todo StartIndex is no longer aligned when we do one optimization
                buffer.put(ScriptOperator.LESSTHANEQUAL.OPCode);
                buffer.put(ScriptOperator.PUSH.OPCode);
                buffer.put((byte)0x01);
                buffer.put((byte)0x05);
                buffer.put(ScriptOperator.JUMPIF.OPCode);
                buffer.put(ScriptOperator.PUSH.OPCode);
                buffer.put((byte)0x01);
                buffer.put((byte)BytesRemoved); // todo test off by ones
                buffer.put(ScriptOperator.SUBTRACTBYTES.OPCode);

                /* // this will patch the top stack value
                    DUP
                    PUSH [x] // is jump greaterthan the boundary // x is the maximal value where a jump still is aligned properly
                    LESSTHANEQUAL // inverse greaterthan
                    PUSH [5] // 5 bytes in if statement
                    JUMPIF   // skip realignment
                    PUSH [y] // amount to reduce jump by
                    SUBTRACTBYTES

                 */
            }

            buffer.put(Script[i]);
        }
        byte[] returnArray = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, returnArray, 0, buffer.position());
        return returnArray;
    }

    private static class ScriptOptimizationAlignmentSection{
        int Index;
        int Offset;

        public ScriptOptimizationAlignmentSection(int index, int offset) {
            Index = index;
            Offset = offset;
        }
    }

    private static class ScriptOptimizationAlignment{
        ArrayList<ScriptOptimizationAlignmentSection> Sections = new ArrayList<>();

        void Add(int Index, int Offset){
            Sections.add(new ScriptOptimizationAlignmentSection(Index, Offset));
        }

        ArrayList<ScriptOptimizationAlignmentSection> get(){
            return Sections;
        }

        ArrayList<ScriptOptimizationAlignmentSection> getReversed(){
            ArrayList<ScriptOptimizationAlignmentSection> ret = new ArrayList<>(Sections);
            Collections.reverse(ret);
            return ret;
        }
    }
}
