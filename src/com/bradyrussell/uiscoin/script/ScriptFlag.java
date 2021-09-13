/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.script;

public enum ScriptFlag {
    /* Debugging Information */
    LINE(0x10),
    BEGINSECTION(0x11),
    ENDSECTION(0x12),
    VARIABLEDECLARATION(0x13),
    FUNCTIONDECLARATION(0x14),
    VARIABLEREFERENCE(0x15),
    FUNCTIONCALL(0x16),
    ;

    public final byte Bytecode;

    ScriptFlag(int bytecode) {
        Bytecode = (byte) bytecode;
    }
}
