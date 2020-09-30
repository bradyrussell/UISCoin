package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;

import java.util.*;

public abstract class BlockChainStorageBase {
    public static final String BlocksDatabase = "blocks";
    public static final String BlockHeadersDatabase = "headers";
    public static final String TransactionToBlockDatabase = "transaction_to_block";
    public static final String TransactionOutputDatabase = "unspent_transaction_outputs";

    public int BlockHeight = -1;
    public byte[] HighestBlockHash = null;

    public abstract boolean open();
    public abstract void close();

    public abstract void addToMempool(Transaction t);
    public abstract void removeFromMempool(Transaction t);
    public abstract List<Transaction> getMempool();

   // public abstract byte[] get(byte[] Key);
    public abstract byte[] get(byte[] Key, String Database);
    // public abstract void get(byte[] Key, byte[] Value);
    //public abstract void put(byte[] Key, byte[] Value);
    public abstract void put(byte[] Key, byte[] Value, String Database);
    public abstract void remove(byte[] Key, String Database);
    public abstract boolean exists(byte[] Key, String Database);
    public abstract List<byte[]> keys(String Database);

    public Block getBlock(byte[] BlockHash) {
        Block block = new Block();
        block.setBinaryData(get(BlockHash, BlocksDatabase));
        return block;
    }

    public BlockHeader getBlockHeader(byte[] BlockHash) {
        BlockHeader blockHeader = new BlockHeader();
        byte[] data = get(BlockHash, BlockHeadersDatabase);
        if(data == null) return null;
        blockHeader.setBinaryData(data);
        return blockHeader;
    }

    public Transaction getTransaction(byte[] TransactionHash){
        Block block = getBlock(get(TransactionHash, TransactionToBlockDatabase));
        for(Transaction transaction:block.Transactions){
            if(Arrays.equals(transaction.getHash(), TransactionHash)) return transaction;
        }
        return null;
    }

    public TransactionOutput getTransactionOutput(byte[] TransactionHash, int Index){
        TransactionOutput unspentTransactionOutput = getUnspentTransactionOutput(TransactionHash, Index);
        if(unspentTransactionOutput != null) return unspentTransactionOutput;
        return getTransaction(TransactionHash).Outputs.get(Index);
    }

    public TransactionOutput getUnspentTransactionOutput(byte[] TransactionHash, int Index){
        if(TransactionHash == null) System.out.println("null");
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
            BlockChain.get().removeFromMempool(transaction); // remove from mempool
            put(transaction.getHash(), block.getHash(), TransactionToBlockDatabase);
            for (TransactionInput input : transaction.Inputs) { //spend input UTXOs
                removeUnspentTransactionOutput(input.InputHash, input.IndexNumber);
            }
            ArrayList<TransactionOutput> outputs = transaction.Outputs;
            for (int i = 0; i < outputs.size(); i++) {
                putUnspentTransactionOutput(transaction.getHash(), i, outputs.get(i));
            }
        }
    }

    public void putBlockHeader(BlockHeader header, byte[] BlockHash) {
        if(BlockHeight < header.BlockHeight) {
            BlockHeight = header.BlockHeight;
            HighestBlockHash = BlockHash;
        }
        put(BlockHash, header.getBinaryData(), BlockHeadersDatabase);
    }

    public void putBlockHeader(Block block) {
        if(BlockHeight < block.Header.BlockHeight) {
            BlockHeight = block.Header.BlockHeight;
            HighestBlockHash = block.getHash();
        }
        put(block.getHash(), block.Header.getBinaryData(), BlockHeadersDatabase);
    }

    public void putUnspentTransactionOutput(byte[] TransactionHash, int Index, TransactionOutput transactionOutput){
        put(Util.ConcatArray(TransactionHash,Util.NumberToByteArray(Index)), transactionOutput.getBinaryData(), TransactionOutputDatabase);
    }

    public void removeUnspentTransactionOutput(byte[] TransactionHash, int Index){
        remove(Util.ConcatArray(TransactionHash,Util.NumberToByteArray(Index)), TransactionOutputDatabase);
    }

    public List<TransactionOutput> ScanUnspentOutputsToAddress(byte[] PublicKeyHash) {
        ArrayList<TransactionOutput> utxo = new ArrayList<>();

        for(byte[] UTXOHash:keys(TransactionOutputDatabase)){
            TransactionOutput output = new TransactionOutput();
            output.setBinaryData(get(UTXOHash,TransactionOutputDatabase));

            byte[] lockingScript = new TransactionOutputBuilder().setPayToPublicKeyHash(PublicKeyHash).get().LockingScript;
            if(Arrays.equals(output.LockingScript,lockingScript)) {
                utxo.add(output);
            }
        }
        return utxo;
    }

    public Block getBlockByHeight(int BlockHeight){
        if(BlockHeight < 0) return null;
        byte[] currentBlockHash = HighestBlockHash;
        while(getBlockHeader(currentBlockHash).BlockHeight != BlockHeight) {
            currentBlockHash = getBlockHeader(currentBlockHash).HashPreviousBlock;
        }
        return getBlock(currentBlockHash);
    }

    public List<Block> getBlockChainFromHeight(int BlockHeight){
        if(this.BlockHeight < BlockHeight) return new ArrayList<>();
        if(HighestBlockHash == null) return new ArrayList<>();
        if(BlockHeight < 0) return new ArrayList<>();
        byte[] currentBlockHash = HighestBlockHash;

        List<Block> blockchain = new ArrayList<>();

        System.out.println("GetBlockChainFromHeight: CurrentBlockHash = "+Util.Base64Encode(currentBlockHash));

        while(getBlockHeader(currentBlockHash) != null && getBlockHeader(currentBlockHash).BlockHeight >= BlockHeight) {
            blockchain.add(getBlock(currentBlockHash));
            currentBlockHash = getBlockHeader(currentBlockHash).HashPreviousBlock;
        }

        Collections.reverse(blockchain);
        return blockchain;
    }

}
