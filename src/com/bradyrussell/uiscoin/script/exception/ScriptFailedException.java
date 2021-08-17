/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.script.exception;

import com.bradyrussell.uiscoin.UISCoinException;

public class ScriptFailedException extends UISCoinException {
    public ScriptFailedException(String message) {
        super(message);
    }
}
