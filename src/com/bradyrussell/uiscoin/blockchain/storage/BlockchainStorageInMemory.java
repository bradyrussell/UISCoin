/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.blockchain.storage;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.blockchain.BlockchainStorage;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;

public class BlockchainStorageInMemory implements BlockchainStorage {
    protected final ArrayList<Block> blocksByHeight = new ArrayList<>(); // this should be all that needs to be stored, everything else can be reconstructed
    private final HashMap<String, Block> blocks = new HashMap<>();
    private final HashMap<String, byte[]> blockHashesByTransaction = new HashMap<>();
    private final HashMap<String, Transaction> mempool = new HashMap<>();
    private final AtomicInteger blockheight = new AtomicInteger(-1);

    private final HashSet<TransactionOutputIdentifier> unspentTransactionOutputSet = new HashSet<>();
    private final boolean computeUnspentTransactionOutputs = true; // should disable if getting blocks out of order, and then build at the end

    @Override
    public void buildUnspentTransactionOutputSet() {
        unspentTransactionOutputSet.clear();
        for (Block block : blocksByHeight) {
            System.out.println("Block "+block.Header.BlockHeight);
            for (Transaction transaction : block.Transactions) {
                System.out.println("Transaction "+Base64.getUrlEncoder().encodeToString(transaction.getHash()));
                for (TransactionInput input : transaction.Inputs) {
                    unspentTransactionOutputSet.remove(new TransactionOutputIdentifier(input.InputHash, input.IndexNumber)); //spent
                    System.out.println("<-- Spending output: "+Base64.getUrlEncoder().encodeToString(input.InputHash)+" : "+input.IndexNumber);
                }
                for (int i = 0; i < transaction.Outputs.size(); i++) {
                    unspentTransactionOutputSet.add(new TransactionOutputIdentifier(transaction.getHash(), i));
                    System.out.println("--> Adding output: "+Base64.getUrlEncoder().encodeToString(transaction.getHash())+" : "+i);
                }
            }
        }
        System.out.println("Full UTXO set is as follows: ");
        for (TransactionOutputIdentifier transactionOutputIdentifier : unspentTransactionOutputSet) {
            System.out.println(Base64.getUrlEncoder().encodeToString(transactionOutputIdentifier.transactionHash)+" : "+transactionOutputIdentifier.index);
        }
        System.out.println("Built UTXO set with "+unspentTransactionOutputSet.size()+" UTXO.");
    }

    @Override
    public boolean hasMempoolTransaction(byte[] transactionHash) {
        return mempool.containsKey(BytesUtil.base64Encode(transactionHash));
    }

    @Override
    public boolean open() {
        return true;
    }

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public boolean isOperational() {
        return true;
    }

    @Override
    public boolean verify() {
        for (Block block : getBlockchain()) {
            if(!block.verify(this)) return false;
        }
        return true;
    }

    @Override
    public boolean verifyRange(int beginHeight, int endHeight) throws NoSuchBlockException {
        for (Block block : getBlockchainRange(beginHeight, endHeight)) {
            if(!block.verify(this)) return false;
        }
        return true;
    }

    @Override
    public int getBlockHeight() {
        return blockheight.get();
    }

    @Override
    public List<Block> getBlockchain() {
        return blocksByHeight;
    }

    @Override
    public List<Block> getBlockchainRange(int beginHeight, int endHeight) throws NoSuchBlockException {
        if(beginHeight < 0 || beginHeight > blockheight.get() || endHeight < 0 || endHeight > blockheight.get() || beginHeight > endHeight) throw new NoSuchBlockException("Cannot find block");
        return blocksByHeight.subList(beginHeight, endHeight+1);
    }

    @Override
    public byte[] getHighestBlockHash() throws NoSuchBlockException {
        return getBlockHeaderByHeight(blockheight.get()).getHash();
    }

    @Override
    public Block getHighestBlock() throws NoSuchBlockException {
        return getBlockByHeight(blockheight.get());
    }

    @Override
    public boolean hasBlockHeader(byte[] blockHash) {
        return hasBlock(blockHash);
    }

    @Override
    public BlockHeader getBlockHeader(byte[] blockHash) throws NoSuchBlockException {
        if(!blocks.containsKey(BytesUtil.base64Encode(blockHash))) throw new NoSuchBlockException("Cannot find block header for block "+BytesUtil.base64Encode(blockHash));
        return blocks.get(BytesUtil.base64Encode(blockHash)).Header;
    }

    @Override
    public BlockHeader getBlockHeaderByHeight(int height) throws NoSuchBlockException {
        return getBlockByHeight(height).Header;
    }

    @Override
    public boolean hasBlock(byte[] blockHash) {
        return blocks.containsKey(BytesUtil.base64Encode(blockHash));
    }

    @Override
    public Block getBlock(byte[] blockHash) throws NoSuchBlockException {
        if(!blocks.containsKey(BytesUtil.base64Encode(blockHash))) throw new NoSuchBlockException("Cannot find block "+BytesUtil.base64Encode(blockHash));
        return blocks.get(BytesUtil.base64Encode(blockHash));
    }

    @Override
    public Block getBlockByHeight(int height) throws NoSuchBlockException {
        if(height < 0 || height > blockheight.get()) throw new NoSuchBlockException("Cannot find block "+height);
        return blocksByHeight.get(height);
    }

    @Override
    public Block getBlockByTransaction(byte[] transactionHash) throws NoSuchBlockException, NoSuchTransactionException {
        if(!blockHashesByTransaction.containsKey(BytesUtil.base64Encode(transactionHash))) throw new NoSuchTransactionException("Cannot find transaction "+BytesUtil.base64Encode(transactionHash));
        return getBlock(blockHashesByTransaction.get(BytesUtil.base64Encode(transactionHash)));
    }

    @Override
    public boolean putBlock(Block block) {
        blockheight.set(block.Header.BlockHeight);
        System.out.println("Block Height is now "+blockheight.get());
        blocks.put(BytesUtil.base64Encode(block.Header.getHash()), block);
        while(blocksByHeight.size() < block.Header.BlockHeight + 1) blocksByHeight.add(null);
        blocksByHeight.set(block.Header.BlockHeight, block);

        for (Transaction transaction : block.Transactions) {
            blockHashesByTransaction.put(BytesUtil.base64Encode(transaction.getHash()), block.Header.getHash());

            if(computeUnspentTransactionOutputs) {
                for (TransactionInput input : transaction.Inputs) {
                    unspentTransactionOutputSet.remove(new TransactionOutputIdentifier(input.InputHash, input.IndexNumber));
                }
                for (int i = 0; i < transaction.Outputs.size(); i++) {
                    unspentTransactionOutputSet.add(new TransactionOutputIdentifier(transaction.getHash(), i));
                }
            }
        }

        return true;
    }

    @Override
    public boolean putBlockHeader(BlockHeader blockHeader) {
        return true;
    }

    @Override
    public boolean hasTransaction(byte[] transactionHash) {
        return blockHashesByTransaction.containsKey(BytesUtil.base64Encode(transactionHash));
    }

    @Override
    public Transaction getTransaction(byte[] transactionHash) throws NoSuchTransactionException, NoSuchBlockException {
        return getBlockByTransaction(transactionHash).Transactions.stream().filter(transaction -> Arrays.equals(transaction.getHash(), transactionHash)).findAny().orElseThrow();
    }

    @Override
    public TransactionInput getTransactionInput(byte[] transactionHash, int index) throws NoSuchTransactionException, NoSuchBlockException {
        return getTransaction(transactionHash).Inputs.get(index);
    }

    @Override
    public TransactionOutput getTransactionOutput(byte[] transactionHash, int index) throws NoSuchTransactionException, NoSuchBlockException {
        return getTransaction(transactionHash).Outputs.get(index);
    }

    @Override
    public boolean isTransactionOutputSpent(byte[] transactionHash, int index) {
        return !unspentTransactionOutputSet.contains(new TransactionOutputIdentifier(transactionHash, index));
    }

    @Override
    public Set<TransactionOutputIdentifier> getUnspentTransactionOutputs() {
        return unspentTransactionOutputSet;
    }

    @Override
    public Set<Transaction> getMempoolTransactions() {
        return new HashSet<>(mempool.values());
    }

    @Override
    public Transaction getMempoolTransaction(byte[] transactionHash) throws NoSuchTransactionException {
        if(!mempool.containsKey(BytesUtil.base64Encode(transactionHash))) throw new NoSuchTransactionException("Cannot find transaction "+BytesUtil.base64Encode(transactionHash));
        return mempool.get(BytesUtil.base64Encode(transactionHash));
    }

    @Override
    public boolean putMempoolTransaction(Transaction transaction) {
        mempool.put(BytesUtil.base64Encode(transaction.getHash()), transaction);
        return true;
    }

    @Override
    public boolean removeMempoolTransaction(byte[] transactionHash) {
        return mempool.remove(BytesUtil.base64Encode(transactionHash)) != null;
    }
}
