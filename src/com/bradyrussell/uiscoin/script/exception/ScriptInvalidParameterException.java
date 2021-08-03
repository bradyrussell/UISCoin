/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.script.exception;

import com.bradyrussell.uiscoin.UISCoinException;

public class ScriptInvalidParameterException extends UISCoinException {
    public ScriptInvalidParameterException(String message) {
        super(message);
    }
}
