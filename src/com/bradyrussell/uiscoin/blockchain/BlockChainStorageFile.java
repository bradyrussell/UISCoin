package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.transaction.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockChainStorageFile extends BlockChainStorageBase {
    public HashMap<byte[], Transaction> MemPool;

    @Override
    public boolean open() {
        for(byte[] highestBlock:keys("blockheight")){
            HighestBlockHash = highestBlock;
            BlockHeight = Util.ByteArrayToNumber(get(highestBlock, "blockheight"));
            System.out.println("Loaded blockchain "+(BlockHeight+1)+" blocks long. Last block: "+Util.Base64Encode(HighestBlockHash));
        }
        MemPool = new HashMap<>();
        return true;
    }

    @Override
    public void close() {
        if(HighestBlockHash != null && BlockHeight >= 0) put(HighestBlockHash, Util.NumberToByteArray(BlockHeight), "blockheight");
    }

    @Override
    public void addToMempool(Transaction t) {
        MemPool.put(t.getHash(), t);
    }

    @Override
    public void removeFromMempool(Transaction t) {
        MemPool.remove(t.getHash());
    }

    @Override
    public List<Transaction> getMempool() {
        return new ArrayList<>(MemPool.values());
    }

    @Override
    public byte[] get(byte[] Key, String Database) {
        if(!Files.exists(Path.of("blockchain/"+Database+"/"+ Util.Base64Encode(Key)))) {
            System.out.println("Path does not exist! "+"blockchain/"+Database+"/"+ Util.Base64Encode(Key));
            return null;
        }
        try {
            return Files.readAllBytes(Path.of("blockchain/"+Database+"/"+Util.Base64Encode(Key)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void put(byte[] Key, byte[] Value, String Database) {
        try {
            MakeDir("blockchain/");
            MakeDir("blockchain/"+Database+"/");
            Files.write(Path.of("blockchain/"+Database+"/"+Util.Base64Encode(Key)),Value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(byte[] Key, String Database) {
        if(!Files.exists(Path.of("blockchain/"+Database+"/"+ Util.Base64Encode(Key)))) {
            System.out.println("Path does not exist! "+"blockchain/"+Database+"/"+ Util.Base64Encode(Key));
            return;
        }
        try {
            Files.delete(Path.of("blockchain/"+Database+"/"+ Util.Base64Encode(Key)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean exists(byte[] Key, String Database) {
        return Files.exists(Path.of("blockchain/"+Database+"/"+ Util.Base64Encode(Key)));
    }

    @Override
    public List<byte[]> keys(String Database){
        if(!Files.exists(Path.of("blockchain/"+Database+"/"))) return new ArrayList<>();
        try (Stream<Path> stream = Files.walk(Path.of("blockchain/"+Database+"/"), 1)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(Util::Base64Decode)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


    private void MakeDir(String s){
        try {
            Files.createDirectory(Path.of(s));
        } catch (IOException ignored) {
        }
    }
}
