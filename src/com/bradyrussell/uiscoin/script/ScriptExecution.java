package com.bradyrussell.uiscoin.script;

import com.bradyrussell.uiscoin.Encryption;
import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Keys;
import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.script.exception.ScriptEmptyStackException;
import com.bradyrussell.uiscoin.script.exception.ScriptInvalidException;
import com.bradyrussell.uiscoin.script.exception.ScriptInvalidParameterException;
import com.bradyrussell.uiscoin.script.exception.ScriptUnsupportedOperationException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.logging.Logger;

import static com.bradyrussell.uiscoin.BytesUtil.*;

public class ScriptExecution {
    private static final Logger Log = Logger.getLogger(ScriptExecution.class.getName());
    public int InstructionCounter;
    public Stack<byte[]> Stack;
    public boolean bScriptFailed = false;
    public int MaximumStepsAllowed = 1000;
    public boolean bExtendedFlowControl = false;

    public boolean LogScriptExecution = false;

    private byte[] SignatureVerificationMessage = null; // we need a way to pass in the data for verifysig. i dont like this but...
    //private long BlockTime = 0; // need a way to pass in blocktime for TIME operator

    public byte[] Script;

    public boolean Initialize(byte[] Script) {
        this.Script = Script;
        Stack = new Stack<>();
        //validate
        if (LogScriptExecution) Log.info("Script initialized " + Script.length + " bytes with empty stack.");
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

    public void dumpStackReadable() {
        Object[] toArray = Stack.toArray();

        for (int i = 0, toArrayLength = toArray.length; i < toArrayLength; i++) {
            byte[] stackElem = (byte[]) toArray[i];
            Log.info(i + " ");
            BytesUtil.printBytesReadable(stackElem);
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

    public int Steps = 0;

    // returns whether the script should continue
    public boolean Step() throws ScriptEmptyStackException, ScriptInvalidParameterException, ScriptInvalidException, ScriptUnsupportedOperationException {
        if (InstructionCounter >= Script.length) return false;

        if(Steps++ > MaximumStepsAllowed) throw new ScriptUnsupportedOperationException("Script exceeded the instruction limit.");

        ScriptOperator scriptOperator = ScriptOperator.getByOpCode(Script[InstructionCounter++]);

        if (LogScriptExecution) Log.info("OP " + scriptOperator);

        if (scriptOperator != null) {
            switch (scriptOperator) {
                case REVERSE -> {
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();
                    byte[] B = new byte[A.length];

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        B[i] = A[(A.length - 1) - i];
                    }
                    if (LogScriptExecution) Log.fine("Reverse top stack element");
                    Stack.push(B);
                    return true;
                }
                case NULL -> {
                    if (LogScriptExecution) Log.fine("Push null onto the stack");
                    Stack.push(new byte[]{});
                    return true;
                }
                case PUSH -> {
                    CheckScriptEndsBefore(1);

                    int NumberOfBytesToPush = Script[InstructionCounter++];

                    if (NumberOfBytesToPush <= 0) {
                        Log.warning("Invalid PUSH amount specified: " + NumberOfBytesToPush);
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] bytes = new byte[NumberOfBytesToPush];

                    CheckScriptEndsBefore(NumberOfBytesToPush);

                    for (int i = 0; i < NumberOfBytesToPush; i++) {
                        bytes[i] = Script[InstructionCounter++];
                    }
                    if (LogScriptExecution)
                        Log.fine("Push " + NumberOfBytesToPush + " bytes onto the stack: " + Arrays.toString(bytes));
                    Stack.push(bytes);
                    return true;
                }
                case INSTRUCTION -> {
                    Stack.push(NumberToByteArray32(InstructionCounter));
                    if (LogScriptExecution) Log.fine("Push instruction counter onto the stack: " + InstructionCounter);
                    return true;
                }
                case FLAG -> {
                    // flags are ignored by the code
                    CheckScriptEndsBefore(1);

                    int FlagValue = Script[InstructionCounter++];
                    if (LogScriptExecution) Log.fine("Flag: " + FlagValue);
                    return true;
                }
                case BIGPUSH -> {
                    CheckScriptEndsBefore(4);

                    byte[] int32bytes = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        int32bytes[i] = Script[InstructionCounter++];
                    }

                    int NumberOfBytesToPush = BytesUtil.ByteArrayToNumber32(int32bytes);

                    if (NumberOfBytesToPush <= 0) {
                        Log.warning("Invalid BIGPUSH amount specified: " + NumberOfBytesToPush);
                        bScriptFailed = true;
                        return false;
                    }

                    CheckScriptEndsBefore(NumberOfBytesToPush);

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
                    CheckScriptEndsBefore(1);

                    int NumberOfBytesForFlag = Script[InstructionCounter++];

                    if (NumberOfBytesForFlag <= 0) {
                        Log.warning("Invalid FLAGDATA amount specified: " + NumberOfBytesForFlag);
                        bScriptFailed = true;
                        return false;
                    }

                    byte[] bytes = new byte[NumberOfBytesForFlag];

                    CheckScriptEndsBefore(NumberOfBytesForFlag);

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
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckInsufficientBytes(B, 1);
                    byte[] A = Stack.pop();
                    CheckInsufficientBytes(A, 1);

                    long iA, iB;

                    if(A.length == 1){
                        iA = A[0];
                    } else if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                    }

                    if(B.length == 1){
                        iB = B[0];
                    } else if (B.length < 8) {
                        iB = ByteArrayToNumber32(B);
                    } else {
                        iB = ByteArrayToNumber64(B);
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " == " + iB + " onto the stack: " + (iA == iB));
                    Stack.push(new byte[]{iA == iB ? (byte) 1 : (byte) 0});

                    return true;
                }
                case BYTESEQUAL -> {
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();
                    boolean equal = A.length == B.length;

                    if (LogScriptExecution)
                        Log.fine("Push " + A.length + " == " + B.length + " onto the stack: " + equal);

                    Stack.push(new byte[]{(byte) (equal ? 1 : 0)});
                    return true;
                }
                case LESSTHAN -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckInsufficientBytes(B, 1);
                    byte[] A = Stack.pop();
                    CheckInsufficientBytes(A, 1);

                    long iA, iB;

                    if(A.length == 1){
                        iA = A[0];
                    } else if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                    }

                    if(B.length == 1){
                        iB = B[0];
                    } else if (B.length < 8) {
                        iB = ByteArrayToNumber32(B);
                    } else {
                        iB = ByteArrayToNumber64(B);
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " < " + iB + " onto the stack: " + (iA < iB));
                    Stack.push(new byte[]{iA < iB ? (byte) 1 : (byte) 0});

                    return true;
                }
                case LESSTHANEQUAL -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckInsufficientBytes(B, 1);
                    byte[] A = Stack.pop();
                    CheckInsufficientBytes(A, 1);

                    long iA, iB;

                    if(A.length == 1){
                        iA = A[0];
                    } else if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                    }

                    if(B.length == 1){
                        iB = B[0];
                    } else if (B.length < 8) {
                        iB = ByteArrayToNumber32(B);
                    } else {
                        iB = ByteArrayToNumber64(B);
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " <= " + iB + " onto the stack: " + (iA <= iB));
                    Stack.push(new byte[]{iA <= iB ? (byte) 1 : (byte) 0});

                    return true;
                }
                case GREATERTHAN -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckInsufficientBytes(B, 1);
                    byte[] A = Stack.pop();
                    CheckInsufficientBytes(A, 1);

                    long iA, iB;

                    if(A.length == 1){
                        iA = A[0];
                    } else if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                    }

                    if(B.length == 1){
                        iB = B[0];
                    } else if (B.length < 8) {
                        iB = ByteArrayToNumber32(B);
                    } else {
                        iB = ByteArrayToNumber64(B);
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " > " + iB + " onto the stack: " + (iA > iB));
                    Stack.push(new byte[]{iA > iB ? (byte) 1 : (byte) 0});

                    return true;
                }
                case GREATERTHANEQUAL -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckInsufficientBytes(B, 1);
                    byte[] A = Stack.pop();
                    CheckInsufficientBytes(A, 1);

                    long iA, iB;

                    if(A.length == 1){
                        iA = A[0];
                    } else if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                    }

                    if(B.length == 1){
                        iB = B[0];
                    } else if (B.length < 8) {
                        iB = ByteArrayToNumber32(B);
                    } else {
                        iB = ByteArrayToNumber64(B);
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " >= " + iB + " onto the stack: " + (iA >= iB));
                    Stack.push(new byte[]{iA >= iB ? (byte) 1 : (byte) 0});

                    return true;
                }
                case NOTEQUAL -> {
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(1);
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
                    CheckInsufficientStackSize(3);

                    byte[] LengthBytes = Stack.pop();
                    CheckInsufficientBytes(LengthBytes, 4);
                    int Length = ByteArrayToNumber32(LengthBytes);

                    byte[] BeginIndexBytes = Stack.pop();
                    CheckInsufficientBytes(BeginIndexBytes, 4);
                    int BeginIndex = ByteArrayToNumber32(BeginIndexBytes);

                    byte[] StackElementIndexBytes = Stack.pop();
                    CheckInsufficientBytes(StackElementIndexBytes, 4);
                    int StackElementIndex = ByteArrayToNumber32(StackElementIndexBytes);

                    CheckInsufficientStackSize(StackElementIndex+1);
                    CheckNumberIsInRange(StackElementIndex, 0, Stack.size()-1);

                    byte[] StackElement = Stack.elementAt(StackElementIndex);

                    CheckNumberIsInRange(Length, 0, StackElement.length);
                    CheckNumberIsInRange(BeginIndex, 0, StackElement.length);

                    CheckInsufficientBytes(StackElement, BeginIndex+Length);

                    byte[] Copy = new byte[Length];
                    System.arraycopy(StackElement, BeginIndex, Copy, 0, Length);

                    Stack.push(Copy);
                    return true;
                }
                case SET -> {
                    CheckInsufficientStackSize(4);

                    byte[] LengthBytes = Stack.pop();
                    CheckInsufficientBytes(LengthBytes, 4);
                    int Length = ByteArrayToNumber32(LengthBytes);

                    byte[] BeginIndexBytes = Stack.pop();
                    CheckInsufficientBytes(BeginIndexBytes, 4);
                    int BeginIndex = ByteArrayToNumber32(BeginIndexBytes);

                    byte[] DestinationIndexBytes = Stack.pop();
                    CheckInsufficientBytes(DestinationIndexBytes, 4);
                    int StackElementIndex = ByteArrayToNumber32(DestinationIndexBytes);

                    byte[] Source = Stack.pop();

                    CheckInsufficientStackSize(StackElementIndex+1);
                    CheckNumberIsInRange(StackElementIndex, 0, Stack.size()-1);

                    //byte[] DestinationOriginal = Stack.elementAt(StackElementIndex);

                    CheckInsufficientBytes(Source, Length);
                    CheckNumberIsInRange(Length, 0, Source.length);
                    CheckNumberIsInRange(BeginIndex+Length, 0, Stack.elementAt(StackElementIndex).length);

                    //byte[] Result = new byte[BeginIndex+Length];
                    // todo bounds checks
                    System.arraycopy(Source, 0, Stack.elementAt(StackElementIndex), BeginIndex, Length);

                    //Stack.push(Copy);
                    return true;
                }
                case COPY -> {
                }
                case ALLOC -> {
                    CheckInsufficientStackSize(1);

                    byte[] LengthBytes = Stack.pop();
                    CheckInsufficientBytes(LengthBytes, 4);
                    int Length = ByteArrayToNumber32(LengthBytes);

                    CheckNumberIsInRange(Length, 0, Short.MAX_VALUE);

                    Stack.push(new byte[Length]);
                    return true;
                }
                case THIS -> { // allows recursion
                    if(!bExtendedFlowControl) throw new ScriptUnsupportedOperationException("The THIS operation is only available with Extended Flow Control enabled.");
                    Stack.push(Script);
                    return true;
                }
                case ADD -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckInsufficientBytes(B, 4);
                    byte[] A = Stack.pop();
                    CheckInsufficientBytes(A, 4);

                    long iA, iB;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (B.length < 8) {
                        iB = ByteArrayToNumber32(B);
                    } else {
                        iB = ByteArrayToNumber64(B);
                        bReturnLong = true;
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " + " + iB + " onto the stack: " + (iA + iB));
                    Stack.push(bReturnLong ? NumberToByteArray64(iA + iB) : NumberToByteArray32((int) (iA + iB)));
                    return true;
                }
                case SUBTRACT -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckInsufficientBytes(B, 4);
                    byte[] A = Stack.pop();
                    CheckInsufficientBytes(A, 4);

                    long iA, iB;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (B.length < 8) {
                        iB = ByteArrayToNumber32(B);
                    } else {
                        iB = ByteArrayToNumber64(B);
                        bReturnLong = true;
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " - " + iB + " onto the stack: " + (iA - iB));
                    Stack.push(bReturnLong ? NumberToByteArray64(iA - iB) : NumberToByteArray32((int) (iA - iB)));
                    return true;
                }
                case MULTIPLY -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckInsufficientBytes(B, 4);
                    byte[] A = Stack.pop();
                    CheckInsufficientBytes(A, 4);

                    long iA, iB;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (B.length < 8) {
                        iB = ByteArrayToNumber32(B);
                    } else {
                        iB = ByteArrayToNumber64(B);
                        bReturnLong = true;
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " * " + iB + " onto the stack: " + (iA * iB));
                    Stack.push(bReturnLong ? NumberToByteArray64(iA * iB) : NumberToByteArray32((int) (iA * iB)));
                    return true;
                }
                case DIVIDE -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckInsufficientBytes(B, 4);
                    byte[] A = Stack.pop();
                    CheckInsufficientBytes(A, 4);

                    long iA, iB;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (B.length < 8) {
                        iB = ByteArrayToNumber32(B);
                    } else {
                        iB = ByteArrayToNumber64(B);
                        bReturnLong = true;
                    }

                    if (iB == 0) {
                        bScriptFailed = true;
                        Log.info("Divide by zero");
                        return false;
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " / " + iB + " onto the stack: " + (iA / iB));
                    Stack.push(bReturnLong ? NumberToByteArray64(iA / iB) : NumberToByteArray32((int) (iA / iB)));
                    return true;
                }
                case MODULO -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckInsufficientBytes(B, 4);
                    byte[] A = Stack.pop();
                    CheckInsufficientBytes(A, 4);

                    long iA, iB;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (B.length < 8) {
                        iB = ByteArrayToNumber32(B);
                    } else {
                        iB = ByteArrayToNumber64(B);
                        bReturnLong = true;
                    }

                    if (iB == 0) {
                        bScriptFailed = true;
                        Log.info("Divide by zero");
                        return false;
                    }

                    if (LogScriptExecution) Log.fine("Push " + iA + " % " + iB + " onto the stack: " + (iA % iB));
                    Stack.push(bReturnLong ? NumberToByteArray64(iA % iB) : NumberToByteArray32((int) (iA % iB)));
                    return true;
                }
                case ADDBYTES -> {
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    CheckInsufficientBytes(A, 4);

                    long iA;
                    boolean bReturnLong = false;

                    if (A.length < 8) {
                        iA = ByteArrayToNumber32(A);
                    } else {
                        iA = ByteArrayToNumber64(A);
                        bReturnLong = true;
                    }

                    if (LogScriptExecution) Log.fine("Push -(" + iA + ")" + " onto the stack: " + -(iA));
                    Stack.push(bReturnLong ? NumberToByteArray64(-iA) : NumberToByteArray32((int) (-iA)));
                    return true;
                }
                case INVERTFLOAT -> {
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    CheckInsufficientBytes(A, 4);

                    Stack.push(FloatToByteArray(1.f / ByteArrayToFloat(A)));
                    return true;
                }
                case CONVERT8TO32 -> {
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    CheckInsufficientBytes(A, 1);

                    Stack.push(BytesUtil.NumberToByteArray32(A[0]));
                    return true;
                }
                case CONVERT32TO8 -> {
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    CheckInsufficientBytes(A, 4);

                    Stack.push(new byte[]{(byte) BytesUtil.ByteArrayToNumber32(A)});
                    return true;
                }
                case CONVERT64TO32 -> {
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    CheckInsufficientBytes(A, 8);

                    Stack.push(BytesUtil.NumberToByteArray32((int) BytesUtil.ByteArrayToNumber64(A)));
                    return true;
                }
                case CONVERT32TO64 -> {
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    CheckInsufficientBytes(A, 4);

                    Stack.push(BytesUtil.NumberToByteArray64(BytesUtil.ByteArrayToNumber32(A)));
                    return true;
                }
                case CONVERTFLOATTO32 -> {
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    CheckInsufficientBytes(A, 4);

                    Stack.push(BytesUtil.NumberToByteArray32((int) BytesUtil.ByteArrayToFloat(A)));
                    return true;
                }
                case CONVERT32TOFLOAT -> {
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    CheckInsufficientBytes(A, 4);

                    Stack.push(BytesUtil.FloatToByteArray((float) BytesUtil.ByteArrayToNumber32(A)));
                    return true;
                }
                case BITNOT -> {
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0, aLength = A.length; i < aLength; i++) {
                        C[i] = (byte) ~A[i];
                    }

                    Stack.push(C);
                    return true;
                }
                case BITOR -> {
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(1);
                    Stack.pop();
                    return true;
                }
                case DUP -> {
                    CheckInsufficientStackSize(1);
                    byte[] bytes = Stack.pop();
                    Stack.push(bytes);
                    Stack.push(bytes);
                    return true;
                }
                case SWAP -> {
                    CheckInsufficientStackSize(2);

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
                    CheckInsufficientStackSize(2);

                    byte[] A = Stack.pop();

                    CheckIncorrectNumberBytes(A, 1);

                    CheckInsufficientStackSize(A[0] + 1);

                    if (A[0] < 0) {
                        Log.info("Negative index for pick");
                        bScriptFailed = true;
                        return false;
                    }

                    Stack.push(Stack.elementAt(A[0]));
                    return true;
                }
                case PUT -> {
                    CheckInsufficientStackSize(2);

                    byte[] A = Stack.pop();

                    CheckIncorrectNumberBytes(A, 1);

                    byte[] B = Stack.pop();

                    CheckInsufficientStackSize(A[0] + 1);

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
                    CheckInsufficientStackSize(1);
                    byte[] bytes = Stack.pop();
                    if (LogScriptExecution)
                        Log.fine("Verify " + Arrays.toString(bytes) + " == true: " + (bytes.length == 1 && bytes[0] == 1));

                    if (!(bytes.length == 1 && bytes[0] == 1)) {
                        bScriptFailed = true;
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
                    CheckInsufficientStackSize(1);
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
                    CheckInsufficientStackSize(1);
                    Stack.push(Hash.getSHA512Bytes(Stack.pop()));
                    return true;
                }
                case ZIP -> {
                    CheckInsufficientStackSize(1);
                    Stack.push(BytesUtil.ZipBytes(Stack.pop()));
                    return true;
                }
                case UNZIP -> {
                    CheckInsufficientStackSize(1);
                    Stack.push(BytesUtil.UnzipBytes(Stack.pop()));
                    return true;
                }
                case ENCRYPTAES -> {
                    CheckInsufficientStackSize(2);
                    byte[] Message = Stack.pop();
                    byte[] Key = Stack.pop();

                    try {
                        Stack.push(Encryption.Encrypt(Message, Key));
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                        e.printStackTrace();
                        bScriptFailed = true;
                        return false;
                    }

                    return true;
                }
                case DECRYPTAES -> {
                    CheckInsufficientStackSize(2);
                    byte[] Message = Stack.pop();
                    byte[] Key = Stack.pop();

                    try {
                        Stack.push(Encryption.Decrypt(Message, Key));
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
                    CheckInsufficientStackSize(2);

                    byte[] PublicKey = Stack.pop();
                    byte[] Signature = Stack.pop();

                    Keys.SignedData signedData = new Keys.SignedData(PublicKey, Signature, SignatureVerificationMessage);
                    try {
                        boolean verifySignedData = Keys.VerifySignedData(signedData);
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
                case LIMIT -> {
                    CheckInsufficientStackSize(2);

                    byte[] B = Stack.pop();
                    byte[] A = Stack.pop();

                    CheckIncorrectNumberBytes(B, 1);

                    int NumberOfBytesToPush = B[0];

                    if (NumberOfBytesToPush < 0) NumberOfBytesToPush = 0;
                    if (NumberOfBytesToPush > A.length) NumberOfBytesToPush = A.length;

                    byte[] bytes = new byte[NumberOfBytesToPush];

                    if (NumberOfBytesToPush >= 1) System.arraycopy(A, 0, bytes, 0, NumberOfBytesToPush);

                    Stack.push(bytes);
                    return true;
                }
                case SPLIT -> {
                    CheckInsufficientStackSize(1);

                    byte[] A = Stack.pop();

                    for (byte b : A) {
                        Stack.push(new byte[]{b});
                    }

                    return true;
                }
                case COMBINE -> {
                    CheckInsufficientStackSize(2);

                    byte[] B = Stack.pop();

                    CheckIncorrectNumberBytes(B, 1);

                    byte NumberOfItemsToCombine = B[0];

                    CheckInsufficientStackSize(NumberOfItemsToCombine);

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
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.peek();
                    Stack.push(NumberToByteArray32(A.length));

                    return true;

                }
                case NOT -> {
                    CheckInsufficientStackSize(1);

                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[i] = (byte) ((A[i] == 0) ? 1 : 0);
                    }

                    Stack.push(C);

                    return true;
                }
                case OR -> {
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(2);
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
                    CheckInsufficientStackSize(1);
                    byte[] A = Stack.pop();

                    CheckIncorrectNumberBytes(A, 4);

                    float v = ByteArrayToFloat(A);
                    if (LogScriptExecution) Log.fine("Push -" + v + " onto the stack: " + (-v));
                    Stack.push(FloatToByteArray(-v));
                    return true;
                }
                case ADDFLOAT -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckIncorrectNumberBytes(B, 4);
                    byte[] A = Stack.pop();
                    CheckIncorrectNumberBytes(A, 4);


                    float v = ByteArrayToFloat(A);
                    if (LogScriptExecution)
                        Log.fine("Push " + v + " + " + ByteArrayToFloat(B) + " onto the stack: " + (v + ByteArrayToFloat(B)));
                    Stack.push(FloatToByteArray(v + ByteArrayToFloat(B)));
                    return true;
                }
                case SUBTRACTFLOAT -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckIncorrectNumberBytes(B, 4);
                    byte[] A = Stack.pop();
                    CheckIncorrectNumberBytes(A, 4);


                    float v = ByteArrayToFloat(A);
                    if (LogScriptExecution)
                        Log.fine("Push " + v + " - " + ByteArrayToFloat(B) + " onto the stack: " + (v - ByteArrayToFloat(B)));
                    Stack.push(FloatToByteArray(v - ByteArrayToFloat(B)));
                    return true;
                }
                case MULTIPLYFLOAT -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckIncorrectNumberBytes(B, 4);
                    byte[] A = Stack.pop();
                    CheckIncorrectNumberBytes(A, 4);


                    float v = ByteArrayToFloat(A);
                    if (LogScriptExecution)
                        Log.fine("Push " + v + " * " + ByteArrayToFloat(B) + " onto the stack: " + (v * ByteArrayToFloat(B)));
                    Stack.push(FloatToByteArray(v * ByteArrayToFloat(B)));
                    return true;
                }
                case DIVIDEFLOAT -> {
                    CheckInsufficientStackSize(2);
                    byte[] B = Stack.pop();
                    CheckIncorrectNumberBytes(B, 4);
                    byte[] A = Stack.pop();
                    CheckIncorrectNumberBytes(A, 4);


                    float v = ByteArrayToFloat(B);
                    if (v == 0.f || !Float.isFinite(v)) {
                        Log.info("Invalid inputs");
                        bScriptFailed = true;
                        return false;
                    }

                    if (LogScriptExecution)
                        Log.fine("Push " + ByteArrayToFloat(A) + " / " + v + " onto the stack: " + (ByteArrayToFloat(A) / v));
                    Stack.push(FloatToByteArray(ByteArrayToFloat(A) / v));
                    return true;
                }
                case DUP2 -> {
                    CheckInsufficientStackSize(2);
                    byte[] A = Stack.pop();
                    byte[] B = Stack.pop();
                    Stack.push(B);
                    Stack.push(A);
                    Stack.push(B);
                    Stack.push(A);
                    return true;
                }
                case DUPN -> {
                    CheckInsufficientStackSize(1);

                    byte[] Amount = Stack.pop();
                    CheckIncorrectNumberBytes(Amount, 1);

                    int NumberOfElements = Amount[0];

                    CheckInsufficientStackSize(NumberOfElements);

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
                    CheckInsufficientStackSize(1);

                    byte[] Amount = Stack.pop();
                    CheckIncorrectNumberBytes(Amount, 1);

                    int NumberOfElements = Amount[0];

                    CheckInsufficientStackSize(NumberOfElements);

                    for (int i = 0; i < NumberOfElements; i++) {
                        Stack.pop();
                    }

                    return true;
                }
                case SHIFTN -> {
                    CheckInsufficientStackSize(1);

                    byte[] Amount = Stack.pop();
                    CheckIncorrectNumberBytes(Amount, 1);

                    int NumberOfElements = Amount[0];

                    Collections.rotate(Stack, NumberOfElements);
                    return true;
                }
                case SHIFTELEMENTSRIGHT -> {
                    CheckInsufficientStackSize(1);

                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[(i + 1) % A.length] = A[i];
                    }

                    Stack.push(C);

                    return true;
                }
                case SHIFTELEMENTSLEFT -> {
                    CheckInsufficientStackSize(1);

                    byte[] A = Stack.pop();
                    byte[] C = new byte[A.length];

                    for (int i = 0; i < A.length; i++) {
                        C[(i + (A.length - 1)) % A.length] = A[i];
                    }

                    Stack.push(C);

                    return true;
                }
                case CALL -> {
                    CheckInsufficientStackSize(2);
                    byte[] VirtualScriptBytes = Stack.pop();
                    byte[] NumberStackItemsBytes = Stack.pop();

                    CheckInsufficientBytes(VirtualScriptBytes, 1);
                    CheckIncorrectNumberBytes(NumberStackItemsBytes, 1);

                    byte NumberStackItems = NumberStackItemsBytes[0];

                    CheckInsufficientStackSize(NumberStackItems);

                    ArrayList<byte[]> VirtualStack = new ArrayList<>();

                    for (int i = 0; i < NumberStackItems; i++) {
                        VirtualStack.add(Stack.pop());
                    }

                    Collections.reverse(VirtualStack);

                    /////////////////////////////////////////////////////
                    ScriptExecution virtualScriptExecution = new ScriptExecution();
                    virtualScriptExecution.Initialize(VirtualScriptBytes, Collections.enumeration(VirtualStack));
                    virtualScriptExecution.setSignatureVerificationMessage(SignatureVerificationMessage); // inherit from parent
                    virtualScriptExecution.LogScriptExecution = LogScriptExecution;  // inherit from parent
                    virtualScriptExecution.bExtendedFlowControl = bExtendedFlowControl; // inherit from parent
                    virtualScriptExecution.Steps = Steps;

                    if (LogScriptExecution)
                        Log.info("Begin virtual script execution: " + BytesUtil.Base64Encode(VirtualScriptBytes));

                    //noinspection StatementWithEmptyBody
                    while (virtualScriptExecution.Step()) ;

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
                    CheckInsufficientStackSize(1);
                    byte[] DestinationBytes = Stack.pop();
                    CheckInsufficientBytes(DestinationBytes, 1);

                    int Destination;

                    if (DestinationBytes.length == 4) {
                        Destination = ByteArrayToNumber32(DestinationBytes);
                    } else {
                        Destination = DestinationBytes[0];
                    }

                    CheckScriptEndsBefore(Destination);

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
                    CheckInsufficientStackSize(2);
                    byte[] DestinationBytes = Stack.pop();
                    CheckInsufficientBytes(DestinationBytes, 1);
                    byte[] ConditionalBooleanBytes = Stack.pop();
                    CheckIncorrectNumberBytes(ConditionalBooleanBytes, 1);

                    if(ConditionalBooleanBytes[0] == 0) return true;

                    int Destination;

                    if (DestinationBytes.length == 4) {
                        Destination = ByteArrayToNumber32(DestinationBytes);
                    } else {
                        Destination = DestinationBytes[0];
                    }

                    CheckScriptEndsBefore(Destination);

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
                    CheckInsufficientStackSize(2);

                    byte[] ExceptBytes = Stack.pop();
                    CheckIncorrectNumberBytes(ExceptBytes, 1);

                    byte[] Amount = Stack.pop();
                    CheckIncorrectNumberBytes(Amount, 1);

                    int NumberExcluded = ExceptBytes[0];
                    int NumberOfElements = Amount[0];

                    CheckNumberIsInRange(NumberExcluded, 0, Stack.size());
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

    private void CheckScriptEndsBefore(int MinimumRemainingBytes) throws ScriptInvalidException {
        if (Script.length < InstructionCounter + MinimumRemainingBytes) {
            bScriptFailed = true;
            throw new ScriptInvalidException("CheckScriptHasMoreBytes Too few bytes in script: " + InstructionCounter +" + "+ MinimumRemainingBytes + " / " + Script.length +
                    "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                    "\n" + ScriptUtil.PrintStack(Stack.elements(), null));
        }
    }

    private void CheckInsufficientStackSize(int MinimumSize) throws ScriptEmptyStackException {
        if (Stack.size() < MinimumSize) {
            bScriptFailed = true;
            throw new ScriptEmptyStackException("CheckInsufficientStack Too few items in stack: " + MinimumSize + " / " + Stack.size() +
                    "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                    "\n" + ScriptUtil.PrintStack(Stack.elements(), null));
        }
    }

    private void CheckInsufficientBytes(byte[] Bytes, int MinimumSize) throws ScriptInvalidParameterException {
        if (Bytes.length < MinimumSize) {
            bScriptFailed = true;
            throw new ScriptInvalidParameterException("CheckInsufficient Too few bytes in element on top of the stack: " + Bytes.length + " / " + MinimumSize +
                    "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                    "\n" + ScriptUtil.PrintStack(Stack.elements(), Bytes));
        }
    }

    private void CheckIncorrectNumberBytes(byte[] Bytes, int Size) throws ScriptInvalidParameterException {
        if (Bytes.length != Size) {
            bScriptFailed = true;
            throw new ScriptInvalidParameterException("CheckIncorrect Expected " + Size + " bytes for top stack element: " + Bytes.length + " != " + Size +
                    "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                    "\n" + ScriptUtil.PrintStack(Stack.elements(), Bytes));
        }
    }

    private void CheckNumberIsInRange(int Number, int InclusiveMin, int InclusiveMax) throws ScriptInvalidParameterException {
        if(Number < InclusiveMin || Number > InclusiveMax) {
            bScriptFailed = true;
            throw new ScriptInvalidParameterException("Parameter was " + Number + " but expected " + (Number < InclusiveMin ? "above or equal to " + InclusiveMin : "below or equal to " + InclusiveMax) +
                    "\n" + ScriptUtil.PrintScriptOpCodesSurroundingHighlight(Script, InstructionCounter - 1, 5, "Exception occurred here!") +
                    "\n" + ScriptUtil.PrintStack(Stack.elements(), null));

        }
    }

}
