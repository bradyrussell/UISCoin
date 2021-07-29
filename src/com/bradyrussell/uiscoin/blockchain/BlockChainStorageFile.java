package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.transaction.Transaction;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockChainStorageFile extends BlockChainStorageBase {
    private static final Logger Log = Logger.getLogger(BlockChainStorageFile.class.getName());

    //public HashMap<byte[], Transaction> MemPool;
    public ArrayList<Transaction> MemPool;

    @Override
    public boolean open() {
        if(exists(Hash.getSHA512Bytes("blockheight"), "blockheight")) {
            byte[] bytes = get(Hash.getSHA512Bytes("blockheight"), "blockheight");
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            HighestBlockHash = new byte[64];
            BlockHeight = buf.getInt();
            buf.get(HighestBlockHash);

            Log.info("Loaded blockchain " + (BlockHeight + 1) + " blocks long. Last block: " + BytesUtil.Base64Encode(HighestBlockHash));
        }

        MemPool = new ArrayList<>();
        if(exists(Hash.getSHA512Bytes("mempool"), "mempool")) {
            byte[] bytes = get(Hash.getSHA512Bytes("mempool"), "mempool");
            ByteBuffer buf = ByteBuffer.wrap(bytes);

            int NumTransactions = buf.getInt();

            for(int i = 0; i < NumTransactions; i++){
                int TransactionLength = buf.getInt();
                byte[] TransactionBytes = new byte[TransactionLength];
                buf.get(TransactionBytes);

                Transaction t = new Transaction();
                t.setBinaryData(TransactionBytes);
                if(t.Verify()) MemPool.add(t);
            }
            Log.info("Loaded mempool with "+MemPool.size()+" transactions.");
        }

        return true;
    }

    private int getMempoolTransactionsSize(){
        int n = 0;
        for (Transaction transaction : MemPool) {
            n+=transaction.getSize();
        }
        return n;
    }

    @Override
    public void close() {
        if(HighestBlockHash != null && BlockHeight >= 0) {
            ByteBuffer buf = ByteBuffer.allocate(68);
            buf.putInt(BlockHeight);
            buf.put(HighestBlockHash);

            put(Hash.getSHA512Bytes("blockheight"), buf.array(), "blockheight");
        }

        ByteBuffer buf = ByteBuffer.allocate(4+(4*MemPool.size())+getMempoolTransactionsSize());

        buf.putInt(MemPool.size());
        for (Transaction transaction : MemPool) {
            buf.putInt(transaction.getSize());
            buf.put(transaction.getBinaryData());
        }
        put(Hash.getSHA512Bytes("mempool"), buf.array(), "mempool");
    }

    @Override
    public void addToMempool(Transaction t) {
        MemPool.add(t);
    }

    @Override
    public void removeFromMempool(Transaction t) {
        if(!MemPool.contains(t)) Log.warning("Mempool does not contain this transaction" + Base64.getUrlEncoder().encodeToString(t.getHash()));
        MemPool.remove(t);
        close(); // just putting this here to store the blockheight more often
    }

    @Override
    public List<Transaction> getMempool() {
        return MemPool;
    }

    @Override
    public byte[] get(byte[] Key, String Database) {
        if(!Files.exists(Path.of("blockchain/"+Database+"/"+ BytesUtil.Base64Encode(Key)))) {
            //System.out.println("Path does not exist! "+"blockchain/"+Database+"/"+ Util.Base64Encode(Key));
            return null;
        }
        try {
            return Files.readAllBytes(Path.of("blockchain/"+Database+"/"+ BytesUtil.Base64Encode(Key)));
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
            Files.write(Path.of("blockchain/"+Database+"/"+ BytesUtil.Base64Encode(Key)),Value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(byte[] Key, String Database) {
        if(!Files.exists(Path.of("blockchain/"+Database+"/"+ BytesUtil.Base64Encode(Key)))) {
            //System.out.println("Path does not exist! "+"blockchain/"+Database+"/"+ Util.Base64Encode(Key));
            return;
        }
        try {
            Files.delete(Path.of("blockchain/"+Database+"/"+ BytesUtil.Base64Encode(Key)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean exists(byte[] Key, String Database) {
        return Files.exists(Path.of("blockchain/"+Database+"/"+ BytesUtil.Base64Encode(Key)));
    }

    @Override
    public List<byte[]> keys(String Database){
        if(!Files.exists(Path.of("blockchain/"+Database+"/"))) return new ArrayList<>();
        try (Stream<Path> stream = Files.walk(Path.of("blockchain/"+Database+"/"), 1)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(BytesUtil::Base64Decode)
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
