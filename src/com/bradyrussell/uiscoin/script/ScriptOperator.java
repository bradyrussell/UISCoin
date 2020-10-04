package com.bradyrussell.uiscoin.script;

public enum ScriptOperator {
    /**
     * push null on the stack
     */
    NOP(0x00), //
    /**
     *the next byte specifies the number of following bytes to put on the stack
     */
    PUSH(0x01), //
    /**
     *push the instruction counter value onto the stack
     */
    INSTRUCTION(0x02), //

    //comparisons
    NUMEQUAL(0x10),
    BYTESEQUAL(0x11),
    SHA512EQUAL(0x12), // does A == sha512(b)
    LENEQUAL(0x13),
    LESSTHAN(0x14),
    LESSTHANEQUAL(0x15),
    GREATERTHAN(0x16),
    GREATERTHANEQUAL(0x17),
    NOTEQUAL(0x18),
    NOTZERO(0x19),

    // math
    ADD(0x20),
    SUBTRACT(0x21),
    MULTIPLY(0x22),
    DIVIDE(0x23),
    ADDBYTES(0x24), // a[i] + b[i] = c[i]
    SUBTRACTBYTES(0x25),
    MULTIPLYBYTES(0x26),
    DIVIDEBYTES(0x27),
    NEGATE(0x28), // -x
    INVERT(0x29), //1/x

    // bit operations
    BITNOT(0x31),
    BITOR(0x32),
    BITAND(0x33),
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
    NOT(0x50),
    OR(0x51),
    AND(0x52),
    XOR(0x53),

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
     *copy the Nth (top of the stack) element on the stack and push it onto the stack
     */
    PICK(0x99), //

    // returns
    VERIFY(0xa0),
    RETURN(0xa1),
    LOCKTIMEVERIFY(0xa2),

    //hashes
    SHA512(0xb0),

    // signature
    /**
     * gets the public key (top of stack) and the signature (second to top) and stops the script, failing if necessary//pushes true if verified
     */
    VERIFYSIG(0xc0), //

    // code
    CODESEPARATOR(0xc0),
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
