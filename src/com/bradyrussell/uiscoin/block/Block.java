package com.bradyrussell.uiscoin.block;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;
import com.bradyrussell.uiscoin.IVerifiable;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.transaction.Transaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Block implements IBinaryData, IVerifiable {
    public BlockHeader Header;
    public ArrayList<Transaction> Transactions;

    public Block() {
        Transactions = new ArrayList<>();
    }

    public Block(BlockHeader header) {
        Header = header;
        Transactions = new ArrayList<>();
    }


    public Block(BlockHeader header, ArrayList<Transaction> transactions) {
        Header = header;
        Transactions = transactions;
    }

    public Block addTransaction(Transaction transaction){
        Transactions.add(transaction);
        return this;
    }

    public Block addCoinbaseTransaction(Transaction transaction){
        Transactions.add(0, transaction);
        return this;
    }

    public Block setCoinbaseTransaction(Transaction transaction){
        if(Transactions.size() < 1) {
            addCoinbaseTransaction(transaction);
        } else {
            Transactions.set(0, transaction);
        }
        return this;
    }

    private int getTransactionsSize(){
        int n = 0;
        for(Transaction transaction:Transactions){
            n+=transaction.getSize()+4;
        }
        return n;
    }

    private List<byte[]> MerkleRootStep(List<byte[]> Nodes) { // my interpretation of https://en.bitcoin.it/wiki/Protocol_documentation Merkle Trees header
        List<byte[]> ret = new ArrayList<>();

        for (int i = 0; i < Nodes.size(); i+=2) {
            byte[] arr = Nodes.get(i);

            if(i == Nodes.size()-1) {
                ret.add(Hash.getSHA512Bytes(Util.ConcatArray(arr, arr)));
            } else {
                ret.add(Hash.getSHA512Bytes(Util.ConcatArray(arr, Nodes.get(i+1))));
            }
        }

        return ret;
    }


    public byte[] CalculateMerkleRoot(){
        if(Transactions.size() == 0) return new byte[0];

        List<byte[]> hashes = new ArrayList<>();

        for(Transaction transaction:Transactions){
            hashes.add(transaction.getHash());
        }

        while(hashes.size() > 1) {
            hashes = MerkleRootStep(hashes);
        }

        return hashes.get(0);
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buf = ByteBuffer.allocate(getSize());

        buf.put(Header.getBinaryData());

        buf.putInt(Transactions.size()); // transaction list prefixed with number of transactions
        for(Transaction transaction:Transactions){
            buf.putInt(transaction.getSize()); // each transaction is prefixed with size
            buf.put(transaction.getBinaryData());
        }

        return buf.array();
    }


    @Override
    public int setBinaryData(byte[] Data) {
        ByteBuffer buffer = ByteBuffer.wrap(Data);

        Header = new BlockHeader();

        byte[] header = new byte[Header.getSize()];
        buffer.get(header, 0, Header.getSize());

        Header.setBinaryData(header);

/*        int coinbaseSize = buffer.getInt();
        byte[] coinbase = new byte[coinbaseSize];
        buffer.get(coinbase, 0, coinbaseSize);*/


        int TransactionsNum = buffer.getInt();

        for(int i = 0; i < TransactionsNum; i++){
            int TransactionLen = buffer.getInt();
            byte[] transactionBytes = new byte[TransactionLen];
            buffer.get(transactionBytes);
            Transaction t = new Transaction();
            t.setBinaryData(transactionBytes);

            Transactions.add(t);
        }
        return buffer.position();
    }

    @Override
    public int getSize() {
        return Header.getSize()+getTransactionsSize()+4+4;
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }

    @Override
    public boolean Verify() {
        return Header.Verify() && VerifyTransactions() && Hash.validateHash(getHash(), Header.DifficultyTarget);
    }

    private boolean VerifyTransactions(){
        for(Transaction transaction:Transactions){
            if(!transaction.Verify()) return false;
        }
        return true;
    }
}
