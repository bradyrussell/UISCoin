package com.bradyrussell.uiscoin.script;

public enum ScriptOperator {
    NOP(0x00), //
    /**
     *the next byte specifies the number of following bytes to put on the stack
     * MULTI BYTE OPERATION (Consumes the following byte as well as N bytes after)
     */
    PUSH(0x01), //
    /**
     *push the instruction counter value onto the stack
     */
    INSTRUCTION(0x02), //

    /**
     * Enables a specific metadata byte flag for the script. Has no effect on execution.
     * MULTI BYTE OPERATION (Consumes the following byte)
     */
    FLAG(0x03), //

    /**
     *the next 4 bytes specify the number of following bytes to put on the stack
     * MULTI BYTE OPERATION (Consumes the following 4 bytes as well as N bytes after)
     */
    BIGPUSH(0x04), //

    /**
     *the next byte specifies the number of following bytes to interpret as metadata
     * MULTI BYTE OPERATION (Consumes the following byte as well as N bytes after)
     */
    FLAGDATA(0x05), //


    /**
     *push the time in unix epoch seconds
     */
    TIME(0x06), //

    /**
     *copy the Nth (from BOTTOM of the stack) element on the stack and push it onto the stack
     */
    PICK(0x07), //

    /**
     * Pop the top stack element as N, pop the next stack element and put it into the Nth from the BOTTOM elements location
     */
    PUT(0x08), //

    //comparisons
    /**
     * are the top two values numerically equal when interpreted as 4 byte integers
     */
    NUMEQUAL(0x10),
    /**
     * are the top two values equal byte arrays
     */
    BYTESEQUAL(0x11),
    /**
     * does the top stack value == sha512(second value)
     */
    SHA512EQUAL(0x12), // does A == sha512(b)
    /**
     * are the top two values equal in length
     */
    LENEQUAL(0x13),

    LESSTHAN(0x14),
    LESSTHANEQUAL(0x15),
    GREATERTHAN(0x16),
    GREATERTHANEQUAL(0x17),
    NOTEQUAL(0x18),
    NOTZERO(0x19),

    /**
     * get(int StackElementIndex, int BeginIndex, int Length)
     * StackElement is the Nth (from BOTTOM of the stack) element on the stack
     * From StackElement, copy from BeginIndex to BeginIndex+Length onto the top of the stack
     */
    GET(0x1a),

    /**
     * set(byte[] Source, int StackElement, int BeginIndex, int Length)
     * StackElement is the Nth (from BOTTOM of the stack) element on the stack
     * From source, copy from the beginning to Length into StackElement at BeginIndex to BeginIndex+Length
     */
    SET(0x1b),
    // math
    /**
     * numeric
     */
    ADD(0x20),
    /**
     * numeric
     */
    SUBTRACT(0x21),
    /**
     * numeric
     */
    MULTIPLY(0x22),
    /**
     * numeric Second To Top Stack Element / Top Stack Element
     */
    DIVIDE(0x23),
    /**
     *  a[i] + b[i] = c[i]
     */
    ADDBYTES(0x24), // a[i] + b[i] = c[i]
    /**
     *  a[i] - b[i] = c[i]
     */
    SUBTRACTBYTES(0x25),
    /**
     *  a[i] * b[i] = c[i]
     */
    MULTIPLYBYTES(0x26),
    /**
     *  a[i] / b[i] = c[i]
     */
    DIVIDEBYTES(0x27),
    /**
     *  -x
     */
    NEGATE(0x28), // -x

    /**
     *  Second To Top Stack Element % Top Stack Element
     */
    MODULO(0x29),
    /**
     *  Converts an 8 bit integer to a 32 bit integer
     */
    CONVERT8TO32(0x2a),

    /**
     *  Converts a 32 bit integer to an 8 bit integer
     */
    CONVERT32TO8(0x2b),

    /**
     *  Converts a 64 bit integer to a 32 bit integer
     */
    CONVERT64TO32(0x2c),

    /**
     *  Converts a 32 bit integer to a 64 bit integer
     */
    CONVERT32TO64(0x2d),

    /**
     *  Converts a 32 bit float to a 32 bit integer
     */
    CONVERTFLOATTO32(0x2e),

    /**
     *  Converts a 32 bit integer to a 32 bit float
     */
    CONVERT32TOFLOAT(0x2f),
    // bit operations
    /**
     * bitwise operation
     */
    BITNOT(0x31),
    /**
     * bitwise operation
     */
    BITOR(0x32),
    /**
     * bitwise operation
     */
    BITAND(0x33),
    /**
     * bitwise operation
     */
    BITXOR(0x34),

    // byte[] / string functions
    /**
     *combine the top two stack items
     */
    APPEND(0x40), //
    /**
     * top stack item is a byte number of elements to trim the second-to-top stack array to
     */
    LIMIT(0x41), //
    /**
     *reverse the order of the top stack element
     */
    REVERSE(0x42), //
    /**
     *split top most stack
     */
    SPLIT(0x43), //
    /**
     *combine N (top of the stack) elements into one array. use depth before combine to do all
     */
    COMBINE(0x44), //
    /**
     * push the length of the top stack element on top of the stack WITHOUT removing it
     */
    LEN(0x45), //

    // boolean logic
    /**
     * boolean logic (byte either 1 or 0)
     */
    NOT(0x50),
    /**
     * boolean logic (byte either 1 or 0)
     */
    OR(0x51),
    /**
     * boolean logic (byte either 1 or 0)
     */
    AND(0x52),
    /**
     * boolean logic (byte either 1 or 0)
     */
    XOR(0x53),

    /**
     * 1/x
     */
    INVERTFLOAT(0x60), //1/x

    /**
     *  -x
     */
    NEGATEFLOAT(0x61), // -x
    /**
     * numeric
     */
    ADDFLOAT(0x62),
    /**
     * numeric
     */
    SUBTRACTFLOAT(0x63),
    /**
     * numeric
     */
    MULTIPLYFLOAT(0x64),
    /**
     * numeric
     */
    DIVIDEFLOAT(0x65),

    // push constants
    /**
     *push null onto the stack
     */
    NULL(0x81), //
    /**
     *push 0 on the stack
     */
    FALSE(0x82), //
    /**
     *push 1 on the stack
     */
    TRUE(0x83), //

    //stack operations
    /**
     * number of elements in stack
     */
    DEPTH(0x90), //
    /**
     *drop the top stack element
     */
    DROP(0x91), //
    /**
     *duplicate the top stack element
     */
    DUP(0x92),//
    /**
     *swap the top 2 stack elements
     */
    SWAP(0x93), //
    /**
     * clear the stack
     */
    CLEAR(0x94),//
    /**
     *duplicate the entire stack
     */
    CLONE(0x95), //
    /**
     *reverse the entire stack
     */
    FLIP(0x96), //
    /**
     * shift the entire stack, so the top element becomes last, second becomes first etc
     */
    SHIFTUP(0x97), //
    /**
     *shift the entire stack, so the top element becomes second, last becomes first etc
     */
    SHIFTDOWN(0x98), //

    /**
     *shift the entire stack, N elements, based on the byte on top of the stack
     */
    SHIFTN(0x99), //

    /**
     * shift the top stack element's elements to the right, so the first element becomes the second, last becomes first
     */
    SHIFTELEMENTSRIGHT(0x9a), //
    /**
     * shift the top stack element's elements to the left, so the first element becomes the last, second becomes first
     */
    SHIFTELEMENTSLEFT(0x9b), //

    /**
     *duplicate the top 2 stack elements
     */
    DUP2(0x9c),//

    /**
     * pops the top stack item as a byte N, then duplicate the next top N stack elements
     */
    DUPN(0x9d),//

    /**
     * pops the top stack item as a byte N, then drops the next top N stack elements
     */
    DROPN(0x9e), //

    /**
     *shift the entire stack EXCEPT the bottom X elements, by Y elements, where X is top of the stack and Y is second so shiftNExcept(1, 4) 1 shift 4 excluded elements
     */
    SHIFTNEXCEPT(0x9f), //
    // returns
    /**
     * script execution continues if there is a 1 on the stack, else fails
     */
    VERIFY(0xa0),
    /**
     * script fails unconditionally
     */
    RETURN(0xa1),

    /**
     * script fails IF there is a 1 on top of the stack
     */
    RETURNIF(0xa2),


//    LOCKTIMEVERIFY(0xa2),

    //hashes
    SHA512(0xb0),

    //compression
    ZIP(0xb1),
    UNZIP(0xb2),

    //encryption
    /**
     * Encrypts a message (top of stack) using the key (second to top) using Cipher.getInstance("AES/ECB/PKCS5Padding").
     */
    ENCRYPTAES(0xb5),

    /**
     * Decrypts a message (top of stack) using the key (second to top) using Cipher.getInstance("AES/ECB/PKCS5Padding").
     */
    DECRYPTAES(0xb6),

    // signature
    /**
     * gets the public key (top of stack) and the signature (second to top) and stops the script, failing if necessary//pushes true if verified
     */
    VERIFYSIG(0xc0), //

    // code
    CODESEPARATOR(0xc1),

    /**
     * Executes script bytecode from the stack. Stack elements are [virtual script bytecode] [byte number of stack items to take] then N byte arrays
     * Pushes the resulting stack and then a true or false based on whether the execution was successful
     * Used to be called virtual script because a new virtual machine is created to run the function (cannot access outside of its scope)
     */
    CALL(0xd0),

    /**
     * Jumps to the specified instruction. This is relative to the current location.
     * In standard transaction scripts you cannot jump backwards.
     * Expects a byte on top of the stack to be ADDED TO the instruction counter.
     */
    JUMP(0xf0),

    /**
     * Jumps to the specified instruction. This is relative to the current location.
     * In standard transaction scripts you cannot jump backwards.
     * Expects a byte on top of the stack to be ADDED TO the instruction counter conditionally followed by a byte as boolean.
     */
    JUMPIF(0xf1),
    ;

    public final byte OPCode;

    ScriptOperator(int opCode) {
        OPCode = (byte) opCode;
    }

    public static ScriptOperator getByOpCode(byte OPCode){
        for(ScriptOperator operator:ScriptOperator.values()){
            if(operator.OPCode == OPCode) return operator;
        }
        return null;
    }
}
