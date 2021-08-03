/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.script.exception;

import com.bradyrussell.uiscoin.UISCoinException;

public class ScriptInvalidException extends UISCoinException {
    public ScriptInvalidException(String message) {
        super(message);
    }
}
