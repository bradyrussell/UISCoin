package com.bradyrussell.uiscoin.script;

import com.bradyrussell.uiscoin.BytesUtil;

import java.util.*;

public class ScriptUtil {
    public static String PrintStack(Enumeration<byte[]> StackValues, byte[] PoppedElement){
        List<byte[]> stackList = Collections.list(StackValues);

        if(PoppedElement != null) stackList.add(PoppedElement);

        StringBuilder sb = new StringBuilder();

        sb.append("Stack has ").append(stackList.size()).append(" element").append(stackList.size() == 1 ? ".\n":"s.\n");

        for (int i = 0; i < stackList.size(); i++) {
            if (PoppedElement != null && i == stackList.size()-1) sb.append(">>>");

            byte[] bytes = stackList.get(i);
            sb.append(Arrays.toString(bytes)).append("\n");
        }

        return sb.toString();
    }

    public static String PrintScriptOpCodesSurroundingHighlight(byte[] Script, int Highlight, int Surrounding, String Message){
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptTrace:"+"\n");

        int Start = Math.max(0,Highlight-Surrounding);
        int End = Math.min(Script.length-1,Highlight+Surrounding);

        boolean bJustHitPush = false;
        byte pushAmt = 0;

        for (int i = 0; i < Script.length; i++) {
            ScriptOperator byOpCode = ScriptOperator.getByOpCode(Script[i]);

            if(bJustHitPush) {
                pushAmt = (byte) (Script[i]+1);
            }
            if(byOpCode == ScriptOperator.PUSH && pushAmt <= 0) bJustHitPush = true;

            if (i >= Start && i <= End) sb.append(i);
            if (i >= Start && i <= End) sb.append(pushAmt > 0 ? " | > " : " | ");
            if (byOpCode == null || pushAmt > 0) {
                if (i >= Start && i <= End) sb.append("0x");
                if (i >= Start && i <= End) sb.append(String.format("%02X", Script[i]));
                if(pushAmt > 0) {
                    if(bJustHitPush) {
                        bJustHitPush = false;
                        if (i >= Start && i <= End) sb.append(" (Push Amount)");
                    } else {
                        if (i >= Start && i <= End) sb.append(" (Push Byte)");
                    }
                    pushAmt--;
                }
            } else {
                if (i >= Start && i <= End) sb.append(byOpCode);
            }

            if(i==Highlight){
                sb.append(" <-- ").append(Message);
            }

            if (i >= Start && i <= End) sb.append(" \n");
        }
        sb.append("\n");

        return sb.toString();
    }

    public static byte[] NumberStringToBytes(String NumberString, boolean bMinimum32){
        if(NumberString.contains(".") || NumberString.toLowerCase().contains("e") || NumberString.contains("-") || NumberString.contains("+")) {
            return BytesUtil.FloatToByteArray(Float.parseFloat(NumberString));
        } else {
            try {
                int intToPush = Integer.parseInt(NumberString);
                if(intToPush <= Byte.MAX_VALUE && intToPush >= Byte.MIN_VALUE && !bMinimum32) {
                    return new byte[]{(byte)intToPush};
                } else {
                    return BytesUtil.NumberToByteArray32(intToPush);
                }
            } catch (NumberFormatException ignored) {
                return BytesUtil.NumberToByteArray64(Long.parseLong(NumberString));
            }
        }
    }

    public static byte[] ByteArrayStringToBytes(String ByteArrayString){
        System.out.println(ByteArrayString);

        String cleanedString = ByteArrayString.strip().replace("[","").replace("]","").replace(" ","").replace("\n","");

        String[] bytes = cleanedString.split(",");
        byte[] byteValues = new byte[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            byteValues[i] = Byte.parseByte(bytes[i]);
        }

        return byteValues;
    }
}
