package com.bradyrussell.uiscoin.block;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;
import com.bradyrussell.uiscoin.transaction.CoinbaseTransaction;
import com.bradyrussell.uiscoin.transaction.Transaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Block implements IBinaryData {
    public BlockHeader Header;
    public CoinbaseTransaction Coinbase;
    public ArrayList<Transaction> Transactions;

    public Block() {
        Transactions = new ArrayList<>();
    }

    public Block(BlockHeader header) {
        Header = header;
        Transactions = new ArrayList<>();
    }

    public Block(BlockHeader header, CoinbaseTransaction coinbase) {
        Header = header;
        Coinbase = coinbase;
        Transactions = new ArrayList<>();
    }

    public Block(BlockHeader header, CoinbaseTransaction coinbase, ArrayList<Transaction> transactions) {
        Header = header;
        Coinbase = coinbase;
        Transactions = transactions;
    }

    public Block addTransaction(Transaction transaction){
        Transactions.add(transaction);
        return this;
    }

    public Block addCoinbaseTransaction(CoinbaseTransaction coinbase){
        Coinbase = coinbase;
        return this;
    }


    private int getTransactionsSize(){
        int n = 0;
        for(Transaction transaction:Transactions){
            n+=transaction.getSize()+4;
        }
        return n;
    }

    public byte[] CalculateMerkleRoot(){
        ByteBuffer buf = ByteBuffer.allocate(Transactions.size()*64);
        for(Transaction transaction:Transactions){
            buf.put(transaction.getHash());
        }

        return Hash.getSHA512Bytes(buf.array());
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buf = ByteBuffer.allocate(getSize());

        buf.put(Header.getBinaryData());

        buf.put(Coinbase.getBinaryData());

        buf.putInt(Transactions.size()); // transaction list prefixed with number of transactions
        for(Transaction transaction:Transactions){
            buf.putInt(transaction.getSize()); // each transaction is prefixed with size
            buf.put(transaction.getBinaryData());
        }

        return buf.array();
    }


    @Override
    public void setBinaryData(byte[] Data) {
        ByteBuffer buffer = ByteBuffer.wrap(Data);

        byte[] header = new byte[148];
        buffer.get(header, 0, 148);
        Header = new BlockHeader();
        Header.setBinaryData(header);

        byte[] coinbase = new byte[140];
        buffer.get(coinbase, 0, 140);
        Coinbase = new CoinbaseTransaction();
        Coinbase.setBinaryData(coinbase);

        int TransactionsNum = buffer.getInt();

        for(int i = 0; i < TransactionsNum; i++){
            int TransactionLen = buffer.getInt();
            byte[] transactionBytes = new byte[TransactionLen];
            buffer.get(transactionBytes);
            Transaction t = new Transaction();
            t.setBinaryData(transactionBytes);

            Transactions.add(t);
        }
    }

    @Override
    public int getSize() {
        return 148+4+140+getTransactionsSize();
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
