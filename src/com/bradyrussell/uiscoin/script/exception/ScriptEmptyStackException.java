package com.bradyrussell.uiscoin.script.exception;

import com.bradyrussell.uiscoin.UISCoinException;

public class ScriptEmptyStackException extends UISCoinException {
    public ScriptEmptyStackException(String message) {
        super(message);
    }
}
