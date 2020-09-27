package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class BlockChainStorageBase {
    public static final String BlocksDatabase = "blocks";
    public static final String BlockHeadersDatabase = "headers";
    public static final String TransactionToBlockDatabase = "transaction_to_block";
    public static final String TransactionOutputDatabase = "unspent_transaction_outputs";

   // public abstract byte[] get(byte[] Key);
    public abstract byte[] get(byte[] Key, String Database);
    // public abstract void get(byte[] Key, byte[] Value);
    //public abstract void put(byte[] Key, byte[] Value);
    public abstract void put(byte[] Key, byte[] Value, String Database);
    public abstract void remove(byte[] Key, String Database);

    public Block getBlock(byte[] BlockHash) {
        Block block = new Block();
        block.setBinaryData(get(BlockHash, BlocksDatabase));
        return block;
    }

    public BlockHeader getBlockHeader(byte[] BlockHash) {
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setBinaryData(get(BlockHash, BlockHeadersDatabase));
        return blockHeader;
    }

    public Transaction getTransaction(byte[] TransactionHash){
        Block block = getBlock(get(TransactionHash, TransactionToBlockDatabase));
        for(Transaction transaction:block.Transactions){
            if(Arrays.equals(transaction.getHash(), TransactionHash)) return transaction;
        }
        return null;
    }

    public TransactionOutput getUnspentTransactionOutput(byte[] TransactionHash, int Index){
        TransactionOutput transactionOutput = new TransactionOutput();
        byte[] binaryData = get(Util.ConcatArray(TransactionHash, Util.NumberToByteArray(Index)), TransactionOutputDatabase);
        if(binaryData == null) return null;
        transactionOutput.setBinaryData(binaryData);

        return transactionOutput;
    }

    public void putBlock(Block block) {
        put(block.getHash(), block.getBinaryData(), BlocksDatabase);
        putBlockHeader(block);
        for(Transaction transaction:block.Transactions){
            put(transaction.getHash(), block.getHash(), TransactionToBlockDatabase);
            ArrayList<TransactionOutput> outputs = transaction.Outputs;
            for (int i = 0; i < outputs.size(); i++) {
                putUnspentTransactionOutput(transaction.getHash(), i, outputs.get(i));
            }
        }
    }

    public void putBlockHeader(Block block) {
        put(block.getHash(), block.Header.getBinaryData(), BlockHeadersDatabase);
    }

    public void putUnspentTransactionOutput(byte[] TransactionHash, int Index, TransactionOutput transactionOutput){
        put(Util.ConcatArray(TransactionHash,Util.NumberToByteArray(Index)), transactionOutput.getBinaryData(), TransactionOutputDatabase);
    }

    public void removeUnspentTransactionOutput(byte[] TransactionHash, int Index){
        remove(Util.ConcatArray(TransactionHash,Util.NumberToByteArray(Index)), TransactionOutputDatabase);
    }

}
