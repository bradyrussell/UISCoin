package com.bradyrussell.uiscoin.blockchain.exception;

import com.bradyrussell.uiscoin.UISCoinException;

public class NoSuchBlockException extends UISCoinException {
    public NoSuchBlockException(String message) {
        super(message);
    }
}
