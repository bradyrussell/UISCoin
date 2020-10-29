package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.transaction.Transaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * A RAM only BlockChain storage useful for unit tests or single run applications.
 */
public class BlockChainStorageEphemeral extends BlockChainStorageBase {
    private static final Logger Log = Logger.getLogger(BlockChainStorageEphemeral.class.getName());

    public HashMap<String, HashMap<byte[],byte[]>> Databases = new HashMap<>();

    private HashMap<byte[],byte[]> getDatabase(String DatabaseName){
        if(Databases.containsKey(DatabaseName)) return Databases.get(DatabaseName);
        HashMap<byte[],byte[]> newdb = new HashMap<>();
        Databases.put(DatabaseName,newdb);
        return newdb;
    }

    public ArrayList<Transaction> MemPool;

    @Override
    public boolean open() {
        if(exists(Hash.getSHA512Bytes("blockheight"), "blockheight")) {
            byte[] bytes = get(Hash.getSHA512Bytes("blockheight"), "blockheight");
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            HighestBlockHash = new byte[64];
            BlockHeight = buf.getInt();
            buf.get(HighestBlockHash);

            Log.info("Loaded blockchain " + (BlockHeight + 1) + " blocks long. Last block: " + Util.Base64Encode(HighestBlockHash));
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
        if(!MemPool.contains(t)) Log.warning("Mempool does not contain this transaction");
        MemPool.remove(t);
    }

    @Override
    public List<Transaction> getMempool() {
        return MemPool;
    }

    @Override
    public byte[] get(byte[] Key, String Database) {
        if(!exists(Key,Database)) {
            return null;
        }

        return getDatabase(Database).get(Key);
    }

    @Override
    public void put(byte[] Key, byte[] Value, String Database) {
        getDatabase(Database).put(Key,Value);
    }

    @Override
    public void remove(byte[] Key, String Database) {
        getDatabase(Database).remove(Key);
    }

    @Override
    public boolean exists(byte[] Key, String Database) {
        return getDatabase(Database).containsKey(Key);
    }

    @Override
    public List<byte[]> keys(String Database){
        return new ArrayList<>(getDatabase(Database).keySet());
    }

}
