package com.bradyrussell.uiscoin.blockchain.storage;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.blockchain.BlockchainStorage;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockchainStorageEphemeral implements BlockchainStorage {
    protected final ArrayList<Block> blocksByHeight = new ArrayList<>(); // this should be all that needs stored, everything else can be reconstructed
    private final HashMap<byte[], Block> blocks = new HashMap<>();
    private final HashMap<byte[], byte[]> blockHashesByTransaction = new HashMap<>();
    private final HashMap<byte[], Transaction> mempool = new HashMap<>();
    private final AtomicInteger blockheight = new AtomicInteger(-1);

    private final HashSet<TransactionOutputIdentifier> unspentTransactionOutputSet = new HashSet<>();
    private boolean computeUnspentTransactionOutputs = true; // should disable if getting blocks out of order, and then build at the end

    @Override
    public void buildUnspentTransactionOutputSet() {
        for (Block block : blocksByHeight) {
            for (Transaction transaction : block.Transactions) {
                for (TransactionInput input : transaction.Inputs) {
                    unspentTransactionOutputSet.remove(new TransactionOutputIdentifier(input.InputHash, input.IndexNumber)); //spent
                }
                for (int i = 0; i < transaction.Outputs.size(); i++) {
                    unspentTransactionOutputSet.add(new TransactionOutputIdentifier(transaction.getHash(), i));
                }
            }
        }
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
    public int getBlockHeight() {
        return blockheight.get();
    }

    @Override
    public List<Block> getBlockchain() {
        return blocksByHeight;
    }

    @Override
    public List<Block> getBlockchainRange(int beginHeight, int endHeight) throws NoSuchBlockException {
        if(beginHeight < 0 || beginHeight > blockheight.get() || endHeight < 0 || endHeight > blockheight.get() || beginHeight > endHeight) throw new NoSuchBlockException("");
        return blocksByHeight.subList(beginHeight, endHeight);
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
    public BlockHeader getBlockHeader(byte[] blockHash) throws NoSuchBlockException {
        if(!blocks.containsKey(blockHash)) throw new NoSuchBlockException("");
        return blocks.get(blockHash).Header;
    }

    @Override
    public BlockHeader getBlockHeaderByHeight(int height) throws NoSuchBlockException {
        return getBlockByHeight(height).Header;
    }

    @Override
    public Block getBlock(byte[] blockHash) throws NoSuchBlockException {
        if(!blocks.containsKey(blockHash)) throw new NoSuchBlockException("");
        return blocks.get(blockHash);
    }

    @Override
    public Block getBlockByHeight(int height) throws NoSuchBlockException {
        if(height < 0 || height > blockheight.get()) throw new NoSuchBlockException("");
        return blocksByHeight.get(height);
    }

    @Override
    public Block getBlockByTransaction(byte[] transactionHash) throws NoSuchBlockException, NoSuchTransactionException {
        if(!blockHashesByTransaction.containsKey(transactionHash)) throw new NoSuchTransactionException("");
        return getBlock(blockHashesByTransaction.get(transactionHash));
    }

    @Override
    public boolean putBlock(Block block) {
        blockheight.set(block.Header.BlockHeight);
        blocks.put(block.Header.getHash(), block);
        blocksByHeight.ensureCapacity(block.Header.BlockHeight + 1);
        blocksByHeight.set(block.Header.BlockHeight, block);

        for (Transaction transaction : block.Transactions) {
            blockHashesByTransaction.put(transaction.getHash(), block.getHash());

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
        return unspentTransactionOutputSet.contains(new TransactionOutputIdentifier(transactionHash, index));
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
        if(!mempool.containsKey(transactionHash)) throw new NoSuchTransactionException("");
        return mempool.get(transactionHash);
    }

    @Override
    public boolean putMempoolTransaction(Transaction transaction) {
        mempool.put(transaction.getHash(), transaction);
        return true;
    }

    @Override
    public boolean removeMempoolTransaction(byte[] transactionHash) {
        return mempool.remove(transactionHash) != null;
    }
}