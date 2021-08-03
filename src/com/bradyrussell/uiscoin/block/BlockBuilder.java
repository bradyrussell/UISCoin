/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.block;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.blockchain.exception.InvalidBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.blockchain.storage.Blockchain;
import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptOperator;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionBuilder;
import com.bradyrussell.uiscoin.transaction.TransactionInputBuilder;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;

public class BlockBuilder {
    private static final Logger Log = Logger.getLogger(BlockBuilder.class.getName());

    Block block = new Block();

    private BlockHeader getOrCreateHeader(){
        if(block.Header == null) {
            block.Header = new BlockHeader();
        }
        return block.Header;
    }

    public BlockBuilder setVersion(int Version){
        getOrCreateHeader().Version = Version;
        return this;
    }

    public BlockBuilder setHashPreviousBlock(byte[] PreviousBlockHash){
        getOrCreateHeader().HashPreviousBlock = PreviousBlockHash;
        return this;
    }

    public BlockBuilder setTimestamp(long Time){
        getOrCreateHeader().Time = Time;
        return this;
    }

    public BlockBuilder setBlockHeight(int BlockHeight){
        getOrCreateHeader().BlockHeight = BlockHeight;
        return this;
    }

    public BlockBuilder setNonce(int Nonce){
        getOrCreateHeader().Nonce = Nonce;
        return this;
    }

    public BlockBuilder calculateMerkleRoot(){
        getOrCreateHeader().HashMerkleRoot = block.calculateMerkleRoot();
        return this;
    }

    public BlockBuilder setDifficultyTarget(int DifficultyTarget){
        getOrCreateHeader().DifficultyTarget = DifficultyTarget;
        return this;
    }

    public BlockBuilder calculateDifficultyTarget() throws NoSuchBlockException {
        Block lastBlock = Blockchain.get().getHighestBlock();
        setDifficultyTarget(BlockHeader.calculateDifficultyTarget(block.Header.Time - lastBlock.Header.Time, lastBlock.Header.DifficultyTarget));
        return this;
    }

    public BlockBuilder setCoinbase(Transaction coinbase){
        block.setCoinbaseTransaction(coinbase);
        return this;
    }

    public BlockBuilder addTransaction(Transaction transaction){
        block.Transactions.add(transaction);
        return this;
    }

    public BlockBuilder addMempoolTransactions(int SizeLimit){
        List<Transaction> mempool = new ArrayList<>(Blockchain.get().getMempoolTransactions());
        mempool.sort((a,b)->{
            long ASecondsOld = Instant.now().getEpochSecond() - a.TimeStamp;
            long BSecondsOld = Instant.now().getEpochSecond() - b.TimeStamp;

            try {
                return (int) ((b.getFees() * ((BSecondsOld / 600) + 1)) - (a.getFees() * ((ASecondsOld / 600) + 1))); // sort by fee but add a bonus multiplier for every 10 minutes old
            } catch (NoSuchTransactionException | NoSuchBlockException e) {
                e.printStackTrace();
                return 0;
            }
        });

        ArrayList<byte[]> toRemove = new ArrayList<>();

        int size = 0;
        for(Transaction t:mempool){
            try {
                if(!t.verify()  || !t.verifyInputsUnspent()) {
                    toRemove.add(t.getHash());
                    continue;
                }
            } catch (NoSuchTransactionException e) {
                e.printStackTrace();
            }

            if((size + t.getSize()) < SizeLimit) {
                block.Transactions.add(t);
            } else {
                break;
            }
        }

        for (byte[] transaction : toRemove) {
            Blockchain.get().removeMempoolTransaction(transaction);
        }
        return this;
    }

    public BlockBuilder addCoinbase(Transaction transaction){
        block.addCoinbaseTransaction(transaction);
        return this;
    }

    public BlockBuilder addCoinbasePayToPublicKeyHash(byte[] PublicKeyHash) throws NoSuchTransactionException, NoSuchBlockException, InvalidBlockException {
        return addCoinbasePayToPublicKeyHash(PublicKeyHash,"Default Coinbase Message");
    }
    public BlockBuilder addCoinbasePayToPublicKeyHash(byte[] PublicKeyHash, String CoinbaseMessage) throws NoSuchTransactionException, NoSuchBlockException, InvalidBlockException {
        if(block.Transactions.size() == 0) {
            Log.warning("Coinbase transaction should be added last in order to calculate fees!");
        }
        if(block.getFees() <= 0) {
            Log.warning("Coinbase transaction does not collect any fees!");
        }
        Transaction transaction = new TransactionBuilder().setVersion(1).setLockTime(0)
                .addInput(new TransactionInputBuilder().setInputTransaction(new byte[64], block.Header.BlockHeight).setUnlockingScript(new ScriptBuilder(68).push(Hash.getSHA512Bytes(CoinbaseMessage)).op(ScriptOperator.TRUE).op(ScriptOperator.VERIFY).get()).get())
                .addOutput(new TransactionOutputBuilder().setPayToPublicKeyHash(PublicKeyHash).setAmount(Block.calculateBlockReward(block.Header.BlockHeight) + block.getFees()).get()).get();
        block.addCoinbaseTransaction(transaction);
        return this;
    }

    public BlockBuilder shuffleTransactions(){
        Transaction coinbase = block.Transactions.get(0);
        block.Transactions.remove(0);
        Collections.shuffle(block.Transactions);
        block.Transactions.add(0, coinbase);
        calculateMerkleRoot();
        return this;
    }

    public Block get(){
        return block;
    }
}
