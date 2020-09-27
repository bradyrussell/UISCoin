package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.Transaction;

import java.util.Arrays;

public abstract class BlockChainStorageBase {
    public abstract Block getBlock(byte[] BlockHash);
    public abstract BlockHeader getBlockHeader(byte[] BlockHash);

    public abstract Transaction getTransaction(byte[] TransactionHash);

    public  Transaction getTransactionFromIndex(byte[] TransactionHash){
        Block block = getTransactionBlockFromIndex(TransactionHash);
        for(Transaction transaction:block.Transactions){
            if(Arrays.equals(transaction.getHash(), TransactionHash)) return transaction;
        }
        return null;
    }

    public abstract Block getTransactionBlockFromIndex(byte[] TransactionHash);

    public abstract void putBlock(Block block);
    public abstract void putBlockAndIndex(Block block);
    public abstract void putBlockHeader(BlockHeader blockHeader);

    public abstract void putTransaction(Transaction transaction);
    public abstract void putTransactionIndex(Transaction transaction, byte[] BlockHash);
}
