package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.Transaction;

public abstract class BlockChainStorageBase {
    public abstract Block getBlock(byte[] BlockHash);
    public abstract BlockHeader getBlockHeader(byte[] BlockHash);

    public abstract Transaction getTransaction(byte[] TransactionHash);
    public abstract Transaction getTransactionFromIndex(byte[] TransactionHash);

    public abstract void putBlock(Block block);
    public abstract void putBlockAndIndex(Block block);
    public abstract void putBlockHeader(BlockHeader blockHeader);

    public abstract void putTransaction(Transaction transaction);
    public abstract void putTransactionIndex(Transaction transaction, byte[] BlockHash);
}
