package com.bradyrussell.uiscoin.script;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.Util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

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

    public boolean Initialize(byte[] Script, Enumeration<byte[]> StackValues) {
        this.Script = Script;
        Stack = new Stack<>();
        //validate
        for (Iterator<byte[]> it = StackValues.asIterator(); it.hasNext(); ) {
            byte[] b = it.next();
            Stack.push(b);
        }
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

    public void dumpStack() {
        Stack.elements().asIterator().forEachRemaining((byte[] bytes) -> System.out.println(Arrays.toString(bytes)));
    }

    public void dumpStackReadable() {
        Object[] toArray = Stack.toArray();

        for (int i = 0, toArrayLength = toArray.length; i < toArrayLength; i++) {
            byte[] stackElem = (byte[]) toArray[i];
            System.out.print(i + " ");
            Util.printBytesReadable(stackElem);
        }
    }


    public String getStackContents() {
        StringBuilder s = new StringBuilder();

        for (Object b : Stack.toArray()) {
            s.append(Arrays.toString((byte[]) b));
        }

        return s.toString();
    }

    public int getStackDepth() {
        return Stack.size();
    }

    public int getStackBytes() {
        int n = 0;
        for (Object b : Stack.toArray()) {
            n += ((byte[]) b).length;
        }
        return n;
    }

    // returns whether the script should continue
    public boolean Step() {
        if (InstructionCounter >= Script.length) return false;

        ScriptOperator scriptOperator = ScriptOperator.getByOpCode(Script[InstructionCounter++]);

        System.out.println("OP " + scriptOperator);

        if (scriptOperator != null) {
            switch (scriptOperator) {
                case REVERSE -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.pop();
                    byte[] B = new byte[A.length];

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        B[i] = A[(A.length - 1) - i];
                    }
                    System.out.println("Reverse top stack element");
                    Stack.push(B);
                    return true;
                }
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
                    System.out.println("Push " + NumberOfBytesToPush + " bytes onto the stack: " + Arrays.toString(bytes));
                    Stack.push(bytes);
                    return true;
                }
                case INSTRUCTION -> {
                    Stack.push(NumberToByteArray(InstructionCounter));
                    System.out.println("Push instruction counter onto the stack: " + InstructionCounter);
                    return true;
                }
                case NUMEQUAL -> {
                    if (Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    System.out.println("Push " + ByteArrayToNumber(A) + " == " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) == ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) == ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case BYTESEQUAL -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (A.length != B.length) {
                        System.out.println("Push " + Arrays.toString(A) + " == " + Arrays.toString(B) + " onto the stack: " + 0);
                        Stack.push(new byte[]{0});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != B[i]) {
                            System.out.println("Push " + Arrays.toString(A) + " == " + Arrays.toString(B) + " onto the stack: " + 0);
                            Stack.push(new byte[]{0});
                            return true;
                        }
                    }
                    System.out.println("Push " + Arrays.toString(A) + " == " + Arrays.toString(B) + " onto the stack: " + 1);
                    Stack.push(new byte[]{1});
                    return true;
                }
                case SHA512EQUAL -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] HashedB = Hash.getSHA512Bytes(B);

                    if (A.length != HashedB.length) {
                        System.out.println("Push " + Arrays.toString(A) + " == " + Arrays.toString(HashedB) + " onto the stack: " + 0);
                        Stack.push(new byte[]{0});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != HashedB[i]) {
                            System.out.println("Push " + Arrays.toString(A) + " == " + Arrays.toString(HashedB) + " onto the stack: " + 0);
                            Stack.push(new byte[]{0});
                            return true;
                        }
                    }
                    System.out.println("Push " + Arrays.toString(A) + " == " + Arrays.toString(HashedB) + " onto the stack: " + 1);
                    Stack.push(new byte[]{1});
                    return true;
                }
                case LENEQUAL -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    boolean equal = A.length == B.length;

                    System.out.println("Push " + A.length + " == " + B.length + " onto the stack: " + equal);

                    Stack.push(new byte[]{(byte) (equal ? 1 : 0)});
                    return true;
                }
                case LESSTHAN -> {
                    if (Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    System.out.println("Push " + ByteArrayToNumber(A) + " < " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) < ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) < ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case LESSTHANEQUAL -> {
                    if (Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    System.out.println("Push " + ByteArrayToNumber(A) + " <= " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) <= ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) <= ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case GREATERTHAN -> {
                    if (Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    System.out.println("Push " + ByteArrayToNumber(A) + " > " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) > ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) > ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case GREATERTHANEQUAL -> {
                    if (Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    System.out.println("Push " + ByteArrayToNumber(A) + " >= " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) >= ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) >= ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case NOTEQUAL -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (A.length != B.length) {
                        System.out.println("Push " + Arrays.toString(A) + " != " + Arrays.toString(B) + " onto the stack: " + 0);
                        Stack.push(new byte[]{1});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != B[i]) {
                            System.out.println("Push " + Arrays.toString(A) + " != " + Arrays.toString(B) + " onto the stack: " + 0);
                            Stack.push(new byte[]{1});
                            return true;
                        }
                    }
                    System.out.println("Push " + Arrays.toString(A) + " != " + Arrays.toString(B) + " onto the stack: " + 1);
                    Stack.push(new byte[]{0});
                    return true;
                }
                case NOTZERO -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.pop();

                    for (byte b : A) {
                        if (b != 0) {
                            System.out.println("Push " + Arrays.toString(A) + " != 0 onto the stack: " + 1);
                            Stack.push(new byte[]{1});
                            return true;
                        }
                    }
                    System.out.println("Push " + Arrays.toString(A) + " != 0 onto the stack: " + 0);
                    Stack.push(new byte[]{0});
                    return true;
                }
                case ADD -> {
                    if (Stack.size() < 2) {
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

                    System.out.println("Push " + ByteArrayToNumber(A) + " + " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) + ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) + ByteArrayToNumber(B)));
                    return true;
                }
                case SUBTRACT -> {
                    if (Stack.size() < 2) {
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

                    System.out.println("Push " + ByteArrayToNumber(A) + " - " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) - ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) - ByteArrayToNumber(B)));
                    return true;
                }
                case MULTIPLY -> {
                    if (Stack.size() < 2) {
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
                    System.out.println("Push " + ByteArrayToNumber(A) + " * " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) * ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) * ByteArrayToNumber(B)));
                    return true;
                }
                case DIVIDE -> {
                    if (Stack.size() < 2) {
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
                    System.out.println("Push " + ByteArrayToNumber(A) + " / " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) / ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) / ByteArrayToNumber(B)));
                    return true;
                }
                case ADDBYTES -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[i] = (byte) (A[i] + B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case SUBTRACTBYTES -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[i] = (byte) (A[i] - B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case MULTIPLYBYTES -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[i] = (byte) (A[i] * B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case DIVIDEBYTES -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if (B[i] == 0) {
                            bScriptFailed = true;
                            System.out.println("Divide by zero");
                            return false;
                        }
                        C[i] = (byte) (A[i] / B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case NEGATE -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.pop();

                    Stack.push(NumberToByteArray(-ByteArrayToNumber(A)));
                    return true;
                }
                case INVERT -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.pop();

                    Stack.push(NumberToByteArray((int) (1.0/(double) ByteArrayToNumber(A))));
                    return true;
                }
                case BITNOT -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        C[i] = (byte) ~A[i];
                    }

                    Stack.push(C);
                    return true;
                }
                case BITOR -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    if (A.length != B.length) return false;

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        C[i] = (byte) (A[i] | B[i]);
                    }

                    Stack.push(C);
                    return true;
                }
                case BITAND -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    if (A.length != B.length) return false;

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        C[i] = (byte) (A[i] & B[i]);
                    }

                    Stack.push(C);
                    return true;
                }
                case BITXOR -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    if (A.length != B.length) return false;

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        C[i] = (byte) (A[i] ^ B[i]);
                    }

                    Stack.push(C);
                    return true;
                }
                case APPEND -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length + B.length];

                    for (int i = 0; i < A.length + B.length; i++) {
                        if (i < A.length) C[i] = A[i];
                        else C[i] = B[i - A.length];
                    }

                    Stack.push(C);
                    return true;
                }
                case NOP -> {
                    return true;
                }
                case FALSE -> {
                    Stack.push(new byte[]{0});
                    return true;
                }
                case TRUE -> {
                    Stack.push(new byte[]{1});
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
                    if (Stack.size() < 2) {
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
                case CLEAR -> {
                    Stack.clear();
                    return true;
                }
                case CLONE -> {
                    for (Object o : Stack.toArray()) {
                        Stack.push((byte[]) o);
                    }
                    return true;
                }
                case FLIP -> {
                    Collections.reverse(Stack);
                    return true;
                }
                case SHIFTUP -> {
                    Collections.rotate(Stack, 1);
                    return true;
                }
                case SHIFTDOWN -> {
                    Collections.rotate(Stack, -1);
                    return true;
                }
                case PICK -> {
                    if (Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] A = Stack.pop();

                    if (A.length != 1) {
                        System.out.println("Expected a single byte on top of the stack");
                        bScriptFailed = true;
                        return false;
                    }

                    Stack.push(Stack.elementAt(A[0]));
                    return true;
                }
                case VERIFY -> {
                    byte[] bytes = Stack.peek();
                    System.out.println("Verify " + Arrays.toString(bytes) + " == true: " + (bytes.length == 1 && bytes[0] == 1));

                    bScriptFailed = !(bytes.length == 1 && bytes[0] == 1);
                    return false;
                }
                case RETURN -> {
                    bScriptFailed = true;
                    return false;
                }
                case LOCKTIMEVERIFY -> {

                }
                case SHA512 -> {
                    Stack.push(Hash.getSHA512Bytes(Stack.pop()));
                    return true;
                }
                case VERIFYSIG -> {
                    if (Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] PublicKey = Stack.pop();
                    byte[] Signature = Stack.pop();

                    Keys.SignedData signedData = new Keys.SignedData(PublicKey, Signature, Hash.getSHA512Bytes("What is the message??"));
                    try {
                        boolean verifySignedData = Keys.VerifySignedData(signedData);
                        bScriptFailed = !verifySignedData;
                        return false;
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
                        e.printStackTrace();
                        return false;
                    }

                }
                case CODESEPARATOR -> {

                }
                case LIMIT -> {
                    if (Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (B.length != 1) {
                        System.out.println("Expected a single byte on top of the stack");
                        bScriptFailed = true;
                        return false;
                    }

                    int NumberOfBytesToPush = B[0];
                    byte[] bytes = new byte[NumberOfBytesToPush];

                    if (NumberOfBytesToPush >= 0) System.arraycopy(A, 0, bytes, 0, NumberOfBytesToPush);

                    Stack.push(bytes);
                    return true;
                }
                case SPLIT -> {
                    if (Stack.size() < 1) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] A = Stack.pop();

                    for (byte b : A) {
                        Stack.push(new byte[]{b});
                    }

                    return true;
                }
                case COMBINE -> {
                    if (Stack.size() < 2) {
                        System.out.println("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] B = Stack.pop();

                    if (B.length != 1) {
                        System.out.println("Expected a single byte on top of the stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte NumberOfItemsToCombine = B[0];

                    ArrayList<Byte> bytes = new ArrayList<>();

                    for (int i = 0; i < NumberOfItemsToCombine; i++) {
                        byte[] A = Stack.pop();
                        for (int j = 0; j < A.length; j++) {
                            byte b = A[(A.length - 1) - j];
                            bytes.add(b);
                        }
                    }

                    Collections.reverse(bytes);

                    byte[] bytesArray = new byte[bytes.size()];
                    for (int i = 0; i < bytesArray.length; i++) {
                        bytesArray[i] = bytes.get(i);
                    }

                    Stack.push(bytesArray);

                    return true;
                }
                case LEN -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.peek();
                    Stack.push(NumberToByteArray(A.length));

                    return true;

                }
                case NOT -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }

                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[i] = (byte) ((A[i] == 0) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case OR -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[i] = (byte) ((A[i] == 1 || B[i] == 1) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case AND -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[i] = (byte) ((A[i] == 1 && B[i] == 1) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case XOR -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        System.out.println("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[i] = (byte) (((A[i] == 1 || B[i] == 1) && (!(A[i] == 1 && B[i] == 1))) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
            }
        }
        System.out.println("Not handled!");
        bScriptFailed = true;
        return false;
    }

}
