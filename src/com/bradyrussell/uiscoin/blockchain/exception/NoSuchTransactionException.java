package com.bradyrussell.uiscoin.blockchain.exception;

public class NoSuchTransactionException extends Exception{

    public NoSuchTransactionException(String message) {
        super(message);
    }
}
