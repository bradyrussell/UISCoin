package com.bradyrussell.uiscoin.script;

public enum ScriptOperator {
    NOP(0x00), // push null on the stack
    PUSH(0x01), // the next byte specifies the number of following bytes to put on the stack
    INSTRUCTION(0x02), // push the instruction counter value onto the stack

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
    APPEND(0x40), // combine the top two stack items
    LIMIT(0x41), // top stack item is a byte number of elements to trim the second-to-top stack array to
    REVERSE(0x42), // reverse the order of the top stack element
    SPLIT(0x43), // split top most stack
    COMBINE(0x44), // combine N (top of the stack) elements into one array. use depth before combine to do all
    LEN(0x45), // push the length of the top stack element on top of the stack WITHOUT removing it

    // boolean logic
    NOT(0x50),
    OR(0x51),
    AND(0x52),
    XOR(0x53),

    // push constants
    NULL(0x81), // push null onto the stack
    FALSE(0x82), // push 0 on the stack
    TRUE(0x83), // push 1 on the stack

    //stack operations
    DEPTH(0x90), // number of elements in stack
    DROP(0x91), // drop the top stack element
    DUP(0x92),// duplicate the top stack element
    SWAP(0x93), // swap the top 2 stack elements
    CLEAR(0x94),// clear the stack
    CLONE(0x95), // duplicate the entire stack
    FLIP(0x96), // reverse the entire stack
    SHIFTUP(0x97), // shift the entire stack, so the top element becomes last, second becomes first etc
    SHIFTDOWN(0x98), // shift the entire stack, so the top element becomes second, last becomes first etc
    PICK(0x99), // copy the Nth (top of the stack) element on the stack and push it onto the stack

    // returns
    VERIFY(0xa0),
    RETURN(0xa1),
    LOCKTIMEVERIFY(0xa2),

    //hashes
    SHA512(0xb0),

    // signature
    VERIFYSIG(0xc0), // gets the public key (top of stack) and the signature (second to top) and pushes true if verified

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
