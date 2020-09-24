package com.bradyrussell.uiscoin.script;

public enum ScriptOperator {
    NULL(0x00), // push null on the stack
    PUSH(0x01), // the next byte specifies the number of following bytes to put on the stack
    INSTRUCTION(0x02), // push the instruction counter value onto the stack

    NUMEQUAL(0x10),
    BYTESEQUAL(0x11),
    SHA512EQUAL(0x12), // does A == sha512(b)

    ADD(0x20),
    SUBTRACT(0x21),
    MULTIPLY(0x22),
    DIVIDE(0x23),

    NOT(0x31),
    OR(0x32),
    AND(0x33),

    NOP(0x81), // do nothing
    FALSE(0x82), // push 0 on the stack
    TRUE(0x83), // push 1 on the stack

    DEPTH(0x90),
    DROP(0x91),
    DUP(0x92),
    SWAP(0x93),

    VERIFY(0xa0),
    RETURN(0xa1),
    LOCKTIMEVERIFY(0xa2),

    SHA512(0xb0),

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
