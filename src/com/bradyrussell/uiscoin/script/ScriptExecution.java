package com.bradyrussell.uiscoin.script;

import com.bradyrussell.uiscoin.Hash;

import java.util.Arrays;
import java.util.Stack;

public class ScriptExecution {
    public int InstructionCounter;
    public Stack<byte[]> Stack;
    public boolean bScriptFailed = false;

    public byte[] Script;

    public boolean Initialize(byte[] Script) {
        this.Script = Script;
        Stack = new Stack<>();
        //validate
        return true;
    }

    public static int ByteArrayToNumber(byte[] Bytes) {
        int n = 0;

        if (Bytes.length > 0) n |= Bytes[0] << 24;
        if (Bytes.length > 1) n |= (Bytes[1] & 0xFF) << 16;
        if (Bytes.length > 2) n |= (Bytes[2] & 0xFF) << 8;
        if (Bytes.length > 3) n |= (Bytes[3] & 0xFF);

        return n;
    }

    public static byte[] NumberToByteArray(int Number) {
        return new byte[]{(byte) (Number >> 24), (byte) (Number >> 16), (byte) (Number >> 8), (byte) Number};
    }

    public static boolean areBytesValidNumber(byte[] Bytes) {
        return Bytes.length <= 4;
    }

    public void dumpStack(){
        Stack.elements().asIterator().forEachRemaining((byte[] bytes)->{
            System.out.println(Arrays.toString(bytes));
        });
    }

    // returns whether the script should continue
    public boolean Step() {
        if(InstructionCounter >= Script.length) return false;

        ScriptOperator scriptOperator = ScriptOperator.getByOpCode(Script[InstructionCounter++]);

        System.out.println("OP "+scriptOperator);

        if (scriptOperator != null) {
            switch (scriptOperator) {

                case NULL -> {
                    System.out.println("Push null onto the stack");
                    Stack.push(new byte[]{});
                    return true;
                }
                case PUSH -> {
                    int NumberOfBytesToPush = Script[InstructionCounter++];
                    byte[] bytes = new byte[NumberOfBytesToPush];

                    for (int i = 0; i < NumberOfBytesToPush; i++) {
                        bytes[i] = Script[InstructionCounter++];
                    }
                    System.out.println("Push "+NumberOfBytesToPush+" bytes onto the stack: "+Arrays.toString(bytes));
                    Stack.push(bytes);
                    return true;
                }
                case INSTRUCTION -> {
                    Stack.push(NumberToByteArray(InstructionCounter));
                    System.out.println("Push instruction counter onto the stack: "+InstructionCounter);
                    return true;
                }
                case NUMEQUAL -> {
                    if(Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    System.out.println("Push "+ByteArrayToNumber(A)+" == "+ByteArrayToNumber(B)+" onto the stack: "+(ByteArrayToNumber(A) == ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) == ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case BYTESEQUAL -> {
                    if(Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (A.length != B.length) {
                        System.out.println("Push "+Arrays.toString(A)+" == "+Arrays.toString(B)+" onto the stack: "+0);
                        Stack.push(new byte[]{0});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != B[i]) {
                            System.out.println("Push "+Arrays.toString(A)+" == "+Arrays.toString(B)+" onto the stack: "+0);
                            Stack.push(new byte[]{0});
                            return true;
                        }
                    }
                    System.out.println("Push "+Arrays.toString(A)+" == "+Arrays.toString(B)+" onto the stack: "+1);
                    Stack.push(new byte[]{1});
                    return true;
                }
                case SHA512EQUAL -> {
                    if(Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] HashedB = Hash.getSHA512Bytes(B);

                    if (A.length != HashedB.length) {
                        System.out.println("Push "+Arrays.toString(A)+" == "+Arrays.toString(HashedB)+" onto the stack: "+0);
                        Stack.push(new byte[]{0});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != HashedB[i]) {
                            System.out.println("Push "+Arrays.toString(A)+" == "+Arrays.toString(HashedB)+" onto the stack: "+0);
                            Stack.push(new byte[]{0});
                            return true;
                        }
                    }
                    System.out.println("Push "+Arrays.toString(A)+" == "+Arrays.toString(HashedB)+" onto the stack: "+1);
                    Stack.push(new byte[]{1});
                    return true;
                }
                case ADD -> {
                    if(Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B)) {
                        System.out.println("Invalid inputs");
                        return false;
                    }

                    System.out.println("Push "+ByteArrayToNumber(A)+" + "+ByteArrayToNumber(B)+" onto the stack: "+(ByteArrayToNumber(A) + ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) + ByteArrayToNumber(B)));
                    return true;
                }
                case SUBTRACT -> {
                    if(Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B)) {
                        System.out.println("Invalid inputs");
                        return false;
                    }

                    System.out.println("Push "+ByteArrayToNumber(A)+" - "+ByteArrayToNumber(B)+" onto the stack: "+(ByteArrayToNumber(A) - ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) - ByteArrayToNumber(B)));
                    return true;
                }
                case MULTIPLY -> {
                    if(Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B)) {
                        System.out.println("Invalid inputs");
                        return false;
                    }
                    System.out.println("Push "+ByteArrayToNumber(A)+" * "+ByteArrayToNumber(B)+" onto the stack: "+(ByteArrayToNumber(A) * ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) * ByteArrayToNumber(B)));
                    return true;
                }
                case DIVIDE -> {
                    if(Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B) || ByteArrayToNumber(B) == 0) {
                        System.out.println("Invalid inputs");
                        return false;
                    }
                    System.out.println("Push "+ByteArrayToNumber(A)+" / "+ByteArrayToNumber(B)+" onto the stack: "+(ByteArrayToNumber(A) / ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) / ByteArrayToNumber(B)));
                    return true;
                }
                case ADDBYTES -> {
                    System.out.println("Not yet implemented");
                    return false;
                }
                case SUBTRACTBYTES -> {
                    System.out.println("Not yet implemented");
                    return false;
                }
                case MULTIPLYBYTES -> {
                    System.out.println("Not yet implemented");
                    return false;
                }
                case DIVIDEBYTES -> {
                    System.out.println("Not yet implemented");
                    return false;
                }
                case NOT -> {
                    if(Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        C[i] = (byte)~A[i];
                    }

                    Stack.push(C);
                    return true;
                }
                case OR -> {
                    if(Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    if(A.length != B.length) return false;

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        C[i] = (byte) (A[i] | B[i]);
                    }

                    Stack.push(C);
                    return true;
                }
                case AND -> {
                    if(Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    if(A.length != B.length) return false;

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        C[i] = (byte) (A[i] & B[i]);
                    }

                    Stack.push(C);
                    return true;
                }
                case APPEND -> {
                    if(Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length+B.length];

                    for(int i = 0; i < A.length+B.length; i++){
                        if(i < A.length) C[i] = A[i];
                        else C[i] = B[i-A.length];
                    }

                    Stack.push(C);
                    return true;
                }
                case NOP -> {
                    return true;
                }
                case FALSE -> {
                    Stack.push(new byte[]{ 0 });
                    return true;
                }
                case TRUE -> {
                    Stack.push(new byte[]{ 1 });
                    return true;
                }
                case DEPTH -> {
                    Stack.push(new byte[]{(byte) Stack.size()});
                    return true;
                }
                case DROP -> {
                    Stack.pop();
                    return true;
                }
                case DUP -> {
                    byte[] bytes = Stack.pop();
                    Stack.push(bytes);
                    Stack.push(bytes);
                    return true;
                }
                case SWAP -> {
                    if(Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    Stack.push(B);
                    Stack.push(A);
                    return true;
                }
                case VERIFY -> {
                    byte[] bytes = Stack.peek();
                    System.out.println("Verify "+Arrays.toString(bytes)+" == true: "+(bytes.length == 1 && bytes[0] == 1));

                    bScriptFailed = !(bytes.length == 1 && bytes[0] == 1);
                    return false;
                }
                case RETURN -> {
                    bScriptFailed = true;
                    return false;
                }
                case LOCKTIMEVERIFY -> {
                    return true;
                }
                case SHA512 -> {
                    Stack.push(Hash.getSHA512Bytes(Stack.pop()));
                    return true;
                }
                case CODESEPARATOR -> {
                    return true;
                }
            }
        }
        return false;
    }

}
