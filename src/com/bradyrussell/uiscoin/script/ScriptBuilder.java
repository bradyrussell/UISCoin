package com.bradyrussell.uiscoin.script;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ScriptBuilder {
    ByteBuffer buffer;

    public ScriptBuilder(int Length) {
        buffer = ByteBuffer.allocate(Length);
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

    public ScriptBuilder push(byte[] DataToPush){
        buffer.put(ScriptOperator.PUSH.OPCode);
        buffer.put((byte)DataToPush.length);
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
        buffer.put(ScriptExecution.NumberToByteArray(IntToPush));
        return this;
    }

    public ScriptBuilder pushHexString(String Hex){ // https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
        int len = Hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(Hex.charAt(i), 16) << 4)
                    + Character.digit(Hex.charAt(i+1), 16));
        }
        buffer.put(data);
        return this;
    }

    public ScriptBuilder pushASCIIString(String Str){
        buffer.put(ScriptOperator.PUSH.OPCode);

        byte[] strBytes = Str.getBytes(StandardCharsets.US_ASCII);

        buffer.put((byte)strBytes.length);
        buffer.put(strBytes);
        return this;
    }

    public ScriptBuilder data(byte[] Data){
        buffer.put(Data);
        return this;
    }

    public byte[] get(){
        return buffer.array();
    }
}
