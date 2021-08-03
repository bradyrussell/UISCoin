/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin;

public class UISCoinException extends Exception{
    public UISCoinException() {
    }

    public UISCoinException(String message) {
        super(message);
    }

    public UISCoinException(String message, Throwable cause) {
        super(message, cause);
    }

    public UISCoinException(Throwable cause) {
        super(cause);
    }

    public UISCoinException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
