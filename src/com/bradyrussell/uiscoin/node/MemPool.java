package com.bradyrussell.uiscoin.node;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;
import com.bradyrussell.uiscoin.transaction.Transaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MemPool implements IBinaryData {
    public ArrayList<Transaction> pendingTransactions = new ArrayList<>();

    public void addPendingTransaction(Transaction transaction){
        if(!pendingTransactions.contains(transaction)) pendingTransactions.add(transaction);
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buffer = ByteBuffer.allocate(getSize()+2000);

        buffer.putInt(pendingTransactions.size());
        for(Transaction transaction: pendingTransactions){
            buffer.putInt(transaction.getSize());
            buffer.put(transaction.getBinaryData());
        }

        byte[] ret = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, ret, 0, buffer.position());
        return ret;
    }

    @Override
    public int setBinaryData(byte[] Data) {
        ByteBuffer buffer = ByteBuffer.wrap(Data);
        int NumEntries = buffer.getInt();
        for(int i = 0; i < NumEntries; i++){
            int EntrySize = buffer.getInt();
            byte[] EntryData = new byte[EntrySize];
            buffer.get(EntryData);
            Transaction transaction = new Transaction();
            transaction.setBinaryData(EntryData);
            pendingTransactions.add(transaction);
        }
        return buffer.position();
    }

    @Override
    public int getSize() {
        int n = 0;
        for(Transaction transaction: pendingTransactions){
            n+= transaction.getSize();
        }

        return n+(4 * pendingTransactions.size())+4;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
