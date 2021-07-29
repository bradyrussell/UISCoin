package com.bradyrussell.uiscoin.blockchain.storage;

import com.bradyrussell.uiscoin.blockchain.BlockchainStorage;

import java.util.logging.Logger;

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
