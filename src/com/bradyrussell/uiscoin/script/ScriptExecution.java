package com.bradyrussell.uiscoin.script;

import com.bradyrussell.uiscoin.Encryption;
import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.Util;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.logging.Logger;

import static com.bradyrussell.uiscoin.Util.ByteArrayToNumber;
import static com.bradyrussell.uiscoin.Util.NumberToByteArray;

public class ScriptExecution {
    private static final Logger Log = Logger.getLogger(ScriptExecution.class.getName());
    public int InstructionCounter;
    public Stack<byte[]> Stack;
    public boolean bScriptFailed = false;

    public boolean LogScriptExecution = false;

    private byte[] SignatureVerificationMessage = null; // we need a way to pass in the data for verifysig. i dont like this but...

    public byte[] Script;

    public boolean Initialize(byte[] Script) {
        this.Script = Script;
        Stack = new Stack<>();
        //validate
        if(LogScriptExecution) Log.info("Script initialized "+Script.length+" bytes with empty stack.");
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
        if(LogScriptExecution) Log.info("Script initialized "+Script.length+" bytes with "+getStackDepth()+" value"+(getStackDepth() == 1 ? "" : "s")+" on the stack.");
        if(LogScriptExecution) Log.fine(getStackContents());
        return true;
    }

    public void setSignatureVerificationMessage(byte[] signatureVerificationMessage) {
        SignatureVerificationMessage = signatureVerificationMessage;
    }



    public static boolean areBytesValidNumber(byte[] Bytes) {
        return Bytes.length <= 4;
    }

    public void dumpStack() {
        Stack.elements().asIterator().forEachRemaining((byte[] bytes) -> Log.fine(Arrays.toString(bytes)));
    }

    public void dumpStackReadable() {
        Object[] toArray = Stack.toArray();

        for (int i = 0, toArrayLength = toArray.length; i < toArrayLength; i++) {
            byte[] stackElem = (byte[]) toArray[i];
            Log.info(i + " ");
            Util.printBytesReadable(stackElem);
        }
    }

    public String getStackContents() {
        StringBuilder s = new StringBuilder();

        for (Object b : Stack.toArray()) {
            s.append(Arrays.toString((byte[]) b));
            s.append('\n');
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

        if(LogScriptExecution) Log.info("OP " + scriptOperator);

        if (scriptOperator != null) {
            switch (scriptOperator) {
                case REVERSE -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.pop();
                    byte[] B = new byte[A.length];

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        B[i] = A[(A.length - 1) - i];
                    }
                    if(LogScriptExecution) Log.fine("Reverse top stack element");
                    Stack.push(B);
                    return true;
                }
                case NULL -> {
                    if(LogScriptExecution) Log.fine("Push null onto the stack");
                    Stack.push(new byte[]{});
                    return true;
                }
                case PUSH -> {
                    int NumberOfBytesToPush = Script[InstructionCounter++];
                    byte[] bytes = new byte[NumberOfBytesToPush];

                    for (int i = 0; i < NumberOfBytesToPush; i++) {
                        bytes[i] = Script[InstructionCounter++];
                    }
                    if(LogScriptExecution) Log.fine("Push " + NumberOfBytesToPush + " bytes onto the stack: " + Arrays.toString(bytes));
                    Stack.push(bytes);
                    return true;
                }
                case INSTRUCTION -> {
                    Stack.push(NumberToByteArray(InstructionCounter));
                    if(LogScriptExecution) Log.fine("Push instruction counter onto the stack: " + InstructionCounter);
                    return true;
                }
                case NUMEQUAL -> {
                    if (Stack.size() < 2) {
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " == " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) == ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) == ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case BYTESEQUAL -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (A.length != B.length) {
                        if(LogScriptExecution) Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(B) + " onto the stack: " + 0);
                        Stack.push(new byte[]{0});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != B[i]) {
                            if(LogScriptExecution)  Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(B) + " onto the stack: " + 0);
                            Stack.push(new byte[]{0});
                            return true;
                        }
                    }
                    if(LogScriptExecution)  Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(B) + " onto the stack: " + 1);
                    Stack.push(new byte[]{1});
                    return true;
                }
                case SHA512EQUAL -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] HashedB = Hash.getSHA512Bytes(B);

                    if (A.length != HashedB.length) {
                        if(LogScriptExecution) Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(HashedB) + " onto the stack: " + 0);
                        Stack.push(new byte[]{0});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != HashedB[i]) {
                            if(LogScriptExecution) Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(HashedB) + " onto the stack: " + 0);
                            Stack.push(new byte[]{0});
                            return true;
                        }
                    }
                    if(LogScriptExecution) Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(HashedB) + " onto the stack: " + 1);
                    Stack.push(new byte[]{1});
                    return true;
                }
                case LENEQUAL -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    boolean equal = A.length == B.length;

                    if(LogScriptExecution) Log.fine("Push " + A.length + " == " + B.length + " onto the stack: " + equal);

                    Stack.push(new byte[]{(byte) (equal ? 1 : 0)});
                    return true;
                }
                case LESSTHAN -> {
                    if (Stack.size() < 2) {
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " < " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) < ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) < ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case LESSTHANEQUAL -> {
                    if (Stack.size() < 2) {
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " <= " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) <= ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) <= ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case GREATERTHAN -> {
                    if (Stack.size() < 2) {
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " > " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) > ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) > ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case GREATERTHANEQUAL -> {
                    if (Stack.size() < 2) {
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " >= " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) >= ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) >= ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case NOTEQUAL -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (A.length != B.length) {
                        if(LogScriptExecution) Log.fine("Push " + Arrays.toString(A) + " != " + Arrays.toString(B) + " onto the stack: " + 0);
                        Stack.push(new byte[]{1});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != B[i]) {
                            if(LogScriptExecution) Log.fine("Push " + Arrays.toString(A) + " != " + Arrays.toString(B) + " onto the stack: " + 0);
                            Stack.push(new byte[]{1});
                            return true;
                        }
                    }
                    if(LogScriptExecution) Log.fine("Push " + Arrays.toString(A) + " != " + Arrays.toString(B) + " onto the stack: " + 1);
                    Stack.push(new byte[]{0});
                    return true;
                }
                case NOTZERO -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.pop();

                    for (byte b : A) {
                        if (b != 0) {
                            if(LogScriptExecution) Log.fine("Push " + Arrays.toString(A) + " != 0 onto the stack: " + 1);
                            Stack.push(new byte[]{1});
                            return true;
                        }
                    }
                    if(LogScriptExecution) Log.fine("Push " + Arrays.toString(A) + " != 0 onto the stack: " + 0);
                    Stack.push(new byte[]{0});
                    return true;
                }
                case ADD -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B)) {
                        Log.info("Invalid inputs");
                        return false;
                    }

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " + " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) + ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) + ByteArrayToNumber(B)));
                    return true;
                }
                case SUBTRACT -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B)) {
                        Log.info("Invalid inputs");
                        return false;
                    }

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " - " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) - ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) - ByteArrayToNumber(B)));
                    return true;
                }
                case MULTIPLY -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B)) {
                        Log.info("Invalid inputs");
                        return false;
                    }
                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " * " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) * ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) * ByteArrayToNumber(B)));
                    return true;
                }
                case DIVIDE -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B) || ByteArrayToNumber(B) == 0) {
                        Log.info("Invalid inputs");
                        return false;
                    }
                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " / " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) / ByteArrayToNumber(B)));
                    Stack.push(NumberToByteArray(ByteArrayToNumber(A) / ByteArrayToNumber(B)));
                    return true;
                }
                case ADDBYTES -> {
                    if (Stack.size() < 2) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if (B[i] == 0) {
                            bScriptFailed = true;
                            Log.info("Divide by zero");
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
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.pop();

                    Stack.push(NumberToByteArray(-ByteArrayToNumber(A)));
                    return true;
                }
                case INVERT -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.pop();

                    Stack.push(NumberToByteArray((int) (1.0/(double) ByteArrayToNumber(A))));
                    return true;
                }
                case BITNOT -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
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
                    if (Stack.size() < 1) {
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] bytes = Stack.pop();
                    Stack.push(bytes);
                    Stack.push(bytes);
                    return true;
                }
                case SWAP -> {
                    if (Stack.size() < 2) {
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] A = Stack.pop();

                    if (A.length != 1) {
                        Log.info("Expected a single byte on top of the stack");
                        bScriptFailed = true;
                        return false;
                    }

                    Stack.push(Stack.elementAt(A[0]));
                    return true;
                }
                case VERIFY -> {
                    if (Stack.size() < 1) {
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] bytes = Stack.pop();
                    if(LogScriptExecution)        Log.fine("Verify " + Arrays.toString(bytes) + " == true: " + (bytes.length == 1 && bytes[0] == 1));

                    if(!(bytes.length == 1 && bytes[0] == 1)) {
                        bScriptFailed = true;
                        return false;
                    }

                    if(LogScriptExecution)        Log.fine("Verify confirmed, continuing...");
                    return true;
                }
                case RETURN -> {
                    bScriptFailed = true;
                    return false;
                }
                case SHA512 -> {
                    Stack.push(Hash.getSHA512Bytes(Stack.pop()));
                    return true;
                }
                case ENCRYPTAES -> {
                    if (Stack.size() < 2) {
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] Message = Stack.pop();
                    byte[] Key = Stack.pop();

                    try {
                        Stack.push(Encryption.Encrypt(Message,Key));
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                        e.printStackTrace();
                        bScriptFailed = true;
                        return false;
                    }

                    return true;
                }
                case DECRYPTAES -> {
                    if (Stack.size() < 2) {
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }
                    byte[] Message = Stack.pop();
                    byte[] Key = Stack.pop();

                    try {
                        Stack.push(Encryption.Decrypt(Message,Key));
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                        e.printStackTrace();
                        bScriptFailed = true;
                        return false;
                    }

                    return true;
                }
                case VERIFYSIG -> {
                    if(SignatureVerificationMessage == null) {
                        Log.warning("SignatureVerificationMessage has not been set");
                        bScriptFailed = true;
                        return false;
                    }
                    if (Stack.size() < 2) {
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] PublicKey = Stack.pop();
                    byte[] Signature = Stack.pop();

                    Keys.SignedData signedData = new Keys.SignedData(PublicKey, Signature, SignatureVerificationMessage);
                    try {
                        boolean verifySignedData = Keys.VerifySignedData(signedData);
                        if(LogScriptExecution)          Log.fine("Signature verification "+(verifySignedData? "successful.":"failed!"));
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
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (B.length != 1) {
                        Log.info("Expected a single byte on top of the stack");
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
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] B = Stack.pop();

                    if (B.length != 1) {
                        Log.info("Expected a single byte on top of the stack");
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
                        Log.info("Too few items in stack");
                        return false;
                    }
                    byte[] A = Stack.peek();
                    Stack.push(NumberToByteArray(A.length));

                    return true;

                }
                case NOT -> {
                    if (Stack.size() < 1) {
                        bScriptFailed = true;
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
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
                        Log.info("Too few items in stack");
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
        Log.fine("Not handled!");
        bScriptFailed = true;
        return false;
    }

}
