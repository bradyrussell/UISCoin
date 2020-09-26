package com.bradyrussell.uiscoin.block;

import com.bradyrussell.uiscoin.transaction.CoinbaseTransaction;
import com.bradyrussell.uiscoin.transaction.Transaction;

import java.util.Collections;

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

    public BlockBuilder setCoinbase(CoinbaseTransaction coinbase){
        block.Coinbase = coinbase;
        return this;
    }

    public BlockBuilder addTransaction(Transaction transaction){
        block.Transactions.add(transaction);
        return this;
    }

    public BlockBuilder shuffleTransactions(){
        Collections.shuffle(block.Transactions);
        return this;
    }

    public Block get(){
        return block;
    }
}
