package com.bradyrussell.uiscoin.script;

import com.bradyrussell.uiscoin.Util;

import java.nio.ByteBuffer;

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

    // todo if a push <number> exists see if it can be pushed shorter like push [0, 0, 0, 0, 0, 0, 0, 2] (9 bytes) could be push [2] convert8to32 convert32to64 (4 bytes)

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

                    int bytesToPush = Util.ByteArrayToNumber32(bytesToPushArr);

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

                    int bytesToPush = Util.ByteArrayToNumber32(bytesToPushArr);

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

    private static byte[] RewriteJumpsAfter(byte[] Script, int StartIndex, int BytesRemoved){
        ByteBuffer buffer = ByteBuffer.allocate(Script.length);

        byte[] changed = new byte[StartIndex-1];
        byte[] unchanged = new byte[Script.length - StartIndex];
        System.arraycopy(Script, 0, changed, 0, StartIndex-1);
        System.arraycopy(Script, StartIndex, unchanged, 0, Script.length - StartIndex);

        for (int i = 0; i < changed.length; i++) {
            if(changed[i] == ScriptOperator.JUMPIF.OPCode || changed[i] == ScriptOperator.JUMP.OPCode) {
                buffer.put(ScriptParser.CompileScriptTokensToBytecode(ScriptParser.GetTokensFromString("", true)));
               // if would jump over the boundry subtract bytesremoved else nothing
            }
        }

        return null;
        // might have to make a JumpRemapList  of pairs of modifications <StartIndex, BytesRemoved> so jumps can iterate thru til they hit a startindex and then subtract bytesremoved from the jumpvalue
    }
}
