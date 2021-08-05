/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.blockchain.BlockchainStorage;

public interface VerifiableWithBlockchain {
    boolean verify(BlockchainStorage blockchain);
}
