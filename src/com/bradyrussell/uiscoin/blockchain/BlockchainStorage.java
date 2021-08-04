/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.blockchain;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;

public interface BlockchainStorage {
    /**
     * Called once to perform any initialization before accessing the blockchain.
     * @return Whether initialization was successful.
     */
    boolean open();

    /**
     * Called once to perform any cleanup or saving after all blockchain operations have been completed.
     * @return Whether cleanup and saving was successful.
     */
    boolean close();

    /**
     * Called to determine whether it is safe to perform blockchain operations.
     * @return Whether it is safe to perform blockchain operations.
     */
    boolean isOperational();

    /**
     * Verify the validity of the entire blockchain.
     * @return Whether the entire blockchain is valid.
     */
    boolean verify();

    /**
     * Verify whether a range of the local blockchain is valid.
     * @param beginHeight The first block to validate, inclusive.
     * @param endHeight The last block to validate, inclusive.
     * @return Whether the range of blocks is valid.
     * @throws NoSuchBlockException If the range provided is not within the bounds of the local blockchain.
     */
    boolean verifyRange(int beginHeight, int endHeight) throws NoSuchBlockException;

    /**
     * Gets the current local block height, can be thought of as the index of the most recent block (zero-indexed).
     * @return The current local block height.
     */
    int getBlockHeight();

    /**
     * Returns an ordered list of the entire local blockchain.
     * @return An ordered list of the entire local blockchain.
     */
    List<Block> getBlockchain();

    /**
     * Returns an ordered list of a range of the local blockchain.
     * @param beginHeight The first block to include in the list, inclusive.
     * @param endHeight The last block to include in the list, inclusive.
     * @return Whether the range of blocks is valid.
     * @throws NoSuchBlockException If the range provided is not within the bounds of the local blockchain.
     */
    List<Block> getBlockchainRange(int beginHeight, int endHeight) throws NoSuchBlockException;

    /**
     * Returns the block hash of the most recent block in the local blockchain.
     * @return The block hash of the most recent block in the local blockchain.
     * @throws NoSuchBlockException If there are no blocks in the local blockchain.
     */
    byte[] getHighestBlockHash() throws NoSuchBlockException;

    /**
     * Returns the most recent block in the local blockchain.
     * @return The most recent block in the local blockchain.
     * @throws NoSuchBlockException If there are no blocks in the local blockchain.
     */
    Block getHighestBlock() throws NoSuchBlockException;

    /**
     * Returns whether the local blockchain contains a block header corresponding to the provided block hash.
     * @param blockHash The block hash to search by.
     * @return Whether the local blockchain contains a block header corresponding to the provided block hash.
     */
    boolean hasBlockHeader(byte[] blockHash);

    /**
     * Retrieves the block header associated with the specified block hash.
     * @param blockHash The block hash to search by.
     * @return The block header associated with the specified block hash.
     * @throws NoSuchBlockException If there was no block header associated with the provided block hash.
     */
    BlockHeader getBlockHeader(byte[] blockHash) throws NoSuchBlockException;

    /**
     * Retrieves the block header associated with the specified block height.
     * @param height The block height to search by.
     * @return The block header associated with the specified block height.
     * @throws NoSuchBlockException If there was no block header associated with the provided block height.
     */
    BlockHeader getBlockHeaderByHeight(int height) throws NoSuchBlockException;

    /**
     * Returns whether the local blockchain contains a block corresponding to the provided block hash.
     * @param blockHash The block hash to search by.
     * @return Whether the local blockchain contains a block corresponding to the provided block hash.
     */
    boolean hasBlock(byte[] blockHash);

    /**
     * Retrieves the block associated with the specified block hash.
     * @param blockHash The block hash to search by.
     * @return The block associated with the specified block hash.
     * @throws NoSuchBlockException If there was no block associated with the provided block hash.
     */
    Block getBlock(byte[] blockHash) throws NoSuchBlockException;

    /**
     * Retrieves the block associated with the specified block height.
     * @param height The block height to search by.
     * @return The block associated with the specified block height.
     * @throws NoSuchBlockException If there was no block associated with the provided block height.
     */
    Block getBlockByHeight(int height) throws NoSuchBlockException;

    /**
     * Retrieves the block associated with the specified transaction.
     * @param transactionHash The hash of the transaction to search by.
     * @return The block containing the specified transaction.
     * @throws NoSuchBlockException If there is no block associated with the specified transaction in the local blockchain.
     * @throws NoSuchTransactionException If there is no transaction with the specified hash in the local blockchain.
     */
    Block getBlockByTransaction(byte[] transactionHash) throws NoSuchBlockException, NoSuchTransactionException;

    /**
     * Store a block on the local blockchain.
     * @param block The block to store.
     * @return Whether the block was stored successfully.
     */
    boolean putBlock(Block block);

    /**
     * Store a block header on the local blockchain. Optional, for header only clients.
     * @param blockHeader The block header to store.
     * @return Whether the block header was stored successfully.
     */
    boolean putBlockHeader(BlockHeader blockHeader);

    /**
     * Returns whether a transaction with the specified hash exist on the local blockchain.
     * @param transactionHash The transaction hash to search by.
     * @return Whether a transaction with the specified hash exist on the local blockchain.
     */
    boolean hasTransaction(byte[] transactionHash);

    /**
     * Retrieve a transaction by its hash.
     * @param transactionHash The transaction hash to search by.
     * @return The transaction with the specified hash.
     * @throws NoSuchTransactionException If there is no transaction with that hash in the local blockchain.
     * @throws NoSuchBlockException If there is no block associated with the specified transaction in the local blockchain.
     */
    Transaction getTransaction(byte[] transactionHash) throws NoSuchTransactionException, NoSuchBlockException;

    /**
     * Returns the transaction input from the transaction hash and index.
     * @param transactionHash The transaction hash to search by.
     * @param index The input index.
     * @return The transaction input specified.
     * @throws NoSuchTransactionException If there is no transaction with that hash in the local blockchain.
     * @throws NoSuchBlockException If there is no block associated with the specified transaction in the local blockchain.
     */
    TransactionInput getTransactionInput(byte[] transactionHash, int index) throws NoSuchTransactionException, NoSuchBlockException;

    /**
     * Returns the transaction output from the transaction hash and index.
     * @param transactionHash The transaction hash to search by.
     * @param index The output index.
     * @return The transaction output specified.
     * @throws NoSuchTransactionException If there is no transaction with that hash in the local blockchain.
     * @throws NoSuchBlockException If there is no block associated with the specified transaction in the local blockchain.
     */
    TransactionOutput getTransactionOutput(byte[] transactionHash, int index) throws NoSuchTransactionException, NoSuchBlockException;

    /**
     * Returns whether the specified transaction output has been spent. Typically, this will check whether it exists in the UTXO set.
     * @param transactionHash The transaction hash to search by.
     * @param index The output index.
     * @return Whether the transaction output has been spent. (true for spent, false for unspent)
     */
    boolean isTransactionOutputSpent(byte[] transactionHash, int index);

    /**
     * Returns the local UTXO (unspent transaction output) set.
     * @return A set of TransactionOutputIdentifier representing the local UTXO set.
     */
    Set<TransactionOutputIdentifier> getUnspentTransactionOutputs();

    /**
     * Builds the local UTXO set from the entire blockchain.
     */
    void buildUnspentTransactionOutputSet();

    boolean hasMempoolTransaction(byte[] transactionHash);

    /**
     * Returns the current set of mempool (pending) transactions.
     * @return
     */
    Set<Transaction> getMempoolTransactions();

    /**
     * Returns the mempool transaction specified.
     * @param transactionHash The transaction hash to search by.
     * @return The specified mempool transaction.
     * @throws NoSuchTransactionException If the specified transaction does not exist in the mempool.
     */
    Transaction getMempoolTransaction(byte[] transactionHash) throws NoSuchTransactionException;

    /**
     * Stores the specified transaction in the mempool.
     * @param transaction The transaction to store in the mempool.
     * @return Whether the transaction was successfully stored in the mempool.
     */
    boolean putMempoolTransaction(Transaction transaction);

    /**
     * Remove the specified transaction from the mempool.
     * @param transactionHash The hash of the transaction to remove from the mempool.
     * @return Whether the transaction was removed from the mempool.
     */
    boolean removeMempoolTransaction(byte[] transactionHash);

    class TransactionOutputIdentifier {
        public byte[] transactionHash;
        public int index;

        public TransactionOutputIdentifier(byte[] transactionHash, int index) {
            this.transactionHash = transactionHash;
            this.index = index;
        }

        @Override
        public String toString() {
            return "TransactionOutputIdentifier{ hash:"+ BytesUtil.base64Encode(transactionHash) +", index: "+index+" }";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionOutputIdentifier that = (TransactionOutputIdentifier) o;
            return index == that.index && Arrays.equals(transactionHash, that.transactionHash);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(index);
            result = 31 * result + Arrays.hashCode(transactionHash);
            return result;
        }
    }
}
