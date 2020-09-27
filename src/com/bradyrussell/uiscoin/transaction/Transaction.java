package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;
import com.bradyrussell.uiscoin.IVerifiable;
import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.blockchain.BlockChain;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Transaction implements IBinaryData, IVerifiable {
    public int Version; // 4
    public ArrayList<TransactionInput> Inputs;
    public ArrayList<TransactionOutput> Outputs;
    public long TimeStamp; // 8

    public Transaction() {
        Inputs = new ArrayList<>();
        Outputs = new ArrayList<>();
    }

    public Transaction(int version, long timeStamp) {
        Version = version;
        TimeStamp = timeStamp;
        Inputs = new ArrayList<>();
        Outputs = new ArrayList<>();
    }

    public Transaction(int version, long timeStamp, ArrayList<TransactionInput> inputs, ArrayList<TransactionOutput> outputs) {
        Version = version;
        Inputs = inputs;
        Outputs = outputs;
        TimeStamp = timeStamp;
    }

    public Transaction addInput(TransactionInput Input){
        Inputs.add(Input);
        return this;
    }

    public Transaction addOutput(TransactionOutput Output){
        Outputs.add(Output);
        return this;
    }


    private int getOutputSize(){
        int n = 0;
        for(TransactionOutput output:Outputs){
            n+=output.getSize();
        }
        return n;
    }

    private int getInputSize(){
        int n = 0;
        for(TransactionInput input:Inputs){
            n+=input.getSize();
        }
        return n;
    }

    @Override
    public byte[] getBinaryData() {
        ByteBuffer buf = ByteBuffer.allocate(getSize());

        buf.putInt(Version);
        buf.putInt(Inputs.size());

        for(TransactionInput transactionInput:Inputs){
            buf.putInt(transactionInput.getSize());
            buf.put(transactionInput.getBinaryData());
        }

        buf.putInt(Outputs.size());

        for(TransactionOutput transactionOutput:Outputs){
            buf.putInt(transactionOutput.getSize());
            buf.put(transactionOutput.getBinaryData());
        }

        buf.putLong(TimeStamp);

        return buf.array();
    }

    @Override
    public int setBinaryData(byte[] Data) {
        ByteBuffer buffer = ByteBuffer.wrap(Data);

        Version = buffer.getInt();

        int NumInputs = buffer.getInt();

        for(int i = 0; i < NumInputs; i++){
            int InputLen = buffer.getInt();

            TransactionInput t = new TransactionInput();

            byte[] dst = new byte[InputLen];
            buffer.get(dst);

            t.setBinaryData(dst);

            Inputs.add(t);
        }

        int NumOutputs = buffer.getInt();

        for(int i = 0; i < NumOutputs; i++){
            int OutputLen = buffer.getInt();

            TransactionOutput o = new TransactionOutput();

            byte[] dst = new byte[OutputLen];
            buffer.get(dst);

            o.setBinaryData(dst);

            Outputs.add(o);
        }

        TimeStamp = buffer.getLong();
        return buffer.position();
    }

    @Override
    public int getSize() {
        return 28 + getOutputSize() + getInputSize();
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }

    @Override //https://www.oreilly.com/library/view/mastering-bitcoin/9781491902639/ch08.html
    public boolean Verify() {
        return VerifyInputs() && VerifyOutputs() && getFees()*MagicNumbers.MinSatPerByte.Value > getSize()
                && Inputs.size() > 0 && Outputs.size() > 0 && TimeStamp < Long.MAX_VALUE
                && getSize() < MagicNumbers.MaxTransactionSize.Value;
    }

    private boolean VerifyOutputs(){
        for(TransactionOutput output:Outputs){
            if(!output.Verify()) return false;
        }
        return true;
    }

    private boolean VerifyInputs(){
        for(TransactionInput input:Inputs){
            if(!input.Verify()) return false;
        }
        return true;
    }

    private long getInputTotal() {
        long amount = 0;
        for(TransactionInput input:Inputs){
            amount += BlockChain.get().getTransaction(input.InputHash).Outputs.get(input.IndexNumber).Amount;// Blockchain lookup : input.InputHash
        }
        return amount;
    }

    private long getOutputTotal() {
        long amount = 0;
        for(TransactionOutput output:Outputs){
            amount += output.Amount;
        }
        return amount;
    }

    private long getFees() {
        return getInputTotal() - getOutputTotal();
    }

}
