package com.bradyrussell.uiscoin.script;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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
        buffer.put(ScriptOperator.PUSH.OPCode);
        buffer.put((byte)data.length);
        buffer.put(data);
        return this;
    }

    public ScriptBuilder fromHexString(String Hex){ // https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
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

    public ScriptBuilder fromText(String Text){
        String[] parts = Text.replace("\n", "").replace(";", " ").replace("  ", " ").split(" ");

        for (int i = 0; i < parts.length; i++) {
            if(parts[i].startsWith("0x")) {
                String substring = parts[i].substring(2);
                System.out.println("Interpreting Token "+i+" as hex data: "+substring);
                fromHexString(substring);
                continue;
            }
            if(parts[i].charAt(0) >= 48 && parts[i].charAt(0) <= 57) {
                System.out.println("Interpreting Token "+i+" as numeric data: "+parts[i]);
                int number = Integer.parseInt(parts[i]);
                if(number < 128 && number > -128) {
                    buffer.put((byte)number);
                } else {
                    buffer.put(ScriptExecution.NumberToByteArray(number));
                }

                continue;
            }

            System.out.println("Interpreting Token "+i+" as operator.");
            ScriptOperator scriptOperator = ScriptOperator.valueOf(parts[i].toUpperCase());

            System.out.println("Token "+i+": OP "+scriptOperator);

            if(scriptOperator == ScriptOperator.PUSH) {
                // PUSH 2576
                // PUSH 'ascii text'
                // PUSH [4,5,6,7]
                //int I = ++i;
                if(parts[++i].startsWith("'")) { // interp as ascii string
                    StringBuilder sb = new StringBuilder();
                    System.out.println("Token "+i+": Begin String ' ");
                    do { // single byte
                        /*I = */;
                        sb.append(parts[i].replace("'", "").replace("'", ""));
                        if(!parts[i].endsWith("'")) sb.append(" ");
                        System.out.println("Token "+i+": String Element "+parts[i].replace("'", "").replace("'", "") + " from string part "+parts[i]);
                    }while(!parts[i++].endsWith("'"));

                    i--; // todo fix the above loop making this necessary

                    pushASCIIString(sb.toString());
                } else if(parts[i].startsWith("[")) { // interp as byte array
                    ArrayList<Byte> bytes = new ArrayList<>();
                    System.out.println("Token "+i+": Begin Byte Array [  ");
                    do { // single byte
                        /*I = */;
                        byte parseByte = Byte.parseByte(parts[i].replace("[", "").replace("]", "").replace(",", ""));
                        bytes.add(parseByte);
                        System.out.println("Token "+i+": Byte Array Element "+parseByte + " from string part "+parts[i]);
                    }while(!parts[i++].endsWith("]"));

                    i--; // todo fix the above loop making this necessary

                    byte[] byteArray = new byte[bytes.size()];
                    for (int j = 0; j < bytes.size(); j++) {
                        byteArray[j] = bytes.get(j);
                    }
                    push(byteArray);
                } else if(parts[i].startsWith("0x")){
                    String hex = parts[i].substring(2);
                    pushHexString(hex);

                    System.out.println("Token "+i+": Hex Data "+parts[i]);
                }
                else { // interp as number
                    pushInt(Integer.parseInt(parts[i]));
                    System.out.println("Token "+i+": Number "+Integer.parseInt(parts[i]));
                }
            }
            else {
                op(scriptOperator);
            }
        }

        return this;
    }

    public byte[] get(){
        return buffer.array();
    }
}
