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
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

import static com.bradyrussell.uiscoin.Util.*;

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
                    if (CheckInsufficientStackSize(1)) return false;
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
                case FLAG -> {
                    // flags are ignored by the code
                    int FlagValue = Script[InstructionCounter++];
                    if(LogScriptExecution) Log.fine("Flag: "+FlagValue);
                    return true;
                }
                case BIGPUSH -> {
                    byte[] int32bytes = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        int32bytes[i] = Script[InstructionCounter++];
                    }

                    int NumberOfBytesToPush = Util.ByteArrayToNumber(int32bytes);

                    byte[] bytes = new byte[NumberOfBytesToPush];

                    for (int i = 0; i < NumberOfBytesToPush; i++) {
                        bytes[i] = Script[InstructionCounter++];
                    }
                    if(LogScriptExecution) Log.fine("BigPush " + NumberOfBytesToPush + " bytes onto the stack: " + Arrays.toString(bytes));
                    Stack.push(bytes);
                    return true;
                }
                case FLAGDATA -> {
                    // flags are ignored by code
                    int NumberOfBytesForFlag = Script[InstructionCounter++];
                    byte[] bytes = new byte[NumberOfBytesForFlag];

                    for (int i = 0; i < NumberOfBytesForFlag; i++) {
                        bytes[i] = Script[InstructionCounter++];
                    }
                    if(LogScriptExecution) Log.fine("FlagData " + NumberOfBytesForFlag + " bytes: " + Arrays.toString(bytes));
                    return true;
                }
                case TIME -> {
                    long epochSecond = Instant.now().getEpochSecond();
                    if(LogScriptExecution) Log.fine("Push Time: " + epochSecond);
                    Stack.push(Util.NumberToByteArray64(epochSecond));
                    return true;
                }
                case NUMEQUAL -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " == " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) == ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) == ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case BYTESEQUAL -> {
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    boolean equal = A.length == B.length;

                    if(LogScriptExecution) Log.fine("Push " + A.length + " == " + B.length + " onto the stack: " + equal);

                    Stack.push(new byte[]{(byte) (equal ? 1 : 0)});
                    return true;
                }
                case LESSTHAN -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " < " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) < ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) < ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case LESSTHANEQUAL -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " <= " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) <= ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) <= ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case GREATERTHAN -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " > " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) > ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) > ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case GREATERTHANEQUAL -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToNumber(A) + " >= " + ByteArrayToNumber(B) + " onto the stack: " + (ByteArrayToNumber(A) >= ByteArrayToNumber(B)));
                    Stack.push(new byte[]{ByteArrayToNumber(A) >= ByteArrayToNumber(B) ? (byte) 1 : (byte) 0});
                    return true;
                }
                case NOTEQUAL -> {
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(1)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if(B.length <= i) C[i] = A[i]; // treat overrun as if it was all 0s
                        C[i] = (byte) (A[i] + B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case SUBTRACTBYTES -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if(B.length <= i) C[i] = A[i]; // treat overrun as if it was all 0s
                        C[i] = (byte) (A[i] - B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case MULTIPLYBYTES -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if(B.length <= i) C[i] = A[i]; // treat overrun as if it was all 1s
                        C[i] = (byte) (A[i] * B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case DIVIDEBYTES -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if(B.length <= i) C[i] = A[i]; // treat overrun as if it was all 1s
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
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.pop();

                    Stack.push(NumberToByteArray(-ByteArrayToNumber(A)));
                    return true;
                }
                case INVERTFLOAT -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.pop();

                    Stack.push(NumberToByteArray((int) (1.0/(double) ByteArrayToNumber(A))));
                    return true;
                }
                case CONVERT8TO32 -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.pop();

                    if (CheckInsufficientBytes(A,1)) return false;

                    Stack.push(Util.NumberToByteArray(A[0]));
                    return true;
                }
                case CONVERT32TO8 -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.pop();

                    if (CheckInsufficientBytes(A,4)) return false;

                    Stack.push(new byte[]{(byte)Util.ByteArrayToNumber(A)});
                    return true;
                }
                case CONVERT64TO32 -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.pop();

                    if (CheckInsufficientBytes(A,8)) return false;

                    Stack.push(Util.NumberToByteArray((int) Util.ByteArrayToNumber64(A)));
                    return true;
                }
                case CONVERT32TO64 -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.pop();

                    if (CheckInsufficientBytes(A,4)) return false;

                    Stack.push(Util.NumberToByteArray64(Util.ByteArrayToNumber(A)));
                    return true;
                }
                case CONVERTFLOATTO32 -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.pop();

                    if (CheckInsufficientBytes(A,4)) return false;

                    Stack.push(Util.NumberToByteArray((int) Util.ByteArrayToFloat(A)));
                    return true;
                }
                case CONVERT32TOFLOAT -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.pop();

                    if (CheckInsufficientBytes(A,4)) return false;

                    Stack.push(Util.FloatToByteArray((float) Util.ByteArrayToNumber(A)));
                    return true;
                }
                case BITNOT -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        C[i] = (byte) ~A[i];
                    }

                    Stack.push(C);
                    return true;
                }
                case BITOR -> {
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(1)) return false;
                    Stack.pop();
                    return true;
                }
                case DUP -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] bytes = Stack.pop();
                    Stack.push(bytes);
                    Stack.push(bytes);
                    return true;
                }
                case SWAP -> {
                    if (CheckInsufficientStackSize(2)) return false;

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
                    if (CheckInsufficientStackSize(2)) return false;

                    byte[] A = Stack.pop();

                    if (CheckIncorrectNumberBytes(A,1)) return false;

                    Stack.push(Stack.elementAt(A[0]));
                    return true;
                }
                case VERIFY -> {
                    if (CheckInsufficientStackSize(1)) return false;
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
                    if (CheckInsufficientStackSize(1)) return false;
                    Stack.push(Hash.getSHA512Bytes(Stack.pop()));
                    return true;
                }
                case ENCRYPTAES -> {
                    if (CheckInsufficientStackSize(2)) return false;
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
                    if (CheckInsufficientStackSize(2)) return false;
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
                        Log.warning("SignatureVerificationMessage has not been set!");
                        bScriptFailed = true;
                        return false;
                    }
                    if (CheckInsufficientStackSize(2)) return false;

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
                    if (CheckInsufficientStackSize(2)) return false;

                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (CheckIncorrectNumberBytes(B,1)) return false;

                    int NumberOfBytesToPush = B[0];
                    byte[] bytes = new byte[NumberOfBytesToPush];

                    if (NumberOfBytesToPush >= 0) System.arraycopy(A, 0, bytes, 0, NumberOfBytesToPush);

                    Stack.push(bytes);
                    return true;
                }
                case SPLIT -> {
                    if (CheckInsufficientStackSize(1)) return false;

                    byte[] A = Stack.pop();

                    for (byte b : A) {
                        Stack.push(new byte[]{b});
                    }

                    return true;
                }
                case COMBINE -> {
                    if (CheckInsufficientStackSize(2)) return false;

                    byte[] B = Stack.pop();

                    if (CheckIncorrectNumberBytes(B,1)) return false;

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
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.peek();
                    Stack.push(NumberToByteArray(A.length));

                    return true;

                }
                case NOT -> {
                    if (CheckInsufficientStackSize(1)) return false;

                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[i] = (byte) ((A[i] == 0) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case OR -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if(B.length <= i) { // leave alone if not long enough
                            C[i] = A[i];
                            continue;
                        }
                        C[i] = (byte) ((A[i] == 1 || B[i] == 1) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case AND -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if(B.length <= i) { // 0 if not long enough
                            C[i] = 0;
                            continue;
                        }
                        C[i] = (byte) ((A[i] == 1 && B[i] == 1) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case XOR -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if(B.length <= i) { // leave alone if not long enough
                            C[i] = A[i];
                            continue;
                        }
                        C[i] = (byte) (((A[i] == 1 || B[i] == 1) && (!(A[i] == 1 && B[i] == 1))) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case NEGATEFLOAT -> {
                    if (CheckInsufficientStackSize(1)) return false;
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A)) {
                        Log.info("Invalid inputs");
                        return false;
                    }

                    float v = ByteArrayToFloat(A);
                    if(LogScriptExecution) Log.fine("Push -" + v + " onto the stack: " + (-v));
                    Stack.push(FloatToByteArray(-v ));
                    return true;
                }
                case ADDFLOAT -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B)) {
                        Log.info("Invalid inputs");
                        return false;
                    }

                    float v = ByteArrayToFloat(A);
                    if(LogScriptExecution) Log.fine("Push " + v + " + " + ByteArrayToFloat(B) + " onto the stack: " + (v + ByteArrayToFloat(B)));
                    Stack.push(FloatToByteArray(v + ByteArrayToFloat(B)));
                    return true;
                }
                case SUBTRACTFLOAT -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B)) {
                        Log.info("Invalid inputs");
                        return false;
                    }

                    float v = ByteArrayToFloat(A);
                    if(LogScriptExecution) Log.fine("Push " + v + " - " + ByteArrayToFloat(B) + " onto the stack: " + (v - ByteArrayToFloat(B)));
                    Stack.push(FloatToByteArray(v - ByteArrayToFloat(B)));
                    return true;
                }
                case MULTIPLYFLOAT -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B)) {
                        Log.info("Invalid inputs");
                        return false;
                    }

                    float v = ByteArrayToFloat(A);
                    if(LogScriptExecution) Log.fine("Push " + v + " * " + ByteArrayToFloat(B) + " onto the stack: " + (v * ByteArrayToFloat(B)));
                    Stack.push(FloatToByteArray(v * ByteArrayToFloat(B)));
                    return true;
                }
                case DIVIDEFLOAT -> {
                    if (CheckInsufficientStackSize(2)) return false;
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    float v = ByteArrayToFloat(B);
                    if (!areBytesValidNumber(A) || !areBytesValidNumber(B) || v == 0.f || !Float.isFinite(v)) {
                        Log.info("Invalid inputs");
                        return false;
                    }

                    if(LogScriptExecution) Log.fine("Push " + ByteArrayToFloat(A) + " / " + v + " onto the stack: " + (ByteArrayToFloat(A) / v));
                    Stack.push(FloatToByteArray(ByteArrayToFloat(A) / v));
                    return true;
                }
                case SHIFTELEMENTSRIGHT -> {
                    if (CheckInsufficientStackSize(1)) return false;

                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[(i+1)%A.length] = A[i];
                    }

                    Stack.push(C);

                    return true;
                }
                case SHIFTELEMENTSLEFT -> {
                    if (CheckInsufficientStackSize(1)) return false;

                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[(i-1)%A.length] = A[i];
                    }

                    Stack.push(C);

                    return true;
                }
                case VIRTUALSCRIPT -> {
                }
                case RESERVED -> {
                }
            }
        }
        Log.fine("Not handled!");
        bScriptFailed = true;
        return false;
    }

    private boolean CheckInsufficientStackSize(int MinimumSize) {
        if (Stack.size() < MinimumSize) {
            bScriptFailed = true;
            Log.info("Too few items in stack: "+Stack.size()+" / "+MinimumSize);
            return true;
        }
        return false;
    }

    private boolean CheckInsufficientBytes(byte[] Bytes, int MinimumSize) {
        if (Bytes.length < MinimumSize) {
            bScriptFailed = true;
            Log.info("Too few bytes in element on top of the stack: "+Bytes.length+" / "+MinimumSize);
            return true;
        }
        return false;
    }

    private boolean CheckIncorrectNumberBytes(byte[] Bytes, int Size) {
        if (Bytes.length == Size) {
            bScriptFailed = true;
            Log.info("Expected "+Size+" bytes for top stack element: "+Bytes.length+" != "+Size);
            return true;
        }
        return false;
    }

}
