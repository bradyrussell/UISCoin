package com.bradyrussell.uiscoin.block;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionBuilder;
import com.bradyrussell.uiscoin.transaction.TransactionInputBuilder;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class BlockBuilder {
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

    public BlockBuilder setBlockHeight(int BlockHeigth){
        getOrCreateHeader().BlockHeight = BlockHeigth;
        return this;
    }

    public BlockBuilder setNonce(int Nonce){
        getOrCreateHeader().Nonce = Nonce;
        return this;
    }

    public BlockBuilder CalculateMerkleRoot(){
        getOrCreateHeader().HashMerkleRoot = block.CalculateMerkleRoot();
        return this;
    }

    public BlockBuilder setDifficultyTarget(int DifficultyTarget){
        getOrCreateHeader().DifficultyTarget = DifficultyTarget;
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

    public BlockBuilder addMemPoolTransactions(int SizeLimit){
        List<Transaction> mempool = BlockChain.get().getMempool();
        mempool.sort((a,b)->{
            long ASecondsOld = Instant.now().getEpochSecond() - a.TimeStamp;
            long BSecondsOld = Instant.now().getEpochSecond() - b.TimeStamp;

            return (int) ((a.getFees()*((ASecondsOld/600)+1)) - (b.getFees()*((BSecondsOld/600)+1))); // sort by fee but add a bonus multiplier for every 10 minutes old
        });

        int size = 0;
        for(Transaction t:mempool){
            if(!t.VerifyInputsUnspent()) {
                BlockChain.get().removeFromMempool(t);
                continue;
            }
            if((size + t.getSize()) < SizeLimit) {
                block.Transactions.add(t);
            } else {
                break;
            }
        }
        return this;
    }

    public BlockBuilder addCoinbase(Transaction transaction){
        block.addCoinbaseTransaction(transaction);
        return this;
    }

    public BlockBuilder addCoinbasePayToPublicKeyHash(byte[] PublicKeyHash){
        Transaction transaction = new TransactionBuilder().setVersion(1).setLockTime(0)
                .addInput(new TransactionInputBuilder().setInputTransaction(Hash.getSHA512Bytes("Anything I want"), block.Header.BlockHeight).setUnlockingScript(Hash.getSHA512Bytes("Anything I want")).get())
                .addOutput(new TransactionOutputBuilder().setPayToPublicKeyHash(PublicKeyHash).setAmount(Block.CalculateBlockReward(0)).get()).get();
        block.addCoinbaseTransaction(transaction);
        return this;
    }

    public BlockBuilder shuffleTransactions(){
        Transaction coinbase = block.Transactions.get(0);
        block.Transactions.remove(0);
        Collections.shuffle(block.Transactions);
        block.Transactions.add(0, coinbase);
        return this;
    }

    public Block get(){
        return block;
    }
}
