package com.bradyrussell.uiscoin.block;

import com.bradyrussell.uiscoin.*;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static List<byte[]> MerkleRootStep(List<byte[]> Nodes) { // my interpretation of https://en.bitcoin.it/wiki/Protocol_documentation Merkle Trees header
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
        return Header.Verify() && VerifyTransactions() && VerifyBlockReward() && Hash.validateHash(getHash(), Header.DifficultyTarget) && getSize() < MagicNumbers.MaxBlockSize.Value;
    }

    public void DebugVerify(){
        System.out.println("Header verify: "+ Header.Verify());
       assert Header.Verify();
        System.out.println("Transactions verify: "+ VerifyTransactions());
       assert VerifyTransactions();
        System.out.println("BlockReward verify: "+ VerifyBlockReward());
       assert VerifyBlockReward();
        System.out.println("PoW verify: "+ Hash.validateHash(getHash(), Header.DifficultyTarget));
       assert Hash.validateHash(getHash(), Header.DifficultyTarget);
        System.out.println("Size verify: "+(getSize() < MagicNumbers.MaxBlockSize.Value));
       assert  getSize() < MagicNumbers.MaxBlockSize.Value;
    }

    private boolean VerifyTransactions(){
        ArrayList<byte[]> TransactionOutputs = new ArrayList<>();

        if(Header.BlockHeight != 0 && Transactions.size() < 2) {
            System.out.println("Too few transactions!");
            return false;
        }
        for (int i = 0; i < Transactions.size(); i++) {
            Transaction transaction = Transactions.get(i);
            if (i == 0)  {
                if (!transaction.VerifyCoinbase(Header.BlockHeight)) {
                    System.out.println("Failed coinbase verification!");
                    transaction.DebugVerifyCoinbase(Header.BlockHeight);
                    return false;
                }
            } else {

                for (TransactionInput input : transaction.Inputs) {
                    byte[] inputTXO = Util.ConcatArray(input.InputHash, Util.NumberToByteArray(input.IndexNumber));
                    for (byte[] transactionOutput : TransactionOutputs) {
                        if(Arrays.equals(inputTXO,transactionOutput)) {
                            System.out.println("Block contains duplicate Transaction Outputs! See transaction "+i+".");
                            return false; // this TXO is already in the block
                        }
                    }
                    TransactionOutputs.add(inputTXO);
                }

                if (!transaction.Verify()){
                    System.out.println("Transaction verification!");
                    transaction.DebugVerify();
                    return false;
                }
            }
        }
        return true;
    }

    public boolean VerifyTransactionsUnspent() {
        for (int i = 0; i < Transactions.size(); i++) {
            if (i != 0 && !Transactions.get(i).VerifyInputsUnspent()) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block that = (Block) o;
        return Arrays.equals(getHash(), that.getHash());
    }

    public static long CalculateBlockReward(int BlockHeight){
        int NumberOfHalvings = BlockHeight / 21000000;
        return Conversions.CoinsToSatoshis(50) >> NumberOfHalvings;
    }

    private boolean VerifyBlockReward(){
        Transaction coinbase = Transactions.get(0);
        if(coinbase == null) return false;
        return coinbase.getOutputTotal() <= CalculateBlockReward(Header.BlockHeight);
    }

}
