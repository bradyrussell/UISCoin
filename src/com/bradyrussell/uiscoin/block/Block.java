package com.bradyrussell.uiscoin.block;

import com.bradyrussell.uiscoin.*;
import com.bradyrussell.uiscoin.blockchain.exception.InvalidBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class Block implements IBinaryData, IVerifiable {
    private static final Logger Log = Logger.getLogger(Block.class.getName());

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

    public static List<byte[]> merkleRootStep(List<byte[]> Nodes) { // my interpretation of https://en.bitcoin.it/wiki/Protocol_documentation Merkle Trees header
        List<byte[]> ret = new ArrayList<>();

        for (int i = 0; i < Nodes.size(); i+=2) {
            byte[] arr = Nodes.get(i);

            if(i == Nodes.size()-1) {
                ret.add(Hash.getSHA512Bytes(BytesUtil.concatArray(arr, arr)));
            } else {
                ret.add(Hash.getSHA512Bytes(BytesUtil.concatArray(arr, Nodes.get(i+1))));
            }
        }

        return ret;
    }


    public byte[] calculateMerkleRoot(){
        if(Transactions.size() == 0) return new byte[0];

        List<byte[]> hashes = new ArrayList<>();

        for(Transaction transaction:Transactions){
            hashes.add(transaction.getHash());
        }

        while(hashes.size() > 1) {
            hashes = merkleRootStep(hashes);
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

    // for now ill leave this returning the header hash because the previous API used this as the block hash
    @Override @Deprecated
    public byte[] getHash() {
       // throw new IllegalStateException("Use of block.getHash()");
        return Header.getHash();
        //return Hash.getSHA512Bytes(getBinaryData());
    }

    @Override
    public boolean verify() {
        try {
            return Header.verify() && verifyTransactions() && verifyBlockReward() && verifyProofOfWork() && getSize() < MagicNumbers.MaxBlockSize.Value && Arrays.equals(Header.HashMerkleRoot, calculateMerkleRoot());
        } catch (NoSuchBlockException | NoSuchTransactionException | InvalidBlockException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void debugVerify() throws NoSuchTransactionException, NoSuchBlockException, InvalidBlockException {
        Log.warning("Header verify: "+ Header.verify());
       assert Header.verify();
        Log.warning("Transactions verify: "+ verifyTransactions());
       assert verifyTransactions();
        Log.warning("BlockReward verify: "+ verifyBlockReward()+Transactions.get(0).getOutputTotal()+" > " + calculateBlockReward(Header.BlockHeight)+"+" + getFees());
       assert verifyBlockReward();
        Log.warning("PoW verify: "+ Hash.validateHash(Header.getHash(), Header.DifficultyTarget));
       assert Hash.validateHash(Header.getHash(), Header.DifficultyTarget);
        Log.warning("Size verify: "+(getSize() < MagicNumbers.MaxBlockSize.Value));
       assert  getSize() < MagicNumbers.MaxBlockSize.Value;
       Log.warning("MerkleRoot Verify: "+Arrays.equals(Header.HashMerkleRoot, calculateMerkleRoot()));
        assert Arrays.equals(Header.HashMerkleRoot, calculateMerkleRoot());
    }

    private boolean verifyTransactions(){
        ArrayList<byte[]> TransactionOutputs = new ArrayList<>();

        if(Header.BlockHeight != 0 && Transactions.size() < 2) {
            Log.warning("Too few transactions!");
            return false;
        }
        for (int i = 0; i < Transactions.size(); i++) {
            Transaction transaction = Transactions.get(i);
            if (i == 0)  {
                if (!transaction.verifyCoinbase(Header.BlockHeight)) {
                    Log.warning("Failed coinbase verification!");
                    transaction.debugVerifyCoinbase(Header.BlockHeight);
                    return false;
                }
            } else {

                for (TransactionInput input : transaction.Inputs) {
                    byte[] inputTXO = BytesUtil.concatArray(input.InputHash, BytesUtil.numberToByteArray32(input.IndexNumber));
                    for (byte[] transactionOutput : TransactionOutputs) {
                        if(Arrays.equals(inputTXO,transactionOutput)) {
                            Log.warning("Block contains duplicate Transaction Outputs! See transaction "+i+".");
                            return false; // this TXO is already in the block
                        }
                    }
                    TransactionOutputs.add(inputTXO);
                }

                if (!transaction.verify()){
                    try {
                        transaction.debugVerify();
                    } catch (NoSuchTransactionException | NoSuchBlockException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            }
        }
        return true;
    }

    public boolean verifyTransactionsUnspent() {
        for (int i = 0; i < Transactions.size(); i++) {
            try {
                if (i != 0 && !Transactions.get(i).verifyInputsUnspent()) return false;
            } catch (NoSuchTransactionException e) {
                e.printStackTrace();
                return false;
            }
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

    public static long calculateBlockReward(int BlockHeight){
        int NumberOfHalvings = BlockHeight / 21000000;
        return Conversions.coinsToSatoshis(50) >> NumberOfHalvings;
    }

    private boolean verifyBlockReward() throws NoSuchTransactionException, NoSuchBlockException, InvalidBlockException {
        Transaction coinbase = Transactions.get(0);
        if(coinbase == null) return false;
        return coinbase.getOutputTotal() <= calculateBlockReward(Header.BlockHeight) + getFees();
    }

    public boolean verifyProofOfWork(){
        return Hash.validateHash(Header.getHash(), Header.DifficultyTarget);
    }

    public long getFees() throws NoSuchTransactionException, NoSuchBlockException, InvalidBlockException {
        if(Transactions.size() <= 0) return 0;
        long totalFees = 0;
        for (Transaction transaction : Transactions) {
            totalFees += Math.max(transaction.getFees(), 0);
        }
        if(totalFees < 0) throw new InvalidBlockException("This block has negative fees.");
        return totalFees;
    }
}
