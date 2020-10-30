package com.bradyrussell.uiscoin.script;

import com.bradyrussell.uiscoin.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ScriptParser {

    public static int GetClosingCharacterIndex(String Search, char Opening, char Closing, int OpeningBraceIndex) {
        int braceDepth = 0;
        int targetDepth = -1;

        for (int i = 0; i < Search.length(); i++) {
            if(Search.charAt(i) == Opening) {
                if(i == OpeningBraceIndex) targetDepth = braceDepth;
                braceDepth++;
            }
            if(Search.charAt(i) == Closing) {
                braceDepth--;
                if(targetDepth == braceDepth) return i;
            }
        }
        return -1;
    }


    public static byte[] CompileScriptTokensToBytecode(ArrayList<String> Tokens){
        ScriptBuilder scriptBuilder = new ScriptBuilder(Tokens.size()+1024);

        for (int i = 0; i < Tokens.size(); i++) {
            String token = Tokens.get(i);
            System.out.print(i+": ");
            System.out.println(token);

            if(token.startsWith("0x")) { // hex data
                scriptBuilder.fromHexString(token.substring(2));
                continue;
            } else if (Character.isDigit(token.charAt(0))) { // numeric data
                scriptBuilder.data(ScriptUtil.NumberStringToBytes(token, false));
                continue;
            } else if (token.startsWith("[")) { // byte array data
                scriptBuilder.data(ScriptUtil.ByteArrayStringToBytes(token));
                continue;
            } else if(token.startsWith("(")) { // function syntax
                String subToken = token.substring(1); // 1){code}
                String parameters = subToken.substring(0,subToken.indexOf(")"));

                if(Tokens.size() > i+1 && Tokens.get(i+1).startsWith("{")){ // code block push data
                    i++;
                    if(parameters.equalsIgnoreCase("*")){
                        scriptBuilder.op(ScriptOperator.DEPTH);
                    } else {
                        scriptBuilder.pushByte(Byte.parseByte(parameters));
                    }

                    String InnerScript = Tokens.get(i).substring(1, Tokens.get(i).length() - 1).strip();
                    scriptBuilder.push(ScriptParser.CompileScriptTokensToBytecode(ScriptParser.GetTokensFromString(InnerScript, true)));
                } else { // parameters to the preceding identifier
                    String[] parametersArray = parameters.split(",");

                    int BufferPosition = scriptBuilder.buffer.position() - 1;
                    byte PreviousOp = scriptBuilder.buffer.get(BufferPosition);
                    scriptBuilder.buffer.put(BufferPosition, ScriptOperator.NOP.OPCode); // replace previous op with NOP
                    scriptBuilder.buffer.position(BufferPosition);  // should be overwritten anyways

                    for (String s : parametersArray) {
                        s = s.strip();
                        if(s.startsWith("0x")){ // hex push data
                            scriptBuilder.push(Util.getBytesFromHexString(s.substring(2)));
                        } else if(s.startsWith("$")){ // variable / pick x
                            scriptBuilder.push(ScriptUtil.NumberStringToBytes(s.substring(1), false)).op(ScriptOperator.PICK);
                        } else if(s.startsWith("[")){ // byte array push data
                            scriptBuilder.push(ScriptUtil.ByteArrayStringToBytes(s));
                        } else if(s.startsWith("{")){ // code block push data
                            String InnerScript = s.substring(1, s.length() - 1).strip();
                            scriptBuilder.push(ScriptParser.CompileScriptTokensToBytecode(ScriptParser.GetTokensFromString(InnerScript, true)));
                        } else if(s.startsWith("\"") || s.startsWith("'")){ // string push data
                            scriptBuilder.pushASCIIString(s.substring(1, s.length()-1));
                        } else {                                // interp as numeric push data
                            // we have other ways to push bytes and need an easy way to push 32 bit low numbers
                            scriptBuilder.push(ScriptUtil.NumberStringToBytes(s, true));
                        }
                    }

                    scriptBuilder.data(new byte[]{PreviousOp});
                }
                continue;
            }

            ScriptOperator scriptOperator = ScriptOperator.valueOf(token.toUpperCase());

            switch (scriptOperator){
                case FLAG -> {
                    if(Tokens.get(++i).startsWith("0x")){ // hex byte flag
                        scriptBuilder.flag(Util.getBytesFromHexString(Tokens.get(i).substring(2))[0]);
                    } else { // interp as byte flag
                        scriptBuilder.flag(Byte.parseByte(Tokens.get(i)));
                    }
                }
                case FLAGDATA -> {
                    if(Tokens.get(++i).startsWith("0x")){ // hex flag data
                        scriptBuilder.flagData(Util.getBytesFromHexString(Tokens.get(i).substring(2)));
                    } else if(Tokens.get(i).startsWith("[")){ // byte array flag data
                        scriptBuilder.flagData(ScriptUtil.ByteArrayStringToBytes(Tokens.get(i)));
                    } else if(Tokens.get(i).startsWith("\"")){ // string flag data
                        scriptBuilder.flagData(Tokens.get(i).substring(1, Tokens.get(i).length()-1).getBytes(StandardCharsets.US_ASCII));
                    } else {                                // interp as numeric flag data
                        scriptBuilder.flagData(ScriptUtil.NumberStringToBytes(Tokens.get(i), false));
                    }
                }
                case BIGPUSH, PUSH -> {
                    if(Tokens.get(++i).startsWith("0x")){ // hex push data
                        scriptBuilder.push(Util.getBytesFromHexString(Tokens.get(i).substring(2)));
                    } else if(Tokens.get(i).startsWith("[")){ // byte array push data
                        scriptBuilder.push(ScriptUtil.ByteArrayStringToBytes(Tokens.get(i)));
                    } else if(Tokens.get(i).startsWith("{")){ // code block push data
                        String InnerScript = Tokens.get(i).substring(1, Tokens.get(i).length() - 1).strip();
                        scriptBuilder.push(ScriptParser.CompileScriptTokensToBytecode(ScriptParser.GetTokensFromString(InnerScript, true)));
                    } else if(Tokens.get(i).startsWith("\"") || Tokens.get(i).startsWith("'")){ // string push data
                        scriptBuilder.pushASCIIString(Tokens.get(i).substring(1, Tokens.get(i).length()-1));
                    } else {                                // interp as numeric push data
                        // we have other ways to push bytes and need an easy way to push 32 bit low numbers
                        scriptBuilder.push(ScriptUtil.NumberStringToBytes(Tokens.get(i), true));
                    }
                }
                default -> scriptBuilder.op(scriptOperator);
            }
        }
        return scriptBuilder.get();
    }

    public static ArrayList<String> GetTokensFromString(String scriptText, boolean bGroupBracketsAndParentheses){
        ArrayList<String> tokens = new ArrayList<>();

        for (int i = 0; i < scriptText.length(); i++) {
            char CurrentChar = scriptText.charAt(i);

            StringBuilder currentToken = new StringBuilder();

            if(isCharacterNumericToken(CurrentChar) && (i+1 > scriptText.length() || isCharacterNumericToken(scriptText.charAt(i+1)))) { // numeric values,  will match 0, .0, 0., 0.0 but not 0x00 etc
                while (i < scriptText.length()) {
                    char ch = scriptText.charAt(i++);
                    if (!isCharacterNumericToken(ch)) {
                        i-=2;
                        break;
                    }
                    currentToken.append(ch);
                }
                tokens.add(currentToken.toString());
            } else if(Character.isJavaIdentifierPart(CurrentChar)) { // typical identifiers
                while (i < scriptText.length()) {
                    char ch = scriptText.charAt(i++);
                    if (!Character.isJavaIdentifierPart(ch)) {
                        i-=2;
                        break;
                    }
                    currentToken.append(ch);
                }
                tokens.add(currentToken.toString());
            } else if(scriptText.length() > i+1 && CurrentChar == '/' && scriptText.charAt(i+1) == '/' ) { // one line comments
                String startingAtComment = scriptText.substring(i);
                int endIndex = startingAtComment.indexOf("\n");
                //String comment = startingAtComment.substring(0, endIndex);
                //tokens.add(comment);
                i += Math.max(endIndex, 0);
            } else if(CurrentChar == '\"' || CurrentChar == '\'') { // string
                String startingAtComment = scriptText.substring(i+1);
                int endIndex = startingAtComment.indexOf(CurrentChar)+1;
                //todo escaped strings
                String string = startingAtComment.substring(0, endIndex);
                tokens.add(CurrentChar+string);
                i += Math.max(endIndex, 0);
            } else if(scriptText.length() > i+1 && CurrentChar == '/' && scriptText.charAt(i+1) == '*' ) { // multi line comments
                String startingAtComment = scriptText.substring(i);
                int endIndex = startingAtComment.indexOf("*/")+1;
                //String comment = startingAtComment.substring(0, endIndex)+"/";
                //tokens.add(comment);
                i += Math.max(endIndex, 0);
            } else if(bGroupBracketsAndParentheses && CurrentChar == '[') { // brackets
                String startingAtComment = scriptText.substring(i);
                int endIndex = startingAtComment.indexOf(']')+1;
                String string = startingAtComment.substring(0, endIndex);
                tokens.add(string);
                i += endIndex;
            } else if(bGroupBracketsAndParentheses && CurrentChar == '{') { // braces
                String startingAtComment = scriptText.substring(i);
                int endIndex = GetClosingCharacterIndex(startingAtComment, '{', '}', 0) +1;
                String string = startingAtComment.substring(0, endIndex);
                tokens.add(string);
                i += endIndex;
            } else if(bGroupBracketsAndParentheses && CurrentChar == '(') { // parentheses
                String startingAtComment = scriptText.substring(i);
                int endIndex = GetClosingCharacterIndex(startingAtComment, '(', ')', 0) +1;
                String string = startingAtComment.substring(0, endIndex);
                tokens.add(string);
                i += endIndex;
            }

            else if(Character.isWhitespace(CurrentChar)){
                // do nothing
            } else {
                tokens.add(String.valueOf(CurrentChar));
            }
        }
        return tokens;
    }

    private static boolean isCharacterNumericToken(char InputChar){
        return Character.isDigit(InputChar) || InputChar == '.' || Character.toLowerCase(InputChar) == 'e';
    }
}
