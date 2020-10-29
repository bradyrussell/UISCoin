package com.bradyrussell.uiscoin.script;

import com.bradyrussell.uiscoin.Util;

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

    public ScriptBuilder virtualScript(byte[] Script){
        return virtualScript(Script, null);
    }

    public ScriptBuilder virtualScript(byte[] Script, List<byte[]> InitialStack){
        if(InitialStack != null) {
            for (byte[] bytes : InitialStack) {
                push(bytes);
            }
            pushByte(InitialStack.size());
        } else {
            pushByte(0);
        }
        push(Script);
        op(ScriptOperator.VIRTUALSCRIPT);
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
        buffer.put(DataToPush.length > 127 ? Util.NumberToByteArray32(DataToPush.length):new byte[]{(byte)DataToPush.length});
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
        buffer.put(Util.NumberToByteArray32(IntToPush));
        return this;
    }

    public ScriptBuilder pushInt64(long IntToPush){
        buffer.put(ScriptOperator.PUSH.OPCode);
        buffer.put((byte)8);
        buffer.put(Util.NumberToByteArray64(IntToPush));
        return this;
    }

    public ScriptBuilder pushFloat(float FloatToPush){
        buffer.put(ScriptOperator.PUSH.OPCode);
        buffer.put((byte)4);
        buffer.put(Util.FloatToByteArray(FloatToPush));
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

    public ScriptBuilder data(byte[] Data){
        buffer.put(Data);
        return this;
    }

    // my attempt to decode bytecode into something parsable by fromText()
    public String toText(){
        byte[] Script = get();
        int InstructionCounter = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("#Decompiled from ").append(Util.Base64Encode(Script)).append("#\n");

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
        Log.info("Parsing script from text...");

        //clean up any extraneous characters and format the script in a single line like TRUE FALSE PUSH [1, 3, 6, 7] RETURN
        String[] parts = Text.replace("\n", " ").replace(";", " ").replace("  ", " ").replace("\r", "").split(" ");

        Log.info(Arrays.toString(parts));

        for (int i = 0; i < parts.length; i++) {
            boolean bIsFunctionSyntax = false;

            if(parts[i].startsWith("#")) {
                String substring = parts[i].substring(1);
                Log.fine("Interpreting Token "+i+" as a comment: "+substring);

                Log.fine("Token "+i+": Begin comment # ");
                do { // single byte
                    /*I = */
                    Log.fine("Token "+i+": comment Element "+parts[i].replace("#", "").replace("#", "") + " from comment part "+parts[i]);
                }while(!parts[i++].endsWith("#"));
            }
            if(parts[i].startsWith("0x")) {
                String substring = parts[i].substring(2);
                Log.fine("Interpreting Token "+i+" as hex data: "+substring);
                fromHexString(substring);
                continue;
            }
            if(parts[i].charAt(0) >= 48 && parts[i].charAt(0) <= 57) {
                Log.fine("Interpreting Token "+i+" as numeric data: "+parts[i]);
                int number = Integer.parseInt(parts[i]);
                if(number < 128 && number > -128) {
                    buffer.put((byte)number);
                } else {
                    buffer.put(Util.NumberToByteArray32(number));
                }

                continue;
            }
            if(parts[i].startsWith("(")) {
                String substring = parts[i].substring(1); // 1){code}
                String parameterCount = substring.substring(0,substring.indexOf(")"));

                if(parameterCount.equalsIgnoreCase("*")){
                    // stack depth -1
                    op(ScriptOperator.DEPTH);
/*                    pushByte(1);
                    op(ScriptOperator.SUBTRACTBYTES);*/
                } else {
                    pushByte(Byte.parseByte(parameterCount));
                }
                bIsFunctionSyntax = true; // drop down to push the code block
            }

            Log.fine("Interpreting Token "+i+" as operator.");
            ScriptOperator scriptOperator = bIsFunctionSyntax ? ScriptOperator.PUSH : ScriptOperator.valueOf(parts[i].toUpperCase());


            Log.fine("Token "+i+": OP "+scriptOperator);

            if(scriptOperator == ScriptOperator.FLAG) {
                if(parts[++i].startsWith("0x")){
                    String hex = parts[i].substring(2);
                    flag(getBytesFromHexString(hex)[0]);

                    Log.fine("Token "+i+": Hex Data "+parts[i]);
                }
                else { // interp as byte
                    flag(Byte.parseByte(parts[i]));
                    Log.fine("Token "+i+": Number "+Integer.parseInt(parts[i]));
                }
            }

            /////////////////////////////
            else if(scriptOperator == ScriptOperator.FLAGDATA) {
                if(parts[++i].startsWith("'")) { // interp as ascii string
                    StringBuilder sb = new StringBuilder();
                    Log.fine("Token "+i+": Begin String ' ");
                    do { // single byte
                        /*I = */
                        sb.append(parts[i].replace("'", "")/*.replace("'", "")*/);
                        if(!parts[i].endsWith("'")) sb.append(" ");
                        Log.fine("Token "+i+": String Element "+parts[i].replace("'", "").replace("'", "") + " from string part "+parts[i]);
                    }while(!parts[i++].endsWith("'"));

                    i--; // todo fix the above loop making this necessary

                    flagData(sb.toString().getBytes(StandardCharsets.US_ASCII));
                } else if(parts[i].startsWith("[")) { // interp as byte array
                    ArrayList<Byte> bytes = new ArrayList<>();
                    Log.fine("Token "+i+": Begin Byte Array [  ");
                    do { // single byte
                        /*I = */
                        byte parseByte = Byte.parseByte(parts[i].replace("[", "").replace("]", "").replace(",", ""));
                        bytes.add(parseByte);
                        Log.fine("Token "+i+": Byte Array Element "+parseByte + " from string part "+parts[i]);
                    }while(!parts[i++].endsWith("]"));

                    i--; // todo fix the above loop making this necessary

                    byte[] byteArray = new byte[bytes.size()];
                    for (int j = 0; j < bytes.size(); j++) {
                        byteArray[j] = bytes.get(j);
                    }
                    flagData(byteArray);
                } else if(parts[i].startsWith("0x")){
                    String hex = parts[i].substring(2);
                    flagData(getBytesFromHexString(hex));

                    Log.fine("Token "+i+": Hex Data "+parts[i]);
                }
                else { // interp as number
                    flagData(Util.NumberToByteArray32(Integer.parseInt(parts[i])));
                    Log.fine("Token "+i+": Number "+Integer.parseInt(parts[i]));
                }
            }
            //////////////////

            else if(scriptOperator == ScriptOperator.PUSH || scriptOperator == ScriptOperator.BIGPUSH) {
                // PUSH 2576
                // PUSH 'ascii text'
                // PUSH [4,5,6,7]
                //int I = ++i;
                if(parts[++i].startsWith("'")) { // interp as ascii string
                    StringBuilder sb = new StringBuilder();
                    Log.fine("Token "+i+": Begin String ' ");
                    do { // single byte
                        /*I = */
                        sb.append(parts[i].replace("'", "")/*.replace("'", "")*/);
                        if(!parts[i].endsWith("'")) sb.append(" ");
                        Log.fine("Token "+i+": String Element "+parts[i].replace("'", "").replace("'", "") + " from string part "+parts[i]);
                    }while(!parts[i++].endsWith("'"));

                    i--; // todo fix the above loop making this necessary

                    pushASCIIString(sb.toString());
                }
                else if(parts[i].startsWith("{")) { // interp as code block
                    StringBuilder sb = new StringBuilder();
                    Log.fine("Token "+i+": Begin Code Block { ");

                    int NumberOfBrackets = 0;

                    do { // single byte
                        /*I = */

                        String part = parts[i];

                        if(parts[i].startsWith("{")) {
                            if(NumberOfBrackets == 0) {
                                part = parts[i].replace("{","");
                            }
                            NumberOfBrackets++;
                        }

                        if(parts[i].endsWith("}")) {
                            NumberOfBrackets--;
                            if(NumberOfBrackets == 0) {
                                part = parts[i].replace("}","");
                            }
                        }

                        sb.append(part);

                        if(!(parts[i].endsWith("}") && NumberOfBrackets == 0)) sb.append(" ");

                        Log.fine("Token "+i+": Code Block Element "+parts[i].replace("{", "").replace("}", "") + " from code block part "+parts[i]);
                    }while(!(parts[i++].endsWith("}") && NumberOfBrackets == 0));

                    i--; // todo fix the above loop making this necessary

                    String codeBlockScript = sb.toString();
                    push(new ScriptBuilder(codeBlockScript.length()).fromText(codeBlockScript).get());
                } else if(parts[i].startsWith("[")) { // interp as byte array
                    ArrayList<Byte> bytes = new ArrayList<>();
                    Log.fine("Token "+i+": Begin Byte Array [  ");
                    do { // single byte
                        /*I = */
                        byte parseByte = Byte.parseByte(parts[i].replace("[", "").replace("]", "").replace(",", ""));
                        bytes.add(parseByte);
                        Log.fine("Token "+i+": Byte Array Element "+parseByte + " from string part "+parts[i]);
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

                    Log.fine("Token "+i+": Hex Data "+parts[i]);
                }
                else { // interp as number
                    if(parts[i].contains(".")) {
                        pushFloat(Float.parseFloat(parts[i]));
                        Log.fine("Token " + i + ": Float " + Float.parseFloat(parts[i]));
                    } else {
                        try {
                            pushInt(Integer.parseInt(parts[i]));
                            Log.fine("Token " + i + ": Number " + Integer.parseInt(parts[i]));
                        } catch (NumberFormatException ignored) {
                            pushInt64(Long.parseLong(parts[i]));
                            Log.fine("Token " + i + ": 64 Bit Number " + Long.parseLong(parts[i]));
                        }
                    }
                }
            }
            else {
                op(scriptOperator);
            }
        }
        Log.info("Script compiled into bytecode.");
        return this;
    }

    public byte[] get(){
        byte[] ret = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, ret, 0, buffer.position());
        return ret;
    }
}
