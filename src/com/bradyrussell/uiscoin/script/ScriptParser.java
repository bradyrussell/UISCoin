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
                String parameterCount = subToken.substring(0,subToken.indexOf(")"));

                if(parameterCount.equalsIgnoreCase("*")){
                    scriptBuilder.op(ScriptOperator.DEPTH);
                } else {
                    scriptBuilder.pushByte(Byte.parseByte(parameterCount));
                }

                if(Tokens.get(++i).startsWith("{")){ // code block push data
                    String InnerScript = Tokens.get(i).substring(1, Tokens.get(i).length() - 1).strip();
                    scriptBuilder.push(ScriptParser.CompileScriptTokensToBytecode(ScriptParser.GetTokensFromString(InnerScript, true)));
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

            if(Character.isJavaIdentifierPart(CurrentChar)) { // todo take numbers with . separately. for some reason trying to do that is taking entire chunks as a single token like .34290582 push 0.7439689
                while (i < scriptText.length()) {
                    char ch = scriptText.charAt(i++);
                    if (!Character.isJavaIdentifierPart(ch)) {
                        i-=2;
                        break;
                    }
                    currentToken.append(ch);
                }
                tokens.add(currentToken.toString());
            } else if(CurrentChar == '/' && scriptText.charAt(i+1) == '/' ) { // one line comments
                String startingAtComment = scriptText.substring(i);
                int endIndex = startingAtComment.indexOf("\n");
                //String comment = startingAtComment.substring(0, endIndex);
                //tokens.add(comment);
                i += endIndex;
            } else if(CurrentChar == '\"' || CurrentChar == '\'') { // string
                String startingAtComment = scriptText.substring(i+1);
                int endIndex = startingAtComment.indexOf(CurrentChar)+1;
                //todo escaped strings
                String string = startingAtComment.substring(0, endIndex);
                tokens.add(CurrentChar+string);
                i += endIndex;
            } else if(CurrentChar == '/' && scriptText.charAt(i+1) == '*' ) { // multi line comments
                String startingAtComment = scriptText.substring(i);
                int endIndex = startingAtComment.indexOf("*/")+1;
                //String comment = startingAtComment.substring(0, endIndex)+"/";
                //tokens.add(comment);
                i += endIndex;
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

}
