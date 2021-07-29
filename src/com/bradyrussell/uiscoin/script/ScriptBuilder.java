package com.bradyrussell.uiscoin.script;

import com.bradyrussell.uiscoin.BytesUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class ScriptBuilder {
    private static final Logger Log = Logger.getLogger(StringBuilder.class.getName());
    ByteBuffer buffer;

    public ScriptBuilder(int BufferLength) {
        buffer = ByteBuffer.allocate(BufferLength);
    }

    public ScriptBuilder op(ScriptOperator Operator){
        buffer.put(Operator.OPCode);
        return this;
    }

    public ScriptBuilder op(ScriptOperator Operator, byte[] Data){
        buffer.put(Operator.OPCode);
        buffer.put(Data);
        return this;
    }

    public ScriptBuilder opWith(ScriptOperator Operator, byte[]... Parameters){
        for (byte[] parameter : Parameters) {
            push(parameter);
        }

        buffer.put(Operator.OPCode);
        return this;
    }

    public ScriptBuilder call(byte[] Script){
        return call(Script, null);
    }

    public ScriptBuilder call(byte[] Script, List<byte[]> InitialStack){
        if(InitialStack != null) {
            for (byte[] bytes : InitialStack) {
                push(bytes);
            }
            pushByte(InitialStack.size());
        } else {
            pushByte(0);
        }
        push(Script);
        op(ScriptOperator.CALL);
        return this;
    }

    public ScriptBuilder flag(byte Flag){
        buffer.put(ScriptOperator.FLAG.OPCode);
        buffer.put(Flag);
        return this;
    }

    public ScriptBuilder flagData(byte[] DataToPush){
        buffer.put(ScriptOperator.FLAGDATA.OPCode);
        buffer.put((byte)DataToPush.length);
        buffer.put(DataToPush);
        return this;
    }

    public ScriptBuilder push(byte[] DataToPush){
        buffer.put(DataToPush.length > 127 ? ScriptOperator.BIGPUSH.OPCode : ScriptOperator.PUSH.OPCode);
        buffer.put(DataToPush.length > 127 ? BytesUtil.numberToByteArray32(DataToPush.length):new byte[]{(byte)DataToPush.length});
        buffer.put(DataToPush);
        return this;
    }

    public ScriptBuilder pushByte(byte ByteToPush){
        buffer.put(ScriptOperator.PUSH.OPCode);
        buffer.put((byte)1);
        buffer.put(ByteToPush);
        return this;
    }

    public ScriptBuilder pushByte(int ByteToPush){
        buffer.put(ScriptOperator.PUSH.OPCode);
        buffer.put((byte)1);
        buffer.put((byte)ByteToPush);
        return this;
    }

    public ScriptBuilder pushInt(int IntToPush){
        buffer.put(ScriptOperator.PUSH.OPCode);
        buffer.put((byte)4);
        buffer.put(BytesUtil.numberToByteArray32(IntToPush));
        return this;
    }

    public ScriptBuilder pushInt64(long IntToPush){
        buffer.put(ScriptOperator.PUSH.OPCode);
        buffer.put((byte)8);
        buffer.put(BytesUtil.numberToByteArray64(IntToPush));
        return this;
    }

    public ScriptBuilder pushFloat(float FloatToPush){
        buffer.put(ScriptOperator.PUSH.OPCode);
        buffer.put((byte)4);
        buffer.put(BytesUtil.floatToByteArray(FloatToPush));
        return this;
    }

    public ScriptBuilder pushHexString(String Hex){ // https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
        byte[] data = getBytesFromHexString(Hex);
        push(data);
        return this;
    }

    public ScriptBuilder fromHexString(String Hex){ // https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
        byte[] data = getBytesFromHexString(Hex);
        buffer.put(data);
        return this;
    }

    private byte[] getBytesFromHexString(String Hex) {
        int len = Hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(Hex.charAt(i), 16) << 4)
                    + Character.digit(Hex.charAt(i + 1), 16));
        }
        return data;
    }

    public ScriptBuilder pushASCIIString(String Str){
        byte[] strBytes = Str.getBytes(StandardCharsets.US_ASCII);
        push(strBytes);
        return this;
    }

    public ScriptBuilder pushUTF8String(String Str){
        byte[] strBytes = Str.getBytes(StandardCharsets.UTF_8);
        push(strBytes);
        return this;
    }

    public ScriptBuilder data(byte[] Data){
        buffer.put(Data);
        return this;
    }

    // my attempt to decode bytecode into something parsable by fromText()
    public String toText(){
        byte[] Script = get();
        int InstructionCounter = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("//Decompiled from ").append(BytesUtil.base64Encode(Script)).append("#\n");

        while(InstructionCounter < Script.length){
            //////////////////////////////////////////////////////////////////////////////////////
            ScriptOperator scriptOperator = ScriptOperator.getByOpCode(Script[InstructionCounter++]);
            sb.append(scriptOperator);

            if(scriptOperator == ScriptOperator.PUSH || scriptOperator == ScriptOperator.BIGPUSH || scriptOperator == ScriptOperator.FLAGDATA) {
                byte NumberOfBytesToPush = Script[InstructionCounter++];
                byte[] bytes = new byte[NumberOfBytesToPush];

                for (int i = 0; i < NumberOfBytesToPush; i++) {
                    bytes[i] = Script[InstructionCounter++];
                }

                sb.append(" [");
                for (int i = 0; i < bytes.length; i++) {
                    sb.append(bytes[i]).append(i == (bytes.length-1) ? "":", ");
                }
                sb.append("]");
            } else if(scriptOperator == ScriptOperator.FLAG){
                sb.append(" 0x");
                sb.append(String.format("%02X", Script[InstructionCounter++]));
            }

            sb.append("\n");
            /////////////////////////////////////////////////////////////////////////////////////
        }
        return sb.toString();
    }

    public ScriptBuilder fromText(String Text){
        data(ScriptParser.CompileScriptTokensToBytecode(ScriptParser.GetTokensFromString(Text, true)));
        return this;
    }

    public byte[] get(){
        byte[] ret = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, ret, 0, buffer.position());
        return ret;
    }
}
