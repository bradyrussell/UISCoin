/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.blockchain.exception;

import com.bradyrussell.uiscoin.UISCoinException;

public class InvalidBlockException extends UISCoinException {
    public InvalidBlockException(String message) {
        super(message);
    }
}
