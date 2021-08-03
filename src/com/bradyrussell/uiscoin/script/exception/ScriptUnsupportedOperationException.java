/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.script.exception;

import com.bradyrussell.uiscoin.UISCoinException;

public class ScriptUnsupportedOperationException extends UISCoinException {
    public ScriptUnsupportedOperationException(String message) {
        super(message);
    }
}
