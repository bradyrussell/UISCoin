package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface BlockchainStorage {
    boolean open();
    boolean close();
    boolean isOperational();
    boolean verify();
    boolean verifyRange(int beginHeight, int endHeight) throws NoSuchBlockException;

    int getBlockHeight();
    List<Block> getBlockchain();
    List<Block> getBlockchainRange(int beginHeight, int endHeight) throws NoSuchBlockException;
    byte[] getHighestBlockHash() throws NoSuchBlockException;
    Block getHighestBlock() throws NoSuchBlockException;

    BlockHeader getBlockHeader(byte[] blockHash) throws NoSuchBlockException;
    BlockHeader getBlockHeaderByHeight(int height) throws NoSuchBlockException;

    Block getBlock(byte[] blockHash) throws NoSuchBlockException;
    Block getBlockByHeight(int height) throws NoSuchBlockException;
    Block getBlockByTransaction(byte[] transactionHash) throws NoSuchBlockException, NoSuchTransactionException;
    boolean putBlock(Block block);

    // optional for header only clients
    boolean putBlockHeader(BlockHeader blockHeader);

    Transaction getTransaction(byte[] transactionHash) throws NoSuchTransactionException, NoSuchBlockException;
    TransactionInput getTransactionInput(byte[] transactionHash, int index) throws NoSuchTransactionException, NoSuchBlockException;
    TransactionOutput getTransactionOutput(byte[] transactionHash, int index) throws NoSuchTransactionException, NoSuchBlockException;
    boolean isTransactionOutputSpent(byte[] transactionHash, int index);
    Set<TransactionOutputIdentifier> getUnspentTransactionOutputs();
    void buildUnspentTransactionOutputSet();

    Set<Transaction> getMempoolTransactions();
    Transaction getMempoolTransaction(byte[] transactionHash) throws NoSuchTransactionException;
    boolean putMempoolTransaction(Transaction transaction);
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
