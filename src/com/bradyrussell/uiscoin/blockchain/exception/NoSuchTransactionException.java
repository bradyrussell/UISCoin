package com.bradyrussell.uiscoin.blockchain.exception;

import com.bradyrussell.uiscoin.UISCoinException;

public class NoSuchTransactionException extends UISCoinException {

    public NoSuchTransactionException(String message) {
        super(message);
    }
}
