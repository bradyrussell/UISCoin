package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.*;
import com.bradyrussell.uiscoin.blockchain.BlockChain;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

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
            n+=output.getSize()+4;
        }
        return n;
    }

    private int getInputSize(){
        int n = 0;
        for(TransactionInput input:Inputs){
            n+=input.getSize()+4;
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
        return 20 + getOutputSize() + getInputSize();
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

    public boolean VerifyCoinbase() {
        return VerifyInputs() && VerifyOutputs()
                && Inputs.size() == 0 && Outputs.size() > 0 && TimeStamp < Long.MAX_VALUE
                && getSize() < MagicNumbers.MaxTransactionSize.Value;
    }

    public void DebugVerify(){
        assert VerifyInputs();
        assert VerifyOutputs();
        assert (getFees()*MagicNumbers.MinSatPerByte.Value) > getSize();
        assert Inputs.size() > 0;
        assert Outputs.size() > 0;
        assert TimeStamp < Long.MAX_VALUE;
        assert getSize() < MagicNumbers.MaxTransactionSize.Value;
    }

    public void DebugVerifyCoinbase(){
        assert VerifyOutputs();
        assert Inputs.size() == 0;
        assert Outputs.size() > 0;
        assert TimeStamp < Long.MAX_VALUE;
        assert getSize() < MagicNumbers.MaxTransactionSize.Value;
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

    public long getInputTotal() {
        long amount = 0;
        for(TransactionInput input:Inputs){
            amount += BlockChain.get().getUnspentTransactionOutput(input.InputHash, input.IndexNumber).Amount;// Blockchain lookup : input.InputHash
        }
        return amount;
    }

    public long getOutputTotal() {
        long amount = 0;
        for(TransactionOutput output:Outputs){
            amount += output.Amount;
        }
        return amount;
    }

    public long getFees() {
        return getInputTotal() - getOutputTotal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Arrays.equals(getHash(), that.getHash());
    }
}
