/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.blockchain.storage;

import java.util.logging.Logger;

import com.bradyrussell.uiscoin.blockchain.BlockchainStorage;

public class Blockchain {
    private static final Logger Log = Logger.getLogger(Blockchain.class.getName());
    private static BlockchainStorage storage;

    public static void initialize(BlockchainStorage blockchainStorage) {
        storage = blockchainStorage;
    }

    public static BlockchainStorage get() {
        return storage;
    }
}
