package com.bradyrussell.uiscoin.script;

import com.bradyrussell.uiscoin.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

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

    private static byte[] TokenLiteralToBytes(String Token){
        String s = Token.strip();
        if(s.startsWith("0x")){ // hex push data
            return( Util.getBytesFromHexString(s.substring(2)));
        } else if(s.equalsIgnoreCase("true")){
            return new byte[]{0x01};
        } else if(s.equalsIgnoreCase("false")){
            return new byte[]{0x00};
        } else if(s.startsWith("[")){ // byte array push data
            return( ScriptUtil.ByteArrayStringToBytes(s));
        } else if(s.startsWith("{")){ // code block push data
            String InnerScript = s.substring(1, s.length() - 1).strip();
            return( ScriptParser.CompileScriptTokensToBytecode(ScriptParser.GetTokensFromString(InnerScript, true)));
        } else if(s.startsWith("\"") || s.startsWith("'")){ // string push data
            return( s.substring(1, s.length()-1)).getBytes(StandardCharsets.US_ASCII);
        } else {                                // interp as numeric push data
            // we have other ways to push bytes and need an easy way to push 32 bit low numbers
           return ( ScriptUtil.NumberStringToBytes(s, true));
        }
    }



    public static byte[] CompileScriptTokensToBytecode_2(ArrayList<String> Tokens) {



        return null;
    }

    public static byte[] CompileScriptTokensToBytecode(ArrayList<String> Tokens){
        HashMap<String, Integer> SymbolTable = new HashMap<>();
        int NextSymbol = 1; // we put a stack cookie in 0

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
            } else if (token.startsWith("*$")) { // addressof symbol
                String assignedSymbol = token.substring(2);

                if(assignedSymbol.length() < 1){
                    continue;
                }

                if(!SymbolTable.containsKey(assignedSymbol)) {
                    System.out.println("Error! Symbol \""+assignedSymbol+"\" was not defined in this scope!");
                    continue;
                } else {
                    scriptBuilder.pushByte(SymbolTable.get(assignedSymbol));
                }

            }  else if (token.startsWith("$")) { // declaration or assignment
                String symbol = token.substring(1);

                if(symbol.length() < 1){
                    continue;
                }

                //assignment
                if(Tokens.size() > i+1 && Tokens.get(i+1).startsWith("=")) {
                    if(Tokens.size() > i+2) {
                        if(!SymbolTable.containsKey(symbol)) {
                            System.out.println("Error! Symbol \""+symbol+"\" was not defined in this scope!");
                            i++;
                            continue;
                        }

                        String assignedValue = Tokens.get(i+2).strip();
                        if(assignedValue.startsWith("$")){ // variable / pick x
                            String assignedSymbol = assignedValue.substring(1);
                            // assign to value of another op
                            try{
                                // allow for direct $x addressing
                                scriptBuilder.push(ScriptUtil.NumberStringToBytes(assignedSymbol, false)).op(ScriptOperator.PICK);
                            } catch (NumberFormatException e) {
                                // get symbol address
                                scriptBuilder.pushByte(SymbolTable.get(assignedSymbol)).op(ScriptOperator.PICK);
                            }

                        } else if (assignedValue.startsWith("*$")) { // addressof symbol
                            String assignedSymbol = assignedValue.substring(2);

                            if(assignedSymbol.length() < 1){
                                continue;
                            }

                            if(!SymbolTable.containsKey(assignedSymbol)) {
                                System.out.println("Error! Symbol \""+assignedSymbol+"\" was not defined in this scope!");
                                continue;
                            } else {
                                scriptBuilder.pushByte(SymbolTable.get(assignedSymbol));
                            }

                        } else {
                            // assign to value of literal
                            scriptBuilder.push(TokenLiteralToBytes(assignedValue));
                        }
                        scriptBuilder.pushByte(SymbolTable.get(symbol)); // location to assign to
                        scriptBuilder.op(ScriptOperator.PUT);
                        i++;
                    }
                    i++;
                } else { // declaration
                    if(SymbolTable.containsKey(symbol)) {
                        System.out.println("Symbol \""+symbol+"\" was already defined in this scope!");
                    } else {
                        SymbolTable.put(symbol, NextSymbol++);
                        System.out.println("Assigned \""+symbol+"\" to $"+(NextSymbol-1)+".");
                    }
                }

                continue;
            } else if (token.startsWith("if")) { // if syntax
                if(Tokens.size() > i+1 && Tokens.get(i+1).startsWith("(")) { // if parameter
                    String parametersToken = Tokens.get(i+1).substring(1);
                    String parameters = parametersToken.substring(0,parametersToken.indexOf(")"));

                    if(Tokens.size() > i+2 && Tokens.get(i+2).startsWith("{")){ // code block push data
                        String InnerScript = Tokens.get(i+2).substring(1, Tokens.get(i+2).length() - 1).strip();
                        byte[] innerBytes = ScriptParser.CompileScriptTokensToBytecode(ScriptParser.GetTokensFromString(InnerScript, true));

                        // push below
                        if(parameters.startsWith("$")){ // variable / pick x
                            try{
                                // allow for direct $x addressing
                                scriptBuilder.push(ScriptUtil.NumberStringToBytes(parameters.substring(1), false)).op(ScriptOperator.PICK);
                            } catch (NumberFormatException e) {
                                // get symbol address
                                if(!SymbolTable.containsKey(parameters.substring(1))) continue;
                                scriptBuilder.pushByte(SymbolTable.get(parameters.substring(1))).op(ScriptOperator.PICK);
                            }
                        } else {
                            scriptBuilder.push(TokenLiteralToBytes(parameters));
                        }
                       // scriptBuilder.push(dataToPush);

                        scriptBuilder.op(ScriptOperator.NOT);
                        // not
                        //push length
                        scriptBuilder.pushByte(innerBytes.length+1).op(ScriptOperator.JUMPIF);
                        scriptBuilder.data(innerBytes);
                        //end jump

                        i += 2;
                    }

                }

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
                        if(s.startsWith("$")){ // variable / pick x
                            try{
                                // allow for direct $x addressing
                                scriptBuilder.push(ScriptUtil.NumberStringToBytes(s.substring(1), false)).op(ScriptOperator.PICK);
                            } catch (NumberFormatException e) {
                                // get symbol address
                                if(!SymbolTable.containsKey(s.substring(1))) continue;
                                scriptBuilder.pushByte(SymbolTable.get(s.substring(1))).op(ScriptOperator.PICK);
                            }
                        } else if (s.startsWith("*$")) { // addressof symbol
                            String assignedSymbol = s.substring(2);

                            if(assignedSymbol.length() < 1){
                                continue;
                            }

                            if(!SymbolTable.containsKey(assignedSymbol)) {
                                System.out.println("Error! Symbol \""+assignedSymbol+"\" was not defined in this scope!");
                                continue;
                            } else {
                                scriptBuilder.pushByte(SymbolTable.get(assignedSymbol));
                            }

                        }  else {
                            scriptBuilder.push(TokenLiteralToBytes(s));
                        }
                    }

                    scriptBuilder.data(new byte[]{PreviousOp});
                }
                continue;
            }

            ScriptOperator scriptOperator = ScriptOperator.valueOf(token.toUpperCase());

            switch (scriptOperator){
                // optimizer should eventually be able to reverse this for cases where its useless
                case SHIFTDOWN, SHIFTUP ->{ //rewrite to shiftnexcept(x, $0) to protect stack // add 2 for 2 stack cookies
                    if(SymbolTable.size() > 0) {
                        // todo we need to check for symbols beforehand
                        scriptBuilder.pushByte(scriptOperator == ScriptOperator.SHIFTDOWN ? -1 : 1).pushByte(0).op(ScriptOperator.PICK).pushByte(2).op(ScriptOperator.ADDBYTES).op(ScriptOperator.SHIFTNEXCEPT);
                    } else {
                        scriptBuilder.op(scriptOperator);
                    }

                }
                case SHIFTN -> { //rewrite to shiftnexcept($0) to protect stack // add 2 for 2 stack cookies
                    if(SymbolTable.size() > 0) {
                        // todo we need to check for symbols beforehand
                        scriptBuilder.pushByte(0).op(ScriptOperator.PICK).pushByte(2).op(ScriptOperator.ADDBYTES).op(ScriptOperator.SHIFTNEXCEPT);
                    } else {
                        scriptBuilder.op(scriptOperator);
                    }
                }
                case FLAG -> {
                    if(Tokens.get(++i).startsWith("0x")){ // hex byte flag
                        scriptBuilder.flag(Util.getBytesFromHexString(Tokens.get(i).substring(2))[0]);
                    } else { // interp as byte flag
                        scriptBuilder.flag(Byte.parseByte(Tokens.get(i)));
                    }
                }
                case FLAGDATA -> {
                    scriptBuilder.flagData(TokenLiteralToBytes(Tokens.get(++i)));
                }
                case BIGPUSH, PUSH -> {
                    String Token = Tokens.get(++i);

                    if(Token.startsWith("$")){ // variable / pick x // in case i need to pop a var onto the stack
                        scriptBuilder.push( ScriptUtil.NumberStringToBytes(Token.substring(1), false)).op(ScriptOperator.PICK);
                    } else {
                        scriptBuilder.push(TokenLiteralToBytes(Token));
                    }
                }
                default -> scriptBuilder.op(scriptOperator);
            }
        }
        int NumberOfVariables = SymbolTable.size();
        return Util.ConcatArray(Util.ConcatArray(InitializeStackSpaceForVariables(NumberOfVariables), scriptBuilder.get()), CleanupStackSpaceForVariables(NumberOfVariables));
    }

    public static byte[] InitializeStackSpaceForVariables(int NumberOfVariables) {
        if(NumberOfVariables <= 0) return new byte[0];

        byte[] Initializer = new byte[NumberOfVariables+5];
        for (int i = 0; i < NumberOfVariables; i++) {
            Initializer[i] = ScriptOperator.NULL.OPCode;
        }
        Initializer[NumberOfVariables] = ScriptOperator.DEPTH.OPCode; // surround the variables with the count on both ends
        Initializer[NumberOfVariables+1] = ScriptOperator.DUP.OPCode;  // at the end we can shift it back down and bytesequal it
        Initializer[NumberOfVariables+2] = ScriptOperator.SHIFTUP.OPCode; // to check the variable space wasnt messed with
        Initializer[NumberOfVariables+3] = ScriptOperator.FLAG.OPCode;
        Initializer[NumberOfVariables+4] = (byte) 0xFF;
        return Initializer;
    }

    public static byte[] CleanupStackSpaceForVariables(int NumberOfVariables) {
        if(NumberOfVariables <= 0) return new byte[0];

        return new byte[]{  ScriptOperator.FLAG.OPCode, (byte) 0xFF, ScriptOperator.FLIP.OPCode, ScriptOperator.DUP.OPCode, ScriptOperator.PUSH.OPCode, 0x01, (byte)-1, ScriptOperator.SWAP.OPCode, ScriptOperator.PUSH.OPCode, 0x01, 4, ScriptOperator.ADDBYTES.OPCode, ScriptOperator.DEPTH.OPCode, ScriptOperator.SWAP.OPCode, ScriptOperator.SUBTRACTBYTES.OPCode, ScriptOperator.SHIFTNEXCEPT.OPCode,
                            ScriptOperator.PUSH.OPCode, 0x01, (byte)NumberOfVariables, ScriptOperator.BYTESEQUAL.OPCode, ScriptOperator.VERIFY.OPCode, // bring down surrounding stack cookies, check that first one == expected
                            ScriptOperator.DUP.OPCode,       ScriptOperator.PUSH.OPCode, 0x01, (byte)NumberOfVariables, ScriptOperator.BYTESEQUAL.OPCode, ScriptOperator.VERIFY.OPCode, // verify the second one == expected after duping it
                            ScriptOperator.DROPN.OPCode, // drop the variable values
                            ScriptOperator.FLIP.OPCode
                 };
    }

    public static ArrayList<String> GetTokensFromString(String scriptText, boolean bGroupBracketsAndParentheses){
        ArrayList<String> tokens = new ArrayList<>();

        for (int i = 0; i < scriptText.length(); i++) {
            char CurrentChar = scriptText.charAt(i);

            StringBuilder currentToken = new StringBuilder();

            //todo check for -1 but not 1-1
            // or should i just treat it separately

            // if is numeric and end of token or the next char is numeric too. this is to avoid capturing 0x00
            if(isCharacterNumericToken(CurrentChar) && (i+1 >= scriptText.length() || isCharacterNumericToken(scriptText.charAt(i+1)))) { // numeric values,  will match 1.0e-4, 0, .0, 0., 0.0 but not 0x00, 1-1, 1.0-1.0 etc
                while (i < scriptText.length()) {
                    char ch = scriptText.charAt(i++);
                    if (!(isCharacterNumericToken(ch) || (ch == '-' && Character.toLowerCase(scriptText.charAt(i-2)) == 'e'))) {
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
