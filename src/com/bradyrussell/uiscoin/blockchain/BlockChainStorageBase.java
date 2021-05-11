package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.script.ScriptMatcher;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;

import java.util.*;
import java.util.logging.Logger;

public abstract class BlockChainStorageBase {
    private static final Logger Log = Logger.getLogger(BlockChainStorageBase.class.getName());

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

    public abstract byte[] get(byte[] Key, String Database);
    public abstract void put(byte[] Key, byte[] Value, String Database);
    public abstract void remove(byte[] Key, String Database);
    public abstract boolean exists(byte[] Key, String Database);
    public abstract List<byte[]> keys(String Database);

    public Block getBlock(byte[] BlockHash) throws NoSuchBlockException {
        Log.info("BlockHash = " + BytesUtil.Base64Encode(BlockHash));

        Block block = new Block();
        byte[] data = get(BlockHash, BlocksDatabase);
        if(data == null) throw new NoSuchBlockException("There was no Block with the hash: "+ BytesUtil.Base64Encode(BlockHash));
        block.setBinaryData(data);
        return block;
    }

    public BlockHeader getBlockHeader(byte[] BlockHash) throws NoSuchBlockException {
        String hash = BytesUtil.Base64Encode(BlockHash);
        Log.info("BlockHash = " + hash);

        BlockHeader blockHeader = new BlockHeader();
        byte[] data = get(BlockHash, BlockHeadersDatabase);
        if(data == null) throw new NoSuchBlockException("There was no Block Header with the hash: "+hash);
        blockHeader.setBinaryData(data);
        return blockHeader;
    }

    public Transaction getTransaction(byte[] TransactionHash) throws NoSuchTransactionException, NoSuchBlockException {
        Log.info("TransactionHash = " + BytesUtil.Base64Encode(TransactionHash));

        Block block = getBlockWithTransaction(TransactionHash);

        try {
            int Index = getIndexOfTransaction(TransactionHash);
            Transaction transaction = block.Transactions.get(Index);
            if(Arrays.equals(transaction.getHash(),TransactionHash)) return transaction;
        } catch (Exception ignored){

        }
        for(Transaction transaction:block.Transactions){
            if(Arrays.equals(transaction.getHash(), TransactionHash)) return transaction;
        }
        throw new NoSuchTransactionException("There was no transaction with the hash: "+ BytesUtil.Base64Encode(TransactionHash)+" in block: "+ BytesUtil.Base64Encode(block.Header.getHash()));
    }

    public Block getBlockWithTransaction(byte[] TransactionHash) throws NoSuchBlockException {
        Log.info("TransactionHash = " + BytesUtil.Base64Encode(TransactionHash));

        byte[] blockHash = get(TransactionHash, TransactionToBlockDatabase);
        if(blockHash == null) throw new NoSuchBlockException("There was no Transaction with the hash: "+ BytesUtil.Base64Encode(TransactionHash));
        return getBlock(blockHash);
    }

    public int getIndexOfTransaction(byte[] TransactionHash) throws NoSuchTransactionException {
        Log.info("TransactionHash = " + BytesUtil.Base64Encode(TransactionHash));

        byte[] bytes = get(BytesUtil.ConcatArray(TransactionHash, new byte[]{'I'}), TransactionToBlockDatabase);
        if(bytes == null) throw new NoSuchTransactionException("There was no Transaction Index with the hash: "+ BytesUtil.Base64Encode(TransactionHash));
        return BytesUtil.ByteArrayToNumber32(bytes);
    }

    public TransactionOutput getTransactionOutput(byte[] TransactionHash, int Index) throws NoSuchTransactionException, NoSuchBlockException {
        Log.info("TransactionHash = " + BytesUtil.Base64Encode(TransactionHash) + ", Index = " + Index);

        TransactionOutput unspentTransactionOutput = getUnspentTransactionOutput(TransactionHash, Index);
        if(unspentTransactionOutput != null) return unspentTransactionOutput;
        return getTransaction(TransactionHash).Outputs.get(Index);
    }

    public TransactionOutput getUnspentTransactionOutput(byte[] TransactionHash, int Index) throws NoSuchTransactionException {
        Log.info("TransactionHash = " + BytesUtil.Base64Encode(TransactionHash) + ", Index = " + Index);

        if(TransactionHash == null) throw new NoSuchTransactionException("There was no Transaction with the hash: null");
        TransactionOutput transactionOutput = new TransactionOutput();
        byte[] binaryData = get(BytesUtil.ConcatArray(TransactionHash, BytesUtil.NumberToByteArray32(Index)), TransactionOutputDatabase);

        if(binaryData == null) return null; // should we keep using this or use an exception? i dont want to use exceptions for business logic but it would be more clear
        transactionOutput.setBinaryData(binaryData);

        return transactionOutput;
    }

    public void putBlock(Block block) {
        byte[] headerHash = block.Header.getHash();
        put(headerHash, block.getBinaryData(), BlocksDatabase);
        putBlockHeader(block);
        ArrayList<Transaction> transactions = block.Transactions;
        for (int TransactionIndex = 0; TransactionIndex < transactions.size(); TransactionIndex++) {
            Transaction transaction = transactions.get(TransactionIndex);
//gonna break blocks need to do other way
            put(transaction.getHash(), headerHash, TransactionToBlockDatabase);
            put(BytesUtil.ConcatArray(transaction.getHash(), new byte[]{'I'}), BytesUtil.NumberToByteArray32(TransactionIndex), TransactionToBlockDatabase); // making second row for semi backwards compat. if this row doesnt exist then default to linear search

            if(TransactionIndex != 0) { // only for non coinbase transactions
                BlockChain.get().removeFromMempool(transaction); // remove from mempool
                for (TransactionInput input : transaction.Inputs) { //spend input UTXOs
                    removeUnspentTransactionOutput(input.InputHash, input.IndexNumber);
                }
            }

            ArrayList<TransactionOutput> outputs = transaction.Outputs;
            for (int i = 0; i < outputs.size(); i++) {
                putUnspentTransactionOutput(transaction.getHash(), i, outputs.get(i));
            }
        }
    }

    public void putBlockHeader(BlockHeader header) {
        if(BlockHeight < header.BlockHeight) {
            BlockHeight = header.BlockHeight;
            HighestBlockHash = header.getHash();
        }
        put(header.getHash(), header.getBinaryData(), BlockHeadersDatabase);
    }

    public void putBlockHeader(Block block) {
        if(BlockHeight < block.Header.BlockHeight) {
            BlockHeight = block.Header.BlockHeight;
            HighestBlockHash = block.Header.getHash();
        }
        put(block.Header.getHash(), block.Header.getBinaryData(), BlockHeadersDatabase);
    }

    public void putUnspentTransactionOutput(byte[] TransactionHash, int Index, TransactionOutput transactionOutput){
        put(BytesUtil.ConcatArray(TransactionHash, BytesUtil.NumberToByteArray32(Index)), transactionOutput.getBinaryData(), TransactionOutputDatabase);
    }

    public void removeUnspentTransactionOutput(byte[] TransactionHash, int Index){
        remove(BytesUtil.ConcatArray(TransactionHash, BytesUtil.NumberToByteArray32(Index)), TransactionOutputDatabase);
    }

    //use matchUTXOForP2PKHAddress(byte[] PublicKeyHash)
    @Deprecated()
    public ArrayList<byte[]> ScanUnspentOutputsToAddress(byte[] PublicKeyHash) {
        ArrayList<byte[]> utxo = new ArrayList<>();
        byte[] lockingScript = new TransactionOutputBuilder().setPayToPublicKeyHash(PublicKeyHash).get().LockingScript;

        for(byte[] UTXOHash:keys(TransactionOutputDatabase)){
            TransactionOutput output = new TransactionOutput();
            output.setBinaryData(get(UTXOHash,TransactionOutputDatabase));

            if(Arrays.equals(output.LockingScript,lockingScript)) {
                utxo.add(UTXOHash);
            }
        }
        return utxo;
    }

   // this returns UTXO, which are 64 bytes TransactionHash, 4 bytes Index
    public ArrayList<byte[]> matchUTXOForP2PKHAddress(byte[] PublicKeyHash) {
        ArrayList<byte[]> utxo = new ArrayList<>();

        ScriptMatcher matcherP2PK = ScriptMatcher.getMatcherP2PK();

        for(byte[] UTXOHash:keys(TransactionOutputDatabase)){
            TransactionOutput output = new TransactionOutput();
            output.setBinaryData(get(UTXOHash,TransactionOutputDatabase));

            if(matcherP2PK.match(output.LockingScript) && Arrays.equals(matcherP2PK.getPushData(0),PublicKeyHash)) {
                utxo.add(UTXOHash);
            }

        }
        return utxo;
    }

    public Block getBlockByHeight(int BlockHeight) throws NoSuchBlockException {
        if(BlockHeight < 0) throw new NoSuchBlockException("Block height begins at zero.");
        byte[] currentBlockHash = HighestBlockHash;
        while(getBlockHeader(currentBlockHash).BlockHeight != BlockHeight) {
            currentBlockHash = getBlockHeader(currentBlockHash).HashPreviousBlock;
        }
        return getBlock(currentBlockHash);
    }

    public List<Block> getBlockChainFromHeight(int BlockHeight) throws NoSuchBlockException {
        if(this.BlockHeight < BlockHeight) return new ArrayList<>();
        if(HighestBlockHash == null) return new ArrayList<>();
        if(BlockHeight < 0) return new ArrayList<>();
        byte[] currentBlockHash = HighestBlockHash;

        List<Block> blockchain = new ArrayList<>();

       // System.out.println("GetBlockChainFromHeight: CurrentBlockHash = "+Util.Base64Encode(currentBlockHash));

        while(getBlockHeader(currentBlockHash) != null && getBlockHeader(currentBlockHash).BlockHeight >= BlockHeight) {
            Block block = getBlock(currentBlockHash);
            blockchain.add(block);
            if(block.Header.BlockHeight == 0) break;
            currentBlockHash = getBlockHeader(currentBlockHash).HashPreviousBlock;
        }

        Collections.reverse(blockchain);
        return blockchain;
    }

    // merkle root the entire blockchain from BlockHeight
    public byte[] getBlockChainMerkleRoot(int BlockHeight) throws NoSuchBlockException {
        List<byte[]> hashes = new ArrayList<>();

        for(Block block:getBlockChainFromHeight(BlockHeight)){
            hashes.add(block.Header.getHash());
        }

        while(hashes.size() > 1) {
            hashes = Block.MerkleRootStep(hashes);
        }

        return hashes.get(0);
    }

}
