/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.script;

import static com.bradyrussell.uiscoin.BytesUtil.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.Encryption;
import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.script.exception.*;

public class ScriptExecution {
    private static final Logger Log = Logger.getLogger(ScriptExecution.class.getName());
    public int InstructionCounter;
    public Stack<byte[]> Stack;
    public boolean bScriptFailed = false;
    public final int MaximumStepsAllowed = 1000;
    public boolean bExtendedFlowControl = false;
    public boolean bThrowExceptionOnFailure = false;

    public boolean LogScriptExecution = false;

    private byte[] SignatureVerificationMessage = null; // we need a way to pass in the data for verifysig. i don't like this but...
    //private long BlockTime = 0; // need a way to pass in blocktime for TIME operator

    public byte[] Script;

    public boolean initialize(byte[] Script) {
        this.Script = Script;
        Stack = new Stack<>();
        //validate
        if (LogScriptExecution) Log.info("Script initialized " + Script.length + " bytes with empty stack.");
        return true;
    }

    public boolean initialize(byte[] Script, Enumeration<byte[]> StackValues) {
        this.Script = Script;
        Stack = new Stack<>();
        //validate
        for (Iterator<byte[]> it = StackValues.asIterator(); it.hasNext(); ) {
            byte[] b = it.next();
            Stack.push(b);
        }
        if (LogScriptExecution)
            Log.info("Script initialized " + Script.length + " bytes with " + getStackDepth() + " value" + (getStackDepth() == 1 ? "" : "s") + " on the stack.");
        if (LogScriptExecution) Log.fine(getStackContents());
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

    public int Steps = 0;

    // returns whether the script should continue
    public boolean step() throws ScriptEmptyStackException, ScriptInvalidParameterException, ScriptInvalidException, ScriptUnsupportedOperationException, ScriptFailedException {
        if (InstructionCounter >= Script.length) return false;

        if(Steps++ > MaximumStepsAllowed) throw new ScriptUnsupportedOperationException("Script exceeded the instruction limit.");

        ScriptOperator scriptOperator = ScriptOperator.getByOpCode(Script[InstructionCounter++]);

        if (LogScriptExecution) Log.info("OP " + scriptOperator);

        if (scriptOperator != null) {
            switch (scriptOperator) {
                case REVERSE -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();
                    byte[] B = new byte[A.length];

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        B[i] = A[(A.length - 1) - i];
                    }
                    if (LogScriptExecution) Log.fine("Reverse top stack element");
                    Stack.push(B);
                    return true;
                }
                case EXPONENT -> {
                    checkInsufficientStackSize(2);
                    byte[] ExponentBytes = Stack.pop();
                    checkIncorrectNumberBytes(ExponentBytes, 4);
                    byte[] BaseBytes = Stack.pop();
                    checkIncorrectNumberBytes(BaseBytes, 4);

                    float Base = byteArrayToFloat(BaseBytes);
                    float Exponent = byteArrayToFloat(ExponentBytes);

                    float Result = (float) Math.pow(Base, Exponent);

                    if (LogScriptExecution) {
                        Log.fine("Push " + Base + " to the " + Exponent + " power onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case ROOT -> {
                    checkInsufficientStackSize(2);
                    byte[] ExponentBytes = Stack.pop();
                    checkIncorrectNumberBytes(ExponentBytes, 4);
                    byte[] BaseBytes = Stack.pop();
                    checkIncorrectNumberBytes(BaseBytes, 4);

                    float Base = byteArrayToFloat(BaseBytes);
                    float Exponent = byteArrayToFloat(ExponentBytes);

                    float Result = (float) Math.pow(Base, 1.0 / Exponent);

                    if (LogScriptExecution) {
                        Log.fine("Push " + Exponent + " root of " + Base + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case ABS -> {
                    checkInsufficientStackSize(1);

                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 4);

                    long iA;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    long Result = Math.abs(iA);

                    if (LogScriptExecution) {
                        Log.fine("Push abs " + iA + " onto the stack: " + Result);
                    }
                    Stack.push(bReturnLong ? numberToByteArray64(Result) : numberToByteArray32((int) Result));
                    return true;
                }
                case FABS -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = Math.abs(X);

                    if (LogScriptExecution) {
                        Log.fine("Push abs " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case LOG -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = (float) Math.log(X);

                    if (LogScriptExecution) {
                        Log.fine("Push log " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case LOGN -> {
                    checkInsufficientStackSize(2);
                    byte[] NBytes = Stack.pop();
                    checkIncorrectNumberBytes(NBytes, 4);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float N = byteArrayToFloat(NBytes);

                    float Result = (float) (Math.log(N) / Math.log(X));

                    if (LogScriptExecution) {
                        Log.fine("Push log " + N + " of " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case SIN -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = (float) Math.sin(X);

                    if (LogScriptExecution) {
                        Log.fine("Push sin " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case COS -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = (float) Math.cos(X);

                    if (LogScriptExecution) {
                        Log.fine("Push cos " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case TAN -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = (float) Math.tan(X);

                    if (LogScriptExecution) {
                        Log.fine("Push tan " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case ASIN -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = (float) Math.asin(X);

                    if (LogScriptExecution) {
                        Log.fine("Push asin " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case ACOS -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = (float) Math.acos(X);

                    if (LogScriptExecution) {
                        Log.fine("Push acos " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case ATAN -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = (float) Math.atan(X);

                    if (LogScriptExecution) {
                        Log.fine("Push atan " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case FLOOR -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = (float) Math.floor(X);

                    if (LogScriptExecution) {
                        Log.fine("Push floor " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case CEIL -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = (float) Math.ceil(X);

                    if (LogScriptExecution) {
                        Log.fine("Push ceil " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case ROUND -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    float Result = (float) Math.round(X);

                    if (LogScriptExecution) {
                        Log.fine("Push round " + X + " onto the stack: " + Result);
                    }

                    Stack.push(floatToByteArray(Result));
                    return true;
                }
                case ISNAN -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    boolean Result = Float.isNaN(X);

                    if (LogScriptExecution) {
                        Log.fine("Push isnan " + X + " onto the stack: " + Result);
                    }

                    Stack.push(new byte[]{Result ? (byte)1 : (byte)0});
                    return true;
                }
                case ISINF -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    boolean Result = Float.isInfinite(X);

                    if (LogScriptExecution) {
                        Log.fine("Push isinfinite " + X + " onto the stack: " + Result);
                    }

                    Stack.push(new byte[]{Result ? (byte)1 : (byte)0});
                    return true;
                }
                case ISFIN -> {
                    checkInsufficientStackSize(1);
                    byte[] XBytes = Stack.pop();
                    checkIncorrectNumberBytes(XBytes, 4);

                    float X = byteArrayToFloat(XBytes);
                    boolean Result = Float.isFinite(X);

                    if (LogScriptExecution) {
                        Log.fine("Push isfinite " + X + " onto the stack: " + Result);
                    }

                    Stack.push(new byte[]{Result ? (byte)1 : (byte)0});
                    return true;
                }
                case NULL -> {
                    if (LogScriptExecution) Log.fine("Push null onto the stack");
                    Stack.push(new byte[]{});
                    return true;
                }
                case PUSH -> {
                    checkScriptEndsBefore(1);

                    int NumberOfBytesToPush = Script[InstructionCounter++];

                    if (NumberOfBytesToPush <= 0) {
                        Log.warning("Invalid PUSH amount specified: " + NumberOfBytesToPush);
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] bytes = new byte[NumberOfBytesToPush];

                    checkScriptEndsBefore(NumberOfBytesToPush);

                    for (int i = 0; i < NumberOfBytesToPush; i++) {
                        bytes[i] = Script[InstructionCounter++];
                    }
                    if (LogScriptExecution)
                        Log.fine("Push " + NumberOfBytesToPush + " bytes onto the stack: " + Arrays.toString(bytes));
                    Stack.push(bytes);
                    return true;
                }
                case INSTRUCTION -> {
                    Stack.push(numberToByteArray32(InstructionCounter));
                    if (LogScriptExecution) Log.fine("Push instruction counter onto the stack: " + InstructionCounter);
                    return true;
                }
                case FLAG -> {
                    // flags are ignored by the code
                    checkScriptEndsBefore(1);

                    int FlagValue = Script[InstructionCounter++];
                    if (LogScriptExecution) Log.fine("Flag: " + FlagValue);
                    return true;
                }
                case BIGPUSH -> {
                    checkScriptEndsBefore(4);

                    byte[] int32bytes = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        int32bytes[i] = Script[InstructionCounter++];
                    }

                    int NumberOfBytesToPush = BytesUtil.byteArrayToNumber32(int32bytes);

                    if (NumberOfBytesToPush <= 0) {
                        Log.warning("Invalid BIGPUSH amount specified: " + NumberOfBytesToPush);
                        bScriptFailed = true;
                        return false;
                    }

                    checkScriptEndsBefore(NumberOfBytesToPush);

                    byte[] bytes = new byte[NumberOfBytesToPush];

                    for (int i = 0; i < NumberOfBytesToPush; i++) {
                        bytes[i] = Script[InstructionCounter++];
                    }
                    if (LogScriptExecution)
                        Log.fine("BigPush " + NumberOfBytesToPush + " bytes onto the stack: " + Arrays.toString(bytes));
                    Stack.push(bytes);
                    return true;
                }
                case FLAGDATA -> {
                    // flags are ignored by code
                    checkScriptEndsBefore(1);

                    int NumberOfBytesForFlag = Script[InstructionCounter++];

                    if (NumberOfBytesForFlag <= 0) {
                        Log.warning("Invalid FLAGDATA amount specified: " + NumberOfBytesForFlag);
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] bytes = new byte[NumberOfBytesForFlag];

                    checkScriptEndsBefore(NumberOfBytesForFlag);

                    for (int i = 0; i < NumberOfBytesForFlag; i++) {
                        bytes[i] = Script[InstructionCounter++];
                    }
                    if (LogScriptExecution)
                        Log.fine("FlagData " + NumberOfBytesForFlag + " bytes: " + Arrays.toString(bytes));
                    return true;
                }
                case TIME -> {
                   /* long epochSecond = Instant.now().getEpochSecond();
                    if(LogScriptExecution) Log.fine("Push Time: " + epochSecond);
                    Stack.push(Util.NumberToByteArray64(epochSecond));*/
                    // time can conditionally break scripts. for this to work it needs to return block time once included in block
                    Log.warning("Time operator is disabled");
                    bScriptFailed = true;
                    throw new ScriptUnsupportedOperationException("The operator "+scriptOperator+" is disabled."+
                            "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                            "\n" + ScriptUtil.PrintStack(Stack.elements(), null));
                }
                case NUMEQUAL -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkInsufficientBytes(B, 1);
                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 1);

                    long iA, iB;

                    if(A.length == 1){
                        iA = A[0];
                    } else if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                    }

                    if(B.length == 1){
                        iB = B[0];
                    } else if (B.length < 8) {
                        iB = byteArrayToNumber32(B);
                    } else {
                        iB = byteArrayToNumber64(B);
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " == " + iB + " onto the stack: " + (iA == iB));
                    Stack.push(new byte[]{iA == iB ? (byte) 1 : (byte) 0});

                    return true;
                }
                case BYTESEQUAL -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (A.length != B.length) {
                        if (LogScriptExecution)
                            Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(B) + " onto the stack: " + 0);
                        Stack.push(new byte[]{0});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != B[i]) {
                            if (LogScriptExecution)
                                Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(B) + " onto the stack: " + 0);
                            Stack.push(new byte[]{0});
                            return true;
                        }
                    }
                    if (LogScriptExecution)
                        Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(B) + " onto the stack: " + 1);
                    Stack.push(new byte[]{1});
                    return true;
                }
                case SHA512EQUAL -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] HashedB = Hash.getSHA512Bytes(B);

                    if (A.length != HashedB.length) {
                        if (LogScriptExecution)
                            Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(HashedB) + " onto the stack: " + 0);
                        Stack.push(new byte[]{0});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != HashedB[i]) {
                            if (LogScriptExecution)
                                Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(HashedB) + " onto the stack: " + 0);
                            Stack.push(new byte[]{0});
                            return true;
                        }
                    }
                    if (LogScriptExecution)
                        Log.fine("Push " + Arrays.toString(A) + " == " + Arrays.toString(HashedB) + " onto the stack: " + 1);
                    Stack.push(new byte[]{1});
                    return true;
                }
                case LENEQUAL -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    boolean equal = A.length == B.length;

                    if (LogScriptExecution)
                        Log.fine("Push " + A.length + " == " + B.length + " onto the stack: " + equal);

                    Stack.push(new byte[]{(byte) (equal ? 1 : 0)});
                    return true;
                }
                case LESSTHAN -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkInsufficientBytes(B, 1);
                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 1);

                    long iA, iB;

                    if(A.length == 1){
                        iA = A[0];
                    } else if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                    }

                    if(B.length == 1){
                        iB = B[0];
                    } else if (B.length < 8) {
                        iB = byteArrayToNumber32(B);
                    } else {
                        iB = byteArrayToNumber64(B);
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " < " + iB + " onto the stack: " + (iA < iB));
                    Stack.push(new byte[]{iA < iB ? (byte) 1 : (byte) 0});

                    return true;
                }
                case LESSTHANEQUAL -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkInsufficientBytes(B, 1);
                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 1);

                    long iA, iB;

                    if(A.length == 1){
                        iA = A[0];
                    } else if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                    }

                    if(B.length == 1){
                        iB = B[0];
                    } else if (B.length < 8) {
                        iB = byteArrayToNumber32(B);
                    } else {
                        iB = byteArrayToNumber64(B);
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " <= " + iB + " onto the stack: " + (iA <= iB));
                    Stack.push(new byte[]{iA <= iB ? (byte) 1 : (byte) 0});

                    return true;
                }
                case GREATERTHAN -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkInsufficientBytes(B, 1);
                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 1);

                    long iA, iB;

                    if(A.length == 1){
                        iA = A[0];
                    } else if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                    }

                    if(B.length == 1){
                        iB = B[0];
                    } else if (B.length < 8) {
                        iB = byteArrayToNumber32(B);
                    } else {
                        iB = byteArrayToNumber64(B);
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " > " + iB + " onto the stack: " + (iA > iB));
                    Stack.push(new byte[]{iA > iB ? (byte) 1 : (byte) 0});

                    return true;
                }
                case GREATERTHANEQUAL -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkInsufficientBytes(B, 1);
                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 1);

                    long iA, iB;

                    if(A.length == 1){
                        iA = A[0];
                    } else if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                    }

                    if(B.length == 1){
                        iB = B[0];
                    } else if (B.length < 8) {
                        iB = byteArrayToNumber32(B);
                    } else {
                        iB = byteArrayToNumber64(B);
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " >= " + iB + " onto the stack: " + (iA >= iB));
                    Stack.push(new byte[]{iA >= iB ? (byte) 1 : (byte) 0});

                    return true;
                }
                case NOTEQUAL -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    if (A.length != B.length) {
                        if (LogScriptExecution)
                            Log.fine("Push " + Arrays.toString(A) + " != " + Arrays.toString(B) + " onto the stack: " + 0);
                        Stack.push(new byte[]{1});
                        return true;
                    }
                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        if (A[i] != B[i]) {
                            if (LogScriptExecution)
                                Log.fine("Push " + Arrays.toString(A) + " != " + Arrays.toString(B) + " onto the stack: " + 0);
                            Stack.push(new byte[]{1});
                            return true;
                        }
                    }
                    if (LogScriptExecution)
                        Log.fine("Push " + Arrays.toString(A) + " != " + Arrays.toString(B) + " onto the stack: " + 1);
                    Stack.push(new byte[]{0});
                    return true;
                }
                case NOTZERO -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    for (byte b : A) {
                        if (b != 0) {
                            if (LogScriptExecution)
                                Log.fine("Push " + Arrays.toString(A) + " != 0 onto the stack: " + 1);
                            Stack.push(new byte[]{1});
                            return true;
                        }
                    }
                    if (LogScriptExecution) Log.fine("Push " + Arrays.toString(A) + " != 0 onto the stack: " + 0);
                    Stack.push(new byte[]{0});
                    return true;
                }
                case GET -> { // todo allow any numeric operands rather than just int32?
                    checkInsufficientStackSize(3);

                    byte[] LengthBytes = Stack.pop();
                    checkInsufficientBytes(LengthBytes, 4);
                    int Length = byteArrayToNumber32(LengthBytes);

                    byte[] BeginIndexBytes = Stack.pop();
                    checkInsufficientBytes(BeginIndexBytes, 4);
                    int BeginIndex = byteArrayToNumber32(BeginIndexBytes);

                    byte[] StackElementIndexBytes = Stack.pop();
                    checkInsufficientBytes(StackElementIndexBytes, 4);
                    int StackElementIndex = byteArrayToNumber32(StackElementIndexBytes);

                    checkInsufficientStackSize(StackElementIndex+1);
                    checkNumberIsInRange(StackElementIndex, 0, Stack.size()-1);

                    byte[] StackElement = Stack.elementAt(StackElementIndex);

                    checkNumberIsInRange(Length, 0, StackElement.length);
                    checkNumberIsInRange(BeginIndex, 0, StackElement.length);

                    checkInsufficientBytes(StackElement, BeginIndex+Length);

                    byte[] Copy = new byte[Length];
                    System.arraycopy(StackElement, BeginIndex, Copy, 0, Length);

                    Stack.push(Copy);
                    return true;
                }
                case SET -> {
                    checkInsufficientStackSize(4);

                    byte[] LengthBytes = Stack.pop();
                    checkInsufficientBytes(LengthBytes, 4);
                    int Length = byteArrayToNumber32(LengthBytes);

                    byte[] BeginIndexBytes = Stack.pop();
                    checkInsufficientBytes(BeginIndexBytes, 4);
                    int BeginIndex = byteArrayToNumber32(BeginIndexBytes);

                    byte[] DestinationIndexBytes = Stack.pop();
                    checkInsufficientBytes(DestinationIndexBytes, 4);
                    int StackElementIndex = byteArrayToNumber32(DestinationIndexBytes);

                    byte[] Source = Stack.pop();

                    checkInsufficientStackSize(StackElementIndex+1);
                    checkNumberIsInRange(StackElementIndex, 0, Stack.size()-1);

                    //byte[] DestinationOriginal = Stack.elementAt(StackElementIndex);

                    checkInsufficientBytes(Source, Length);
                    checkNumberIsInRange(Length, 0, Source.length);
                    checkNumberIsInRange(BeginIndex+Length, 0, Stack.elementAt(StackElementIndex).length);

                    //byte[] Result = new byte[BeginIndex+Length];
                    // todo bounds checks
                    System.arraycopy(Source, 0, Stack.elementAt(StackElementIndex), BeginIndex, Length);

                    //Stack.push(Copy);
                    return true;
                }
                case COPY -> {
                    checkInsufficientStackSize(5);

                    byte[] LengthBytes = Stack.pop();
                    checkInsufficientBytes(LengthBytes, 4);
                    int Length = byteArrayToNumber32(LengthBytes);

                    byte[] DestBeginIndexBytes = Stack.pop();
                    checkInsufficientBytes(DestBeginIndexBytes, 4);
                    int DestBeginIndex = byteArrayToNumber32(DestBeginIndexBytes);

                    byte[] DestStackElementBytes = Stack.pop();
                    checkInsufficientBytes(DestStackElementBytes, 4);
                    int DestStackElementIndex = byteArrayToNumber32(DestStackElementBytes);

                    byte[] SourceBeginIndexBytes = Stack.pop();
                    checkInsufficientBytes(SourceBeginIndexBytes, 4);
                    int SourceBeginIndex = byteArrayToNumber32(SourceBeginIndexBytes);

                    byte[] SourceStackElementBytes = Stack.pop();
                    checkInsufficientBytes(SourceStackElementBytes, 4);
                    int SourceStackElementIndex = byteArrayToNumber32(SourceStackElementBytes);

                    // get the source

                    checkInsufficientStackSize(SourceStackElementIndex+1);
                    checkNumberIsInRange(SourceStackElementIndex, 0, Stack.size()-1);

                    byte[] SourceStackElement = Stack.elementAt(SourceStackElementIndex);

                    checkNumberIsInRange(Length, 0, SourceStackElement.length);
                    checkNumberIsInRange(SourceBeginIndex, 0, SourceStackElement.length);

                    checkInsufficientBytes(SourceStackElement, SourceBeginIndex+Length);

                    // get the dest
                    checkInsufficientStackSize(DestStackElementIndex+1);
                    checkNumberIsInRange(SourceStackElementIndex, 0, Stack.size()-1);

                    byte[] DestStackElement = Stack.elementAt(DestStackElementIndex);

                    checkNumberIsInRange(Length, 0, DestStackElement.length);
                    checkNumberIsInRange(DestBeginIndex, 0, DestStackElement.length);

                    checkInsufficientBytes(DestStackElement, DestBeginIndex+Length);

                    System.arraycopy(SourceStackElement, SourceBeginIndex, DestStackElement, DestBeginIndex, Length);
                    return true;

                }
                case ALLOC -> {
                    checkInsufficientStackSize(1);

                    byte[] LengthBytes = Stack.pop();
                    checkInsufficientBytes(LengthBytes, 4);
                    int Length = byteArrayToNumber32(LengthBytes);

                    checkNumberIsInRange(Length, 0, Short.MAX_VALUE);

                    Stack.push(new byte[Length]);
                    return true;
                }
                case THIS -> { // allows recursion
                    if(!bExtendedFlowControl) {
                        bScriptFailed = true;
                        throw new ScriptUnsupportedOperationException("The THIS operation is only available with Extended Flow Control enabled.");
                    }
                    Stack.push(Script);
                    return true;
                }
                case ADD -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkInsufficientBytes(B, 4);
                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 4);

                    long iA, iB;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (B.length < 8) {
                        iB = byteArrayToNumber32(B);
                    } else {
                        iB = byteArrayToNumber64(B);
                        bReturnLong = true;
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " + " + iB + " onto the stack: " + (iA + iB));
                    Stack.push(bReturnLong ? numberToByteArray64(iA + iB) : numberToByteArray32((int) (iA + iB)));
                    return true;
                }
                case SUBTRACT -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkInsufficientBytes(B, 4);
                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 4);

                    long iA, iB;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (B.length < 8) {
                        iB = byteArrayToNumber32(B);
                    } else {
                        iB = byteArrayToNumber64(B);
                        bReturnLong = true;
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " - " + iB + " onto the stack: " + (iA - iB));
                    Stack.push(bReturnLong ? numberToByteArray64(iA - iB) : numberToByteArray32((int) (iA - iB)));
                    return true;
                }
                case MULTIPLY -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkInsufficientBytes(B, 4);
                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 4);

                    long iA, iB;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (B.length < 8) {
                        iB = byteArrayToNumber32(B);
                    } else {
                        iB = byteArrayToNumber64(B);
                        bReturnLong = true;
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " * " + iB + " onto the stack: " + (iA * iB));
                    Stack.push(bReturnLong ? numberToByteArray64(iA * iB) : numberToByteArray32((int) (iA * iB)));
                    return true;
                }
                case DIVIDE -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkInsufficientBytes(B, 4);
                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 4);

                    long iA, iB;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (B.length < 8) {
                        iB = byteArrayToNumber32(B);
                    } else {
                        iB = byteArrayToNumber64(B);
                        bReturnLong = true;
                    }

                    if (iB == 0) {
                        bScriptFailed = true;
                        Log.info("Divide by zero");
                        return false;
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " / " + iB + " onto the stack: " + (iA / iB));
                    Stack.push(bReturnLong ? numberToByteArray64(iA / iB) : numberToByteArray32((int) (iA / iB)));
                    return true;
                }
                case MODULO -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkInsufficientBytes(B, 4);
                    byte[] A = Stack.pop();
                    checkInsufficientBytes(A, 4);

                    long iA, iB;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (B.length < 8) {
                        iB = byteArrayToNumber32(B);
                    } else {
                        iB = byteArrayToNumber64(B);
                        bReturnLong = true;
                    }

                    if (iB == 0) {
                        bScriptFailed = true;
                        Log.info("Divide by zero");
                        return false;
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " % " + iB + " onto the stack: " + (iA % iB));
                    Stack.push(bReturnLong ? numberToByteArray64(iA % iB) : numberToByteArray32((int) (iA % iB)));
                    return true;
                }
                case ADDBYTES -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if (B.length <= i) C[i] = A[i]; // treat overrun as if it was all 0s
                        else C[i] = (byte) (A[i] + B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case SUBTRACTBYTES -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if (B.length <= i) C[i] = A[i]; // treat overrun as if it was all 0s
                        else C[i] = (byte) (A[i] - B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case MULTIPLYBYTES -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if (B.length <= i) C[i] = A[i]; // treat overrun as if it was all 1s
                        else C[i] = (byte) (A[i] * B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case DIVIDEBYTES -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if (B.length <= i) C[i] = A[i]; // treat overrun as if it was all 1s
                        else if (B[i] == 0) {
                            bScriptFailed = true;
                            Log.info("Divide by zero");
                            return false;
                        } else C[i] = (byte) (A[i] / B[i]);
                    }

                    Stack.push(C);

                    return true;
                }
                case NEGATE -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    checkInsufficientBytes(A, 4);

                    long iA;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = byteArrayToNumber32(A);
                    } else {
                        iA = byteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (LogScriptExecution) Log.fine("Push -(" + iA + ")" + " onto the stack: " + -(iA));
                    Stack.push(bReturnLong ? numberToByteArray64(-iA) : numberToByteArray32((int) (-iA)));
                    return true;
                }
                case INVERTFLOAT -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    checkInsufficientBytes(A, 4);

                    Stack.push(floatToByteArray(1.f / byteArrayToFloat(A)));
                    return true;
                }
                case CONVERT8TO32 -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    checkInsufficientBytes(A, 1);

                    Stack.push(BytesUtil.numberToByteArray32(A[0]));
                    return true;
                }
                case CONVERT32TO8 -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    checkInsufficientBytes(A, 4);

                    Stack.push(new byte[]{(byte) BytesUtil.byteArrayToNumber32(A)});
                    return true;
                }
                case CONVERT64TO32 -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    checkInsufficientBytes(A, 8);

                    Stack.push(BytesUtil.numberToByteArray32((int) BytesUtil.byteArrayToNumber64(A)));
                    return true;
                }
                case CONVERT32TO64 -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    checkInsufficientBytes(A, 4);

                    Stack.push(BytesUtil.numberToByteArray64(BytesUtil.byteArrayToNumber32(A)));
                    return true;
                }
                case CONVERTFLOATTO32 -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    checkInsufficientBytes(A, 4);

                    Stack.push(BytesUtil.numberToByteArray32((int) BytesUtil.byteArrayToFloat(A)));
                    return true;
                }
                case CONVERT32TOFLOAT -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    checkInsufficientBytes(A, 4);

                    Stack.push(BytesUtil.floatToByteArray((float) BytesUtil.byteArrayToNumber32(A)));
                    return true;
                }
                case BITNOT -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        C[i] = (byte) ~A[i];
                    }

                    Stack.push(C);
                    return true;
                }
                case BITOR -> {
                    checkInsufficientStackSize(2);
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
                    checkInsufficientStackSize(2);
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
                    checkInsufficientStackSize(2);
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
                    checkInsufficientStackSize(2);
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
                    checkInsufficientStackSize(1);
                    Stack.pop();
                    return true;
                }
                case DUP -> {
                    checkInsufficientStackSize(1);
                    byte[] bytes = Stack.pop();
                    Stack.push(bytes);
                    Stack.push(bytes);
                    return true;
                }
                case SWAP -> {
                    checkInsufficientStackSize(2);

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
                    checkInsufficientStackSize(2);

                    byte[] A = Stack.pop();

                    checkIncorrectNumberBytes(A, 1);

                    checkInsufficientStackSize(A[0] + 1);

                    if (A[0] < 0) {
                        Log.info("Negative index for pick");
                        bScriptFailed = true;
                        return false;
                    }

                    Stack.push(Stack.elementAt(A[0]));
                    return true;
                }
                case PUT -> {
                    checkInsufficientStackSize(2);

                    byte[] A = Stack.pop();

                    checkIncorrectNumberBytes(A, 1);

                    byte[] B = Stack.pop();

                    checkInsufficientStackSize(A[0] + 1);

                    if (A[0] < 0) {
                        Log.info("Negative index for replace");
                        bScriptFailed = true;
                        return false;
                    }

                    Stack.ensureCapacity(A[0]+1);
                    Stack.insertElementAt(B, A[0]);
                    Stack.remove(A[0]+1);

                    return true;
                }
                case VERIFY -> {
                    checkInsufficientStackSize(1);
                    byte[] bytes = Stack.pop();
                    if (LogScriptExecution)
                        Log.fine("Verify " + Arrays.toString(bytes) + " == true: " + (bytes.length == 1 && bytes[0] == 1));

                    if (!(bytes.length == 1 && bytes[0] == 1)) {
                        bScriptFailed = true;
                        if(bThrowExceptionOnFailure) throw new ScriptFailedException(ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Script failed here!")+"\n"+ScriptUtil.PrintStack(Stack.elements(), null));
                        return false;
                    }

                    if (LogScriptExecution) Log.fine("Verify confirmed, continuing...");
                    return true;
                }
                case RETURN -> {
                    bScriptFailed = true;
                    return false;
                }
                case RETURNIF -> {
                    checkInsufficientStackSize(1);
                    byte[] bytes = Stack.pop();
                    if (LogScriptExecution)
                        Log.fine("ReturnIf " + Arrays.toString(bytes) + " == true: " + (bytes.length == 1 && bytes[0] == 1));

                    if (bytes.length == 1 && bytes[0] == 1) {
                        bScriptFailed = true;
                        return false;
                    }

                    if (LogScriptExecution) Log.fine("ReturnIf passed, continuing...");
                    return true;
                }
                case SHA512 -> {
                    checkInsufficientStackSize(1);
                    Stack.push(Hash.getSHA512Bytes(Stack.pop()));
                    return true;
                }
                case ZIP -> {
                    checkInsufficientStackSize(1);
                    Stack.push(BytesUtil.zipBytes(Stack.pop()));
                    return true;
                }
                case UNZIP -> {
                    checkInsufficientStackSize(1);
                    Stack.push(BytesUtil.unzipBytes(Stack.pop()));
                    return true;
                }
                case ENCRYPTAES -> {
                    checkInsufficientStackSize(2);
                    byte[] Key = Stack.pop();
                    byte[] Message = Stack.pop();

                    try {
                        Stack.push(Encryption.encrypt(Message, Key));
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                        e.printStackTrace();
                        bScriptFailed = true;
                        return false;
                    }

                    return true;
                }
                case DECRYPTAES -> {
                    checkInsufficientStackSize(2);
                    byte[] Key = Stack.pop();
                    byte[] Message = Stack.pop();

                    try {
                        Stack.push(Encryption.decrypt(Message, Key));
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                        e.printStackTrace();
                        bScriptFailed = true;
                        return false;
                    }

                    return true;
                }
                case VERIFYSIG -> {
                    if (SignatureVerificationMessage == null) {
                        Log.warning("SignatureVerificationMessage has not been set!");
                        bScriptFailed = true;
                        return false;
                    }
                    checkInsufficientStackSize(2);

                    byte[] PublicKey = Stack.pop();
                    byte[] Signature = Stack.pop();

                    Keys.SignedData signedData = new Keys.SignedData(PublicKey, Signature, SignatureVerificationMessage);
                    try {
                        boolean verifySignedData = Keys.verifySignedData(signedData);
                        if (LogScriptExecution)
                            Log.fine("Signature verification " + (verifySignedData ? "successful." : "failed!"));
                        bScriptFailed = !verifySignedData;
                        return false;
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
                        e.printStackTrace();
                        bScriptFailed = true;
                        return false;
                    }

                }
                case CODESEPARATOR -> {

                }
                //noinspection deprecation
                case VERIFYMULTISIG -> {
                    /*
                     * input stack:
                     * [signatures...]
                     * [number of required signatures]
                     * [public keys]
                     * [number of public keys]
                     *
                     */
                    if (SignatureVerificationMessage == null) {
                        Log.warning("SignatureVerificationMessage has not been set!");
                        bScriptFailed = true;
                        return false;
                    }

                    checkInsufficientStackSize(1);

                    byte[] NumPublicKeysBytes = Stack.pop();
                    checkIncorrectNumberBytes(NumPublicKeysBytes, 1);
                    byte NumPublicKeys = NumPublicKeysBytes[0];

                    checkInsufficientStackSize(NumPublicKeys+1);

                    ArrayList<byte[]> PublicKeys = new ArrayList<>();

                    for (int i = 0; i < NumPublicKeys; i++) {
                        PublicKeys.add(Stack.pop());
                    }

                    byte[] NumSignaturesBytes = Stack.pop();
                    checkIncorrectNumberBytes(NumSignaturesBytes, 1);
                    byte NumSignatures = NumSignaturesBytes[0];

                    ArrayList<byte[]> Signatures = new ArrayList<>();

                    checkInsufficientStackSize(NumSignatures);

                    for (int i = 0; i < NumSignatures; i++) {
                        Signatures.add(Stack.pop());
                    }

                    ArrayList<byte[]> VerifiedPublicKeys = new ArrayList<>();

                    for (byte[] signature : Signatures) {
                        boolean bVerified = false;
                        for (byte[] publicKey : PublicKeys) {
                            if(VerifiedPublicKeys.contains(publicKey)) continue; // skip verified public keys, otherwise one keyholder could submit multiple sigs
                            try {
                                boolean verifySignedData = Keys.verifySignedData(new Keys.SignedData(publicKey, signature, SignatureVerificationMessage));
                                if(verifySignedData) {
                                    bVerified = true;
                                    VerifiedPublicKeys.add(publicKey);
                                    break;//next signature
                                }
                            } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
                                e.printStackTrace();
                                bScriptFailed = true;
                                return false;
                            }
                        }
                        if(!bVerified) {
                            bScriptFailed = true;
                            return false;
                        }
                    }
                    // success
                    return false;
                }
                case LIMIT -> {
                    checkInsufficientStackSize(2);

                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    checkIncorrectNumberBytes(B, 1);

                    int NumberOfBytesToPush = B[0];

                    if (NumberOfBytesToPush < 0) NumberOfBytesToPush = 0;
                    if (NumberOfBytesToPush > A.length) NumberOfBytesToPush = A.length;

                    byte[] bytes = new byte[NumberOfBytesToPush];

                    if (NumberOfBytesToPush >= 1) System.arraycopy(A, 0, bytes, 0, NumberOfBytesToPush);

                    Stack.push(bytes);
                    return true;
                }
                case SPLIT -> {
                    checkInsufficientStackSize(1);

                    byte[] A = Stack.pop();

                    for (byte b : A) {
                        Stack.push(new byte[]{b});
                    }

                    return true;
                }
                case COMBINE -> {
                    checkInsufficientStackSize(2);

                    byte[] B = Stack.pop();

                    checkIncorrectNumberBytes(B, 1);

                    byte NumberOfItemsToCombine = B[0];

                    checkInsufficientStackSize(NumberOfItemsToCombine);

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
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.peek();
                    Stack.push(numberToByteArray32(A.length));

                    return true;

                }
                case NOT -> {
                    checkInsufficientStackSize(1);

                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[i] = (byte) ((A[i] == 0) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case OR -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if (B.length <= i) { // leave alone if not long enough
                            C[i] = A[i];
                            continue;
                        }
                        C[i] = (byte) ((A[i] == 1 || B[i] == 1) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case AND -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if (B.length <= i) { // 0 if not long enough
                            C[i] = 0;
                            continue;
                        }
                        C[i] = (byte) ((A[i] == 1 && B[i] == 1) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case XOR -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        if (B.length <= i) { // leave alone if not long enough
                            C[i] = A[i];
                            continue;
                        }
                        C[i] = (byte) (((A[i] == 1 || B[i] == 1) && (!(A[i] == 1 && B[i] == 1))) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case NEGATEFLOAT -> {
                    checkInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    checkIncorrectNumberBytes(A, 4);

                    float v = byteArrayToFloat(A);
                    if (LogScriptExecution) Log.fine("Push -" + v + " onto the stack: " + (-v));
                    Stack.push(floatToByteArray(-v));
                    return true;
                }
                case ADDFLOAT -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkIncorrectNumberBytes(B, 4);
                    byte[] A = Stack.pop();
                    checkIncorrectNumberBytes(A, 4);


                    float v = byteArrayToFloat(A);
                    if (LogScriptExecution)
                        Log.fine("Push " + v + " + " + byteArrayToFloat(B) + " onto the stack: " + (v + byteArrayToFloat(B)));
                    Stack.push(floatToByteArray(v + byteArrayToFloat(B)));
                    return true;
                }
                case SUBTRACTFLOAT -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkIncorrectNumberBytes(B, 4);
                    byte[] A = Stack.pop();
                    checkIncorrectNumberBytes(A, 4);


                    float v = byteArrayToFloat(A);
                    if (LogScriptExecution)
                        Log.fine("Push " + v + " - " + byteArrayToFloat(B) + " onto the stack: " + (v - byteArrayToFloat(B)));
                    Stack.push(floatToByteArray(v - byteArrayToFloat(B)));
                    return true;
                }
                case MULTIPLYFLOAT -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkIncorrectNumberBytes(B, 4);
                    byte[] A = Stack.pop();
                    checkIncorrectNumberBytes(A, 4);


                    float v = byteArrayToFloat(A);
                    if (LogScriptExecution)
                        Log.fine("Push " + v + " * " + byteArrayToFloat(B) + " onto the stack: " + (v * byteArrayToFloat(B)));
                    Stack.push(floatToByteArray(v * byteArrayToFloat(B)));
                    return true;
                }
                case DIVIDEFLOAT -> {
                    checkInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    checkIncorrectNumberBytes(B, 4);
                    byte[] A = Stack.pop();
                    checkIncorrectNumberBytes(A, 4);


                    float v = byteArrayToFloat(B);
                    if (v == 0.f || !Float.isFinite(v)) {
                        Log.info("Invalid inputs");
                        bScriptFailed = true;
                        return false;
                    }

                    if (LogScriptExecution)
                        Log.fine("Push " + byteArrayToFloat(A) + " / " + v + " onto the stack: " + (byteArrayToFloat(A) / v));
                    Stack.push(floatToByteArray(byteArrayToFloat(A) / v));
                    return true;
                }
                case DUP2 -> {
                    checkInsufficientStackSize(2);
                    byte[] A = Stack.pop();
                    byte[] B = Stack.pop();
                    Stack.push(B);
                    Stack.push(A);
                    Stack.push(B);
                    Stack.push(A);
                    return true;
                }
                case DUPN -> {
                    checkInsufficientStackSize(1);

                    byte[] Amount = Stack.pop();
                    checkIncorrectNumberBytes(Amount, 1);

                    int NumberOfElements = Amount[0];

                    checkInsufficientStackSize(NumberOfElements);

                    ArrayList<byte[]> stackElements = new ArrayList<>();

                    for (int i = 0; i < NumberOfElements; i++) {
                        stackElements.add(Stack.pop());
                    }

                    for (int i = 0; i < NumberOfElements * 2; i++) {
                        Stack.push(stackElements.get(NumberOfElements - (1 + (i % NumberOfElements))));
                    }

                    return true;
                }
                case DROPN -> {
                    checkInsufficientStackSize(1);

                    byte[] Amount = Stack.pop();
                    checkIncorrectNumberBytes(Amount, 1);

                    int NumberOfElements = Amount[0];

                    checkInsufficientStackSize(NumberOfElements);

                    for (int i = 0; i < NumberOfElements; i++) {
                        Stack.pop();
                    }

                    return true;
                }
                case SHIFTN -> {
                    checkInsufficientStackSize(1);

                    byte[] Amount = Stack.pop();
                    checkIncorrectNumberBytes(Amount, 1);

                    int NumberOfElements = Amount[0];

                    Collections.rotate(Stack, NumberOfElements);
                    return true;
                }
                case SHIFTELEMENTSRIGHT -> {
                    checkInsufficientStackSize(1);

                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[(i + 1) % A.length] = A[i];
                    }

                    Stack.push(C);

                    return true;
                }
                case SHIFTELEMENTSLEFT -> {
                    checkInsufficientStackSize(1);

                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[(i + (A.length - 1)) % A.length] = A[i];
                    }

                    Stack.push(C);

                    return true;
                }
                case CALL -> {
                    checkInsufficientStackSize(2);
                    byte[] VirtualScriptBytes = Stack.pop();
                    byte[] NumberStackItemsBytes = Stack.pop();

                    checkInsufficientBytes(VirtualScriptBytes, 1);
                    checkIncorrectNumberBytes(NumberStackItemsBytes, 1);

                    byte NumberStackItems = NumberStackItemsBytes[0];

                    checkInsufficientStackSize(NumberStackItems);

                    ArrayList<byte[]> VirtualStack = new ArrayList<>();

                    for (int i = 0; i < NumberStackItems; i++) {
                        VirtualStack.add(Stack.pop());
                    }

                    Collections.reverse(VirtualStack);

                    /////////////////////////////////////////////////////
                    ScriptExecution virtualScriptExecution = new ScriptExecution();
                    virtualScriptExecution.initialize(VirtualScriptBytes, Collections.enumeration(VirtualStack));
                    virtualScriptExecution.setSignatureVerificationMessage(SignatureVerificationMessage); // inherit from parent
                    virtualScriptExecution.LogScriptExecution = LogScriptExecution;  // inherit from parent
                    virtualScriptExecution.bExtendedFlowControl = bExtendedFlowControl; // inherit from parent
                    virtualScriptExecution.Steps = Steps;

                    if (LogScriptExecution)
                        Log.info("Begin virtual script execution: " + BytesUtil.base64Encode(VirtualScriptBytes));

                    //noinspection StatementWithEmptyBody
                    while (virtualScriptExecution.step()) ;

                    for (Object result : virtualScriptExecution.Stack.toArray()) {
                        Stack.push((byte[]) result);
                    }

                    Stack.push(new byte[]{(byte) (virtualScriptExecution.bScriptFailed ? 0 : 1)});

                    Steps = virtualScriptExecution.Steps;

                    if (LogScriptExecution)
                        Log.info("End virtual script execution: " + !virtualScriptExecution.bScriptFailed);
                    /////////////////////////////////////////////////////
                    return true;
                }
                case JUMP -> {
                    checkInsufficientStackSize(1);
                    byte[] DestinationBytes = Stack.pop();
                    checkInsufficientBytes(DestinationBytes, 1);

                    int Destination;

                    if (DestinationBytes.length == 4) {
                        Destination = byteArrayToNumber32(DestinationBytes);
                    } else {
                        Destination = DestinationBytes[0];
                    }

                    checkScriptEndsBefore(Destination);

                    if(!bExtendedFlowControl && Destination <= 0) {
                        bScriptFailed = true;
                        throw new ScriptUnsupportedOperationException("Jumping backwards is not supported."+
                                "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 10, "Exception occurred here!") +
                                "\n" + ScriptUtil.PrintStack(Stack.elements(), null));
                    }

                    int oldInstructionConter = InstructionCounter;
                    InstructionCounter += Destination-1;
                    Log.info("Jumped from "+oldInstructionConter+" to "+InstructionCounter+".");
                    return true;
                }
                case JUMPIF -> {
                    checkInsufficientStackSize(2);
                    byte[] DestinationBytes = Stack.pop();
                    checkInsufficientBytes(DestinationBytes, 1);
                    byte[] ConditionalBooleanBytes = Stack.pop();
                    checkIncorrectNumberBytes(ConditionalBooleanBytes, 1);

                    if(ConditionalBooleanBytes[0] == 0) return true;

                    int Destination;

                    if (DestinationBytes.length == 4) {
                        Destination = byteArrayToNumber32(DestinationBytes);
                    } else {
                        Destination = DestinationBytes[0];
                    }

                    checkScriptEndsBefore(Destination);

                    if(!bExtendedFlowControl && Destination <= 0) {
                        bScriptFailed = true;
                        throw new ScriptUnsupportedOperationException("Jumping backwards is not supported."+
                                "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 100, "Exception occurred here!") +
                                "\n" + ScriptUtil.PrintStack(Stack.elements(), null));
                    }
                    int oldInstructionConter = InstructionCounter;
                    InstructionCounter += Destination-1;
                    Log.info("Jumped from "+oldInstructionConter+" to "+InstructionCounter+".");
                    return true;
                }
                case SHIFTNEXCEPT -> {
                    checkInsufficientStackSize(2);

                    byte[] ExceptBytes = Stack.pop();
                    checkIncorrectNumberBytes(ExceptBytes, 1);

                    byte[] Amount = Stack.pop();
                    checkIncorrectNumberBytes(Amount, 1);

                    int NumberExcluded = ExceptBytes[0];
                    int NumberOfElements = Amount[0];

                    checkNumberIsInRange(NumberExcluded, 0, Stack.size());
                    //if(NumberExcluded < 0) throw new ScriptInvalidParameterException("Number to exclude was not valid! "+NumberExcluded);

                    ArrayList<byte[]> beforeStack = Collections.list(Stack.elements());
                    List<byte[]> toRotate = beforeStack.subList(NumberExcluded, beforeStack.size());

                    Collections.rotate(toRotate, NumberOfElements);

                    Stack.clear();

                    Stack.addAll(beforeStack.subList(0, NumberExcluded));
                    Stack.addAll(toRotate);

                    return true;
                }
            }
        }
        Log.fine("Not handled!");
        bScriptFailed = true;
        throw new ScriptUnsupportedOperationException("The operation "+scriptOperator+" was not handled."+
                "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                "\n" + ScriptUtil.PrintStack(Stack.elements(), null));
    }

    private void checkScriptEndsBefore(int MinimumRemainingBytes) throws ScriptInvalidException {
        if (Script.length < InstructionCounter + MinimumRemainingBytes) {
            bScriptFailed = true;
            throw new ScriptInvalidException("CheckScriptHasMoreBytes Too few bytes in script: " + InstructionCounter +" + "+ MinimumRemainingBytes + " / " + Script.length +
                    "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                    "\n" + ScriptUtil.PrintStack(Stack.elements(), null));
        }
    }

    private void checkInsufficientStackSize(int MinimumSize) throws ScriptEmptyStackException {
        if (Stack.size() < MinimumSize) {
            bScriptFailed = true;
            throw new ScriptEmptyStackException("CheckInsufficientStack Too few items in stack: " + MinimumSize + " / " + Stack.size() +
                    "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                    "\n" + ScriptUtil.PrintStack(Stack.elements(), null));
        }
    }

    private void checkInsufficientBytes(byte[] Bytes, int MinimumSize) throws ScriptInvalidParameterException {
        if (Bytes.length < MinimumSize) {
            bScriptFailed = true;
            throw new ScriptInvalidParameterException("CheckInsufficient Too few bytes in element on top of the stack: " + Bytes.length + " / " + MinimumSize +
                    "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                    "\n" + ScriptUtil.PrintStack(Stack.elements(), Bytes));
        }
    }

    private void checkIncorrectNumberBytes(byte[] Bytes, int Size) throws ScriptInvalidParameterException {
        if (Bytes.length != Size) {
            bScriptFailed = true;
            throw new ScriptInvalidParameterException("CheckIncorrect Expected " + Size + " bytes for top stack element: " + Bytes.length + " != " + Size +
                    "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                    "\n" + ScriptUtil.PrintStack(Stack.elements(), Bytes));
        }
    }

    private void checkNumberIsInRange(int Number, int InclusiveMin, int InclusiveMax) throws ScriptInvalidParameterException {
        if(Number < InclusiveMin || Number > InclusiveMax) {
            bScriptFailed = true;
            throw new ScriptInvalidParameterException("Parameter was " + Number + " but expected " + (Number < InclusiveMin ? "above or equal to " + InclusiveMin : "below or equal to " + InclusiveMax) +
                    "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                    "\n" + ScriptUtil.PrintStack(Stack.elements(), null));

        }
    }

}
