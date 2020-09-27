package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;

public class BlockChainStorageFile extends BlockChainStorageBase {
    @Override
    public Block getBlock(byte[] BlockHash) {
        if(!Files.exists(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(BlockHash)))) return null;

        try {
            byte[] Bytes = Files.readAllBytes(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(BlockHash)));

            Block block = new Block();
            block.setBinaryData(Bytes);
            return block;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public BlockHeader getBlockHeader(byte[] BlockHash) {
        if(!Files.exists(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(BlockHash)))) return null;

        try {
            byte[] Bytes = Files.readAllBytes(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(BlockHash)));

            BlockHeader blockHeader = new BlockHeader();
            blockHeader.setBinaryData(Bytes);
            return blockHeader;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Transaction getTransaction(byte[] TransactionHash) {
        if(!Files.exists(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(TransactionHash)))) return null;

        try {
            byte[] Bytes = Files.readAllBytes(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(TransactionHash)));

            Transaction transaction = new Transaction();
            transaction.setBinaryData(Bytes);
            return transaction;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

/*    @Override
    public Transaction getTransactionFromIndex(byte[] TransactionHash) {
        Block block = getTransactionBlockFromIndex(TransactionHash);
        for(Transaction transaction:block.Transactions){
            if(Arrays.equals(transaction.getHash(), TransactionHash)) return transaction;
        }
        return null;
    }*/

    @Override
    public Block getTransactionBlockFromIndex(byte[] TransactionHash) {
        try {
            byte[] Bytes = Files.readAllBytes(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(TransactionHash)));

            return getBlock(Bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void putBlock(Block block) {
        try {
            MakeDir("blockchain/");
            Files.write(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(block.getHash())),block.getBinaryData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void putBlockAndIndex(Block block) {
        putBlock(block);
        for(Transaction transaction:block.Transactions){
            putTransactionIndex(transaction, block.getHash());
        }
    }

    @Override
    public void putBlockHeader(BlockHeader blockHeader) {
        try {
            MakeDir("blockchain/");
            Files.write(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(blockHeader.getHash())),blockHeader.getBinaryData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void putTransaction(Transaction transaction) {

        try {
            MakeDir("blockchain/");
            Files.write(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(transaction.getHash())),transaction.getBinaryData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void putTransactionIndex(Transaction transaction, byte[] BlockHash) {
        try {
            MakeDir("blockchain/");
            Files.write(Path.of("blockchain/"+Base64.getUrlEncoder().encodeToString(transaction.getHash())),BlockHash);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void MakeDir(String s){
        try {
            Files.createDirectory(Path.of(s));
        } catch (IOException e) {
            /*e.printStackTrace();*/
        }
    }
}
