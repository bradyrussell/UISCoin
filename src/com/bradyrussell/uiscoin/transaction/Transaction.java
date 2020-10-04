package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.*;
import com.bradyrussell.uiscoin.blockchain.BlockChain;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

public class Transaction implements IBinaryData, IVerifiable {
    private static final Logger Log = Logger.getLogger(Transaction.class.getName());
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
        return VerifyInputs() && VerifyOutputs() && getFees() > getSize()*MagicNumbers.MinSatPerByte.Value
                && Inputs.size() > 0 && Outputs.size() > 0 && TimeStamp < Long.MAX_VALUE
                && getSize() < MagicNumbers.MaxTransactionSize.Value && getFees() > 0 && getInputTotal() > 0 && getOutputTotal() > 0;
    }

    public boolean VerifyCoinbase(int BlockHeight) {
        return VerifyCoinbaseInputs() && VerifyOutputs()
                && Inputs.size() == 1 && Outputs.size() > 0 && TimeStamp < Long.MAX_VALUE
                && getSize() < MagicNumbers.MaxTransactionSize.Value && Inputs.get(0).IndexNumber == BlockHeight
                && Arrays.equals(Inputs.get(0).InputHash, new byte[64]);
    }

    public void DebugVerify(){
        Log.warning("VerifyInputs"+ VerifyInputs());
        assert VerifyInputs();
        Log.warning("VerifyOutputs"+ VerifyOutputs());
        assert VerifyOutputs();
        Log.warning("VerifyFees"+( getFees() > getSize()*MagicNumbers.MinSatPerByte.Value));
        assert getFees() > getSize()*MagicNumbers.MinSatPerByte.Value;
        Log.warning("VerifyInputsSize"+( Inputs.size() > 0));
        assert Inputs.size() > 0;
        Log.warning("VerifyOutputsSize"+( Outputs.size() > 0));
        assert Outputs.size() > 0;
        Log.warning("VerifyTimestamp"+ (TimeStamp < Long.MAX_VALUE));
        assert TimeStamp < Long.MAX_VALUE;
        Log.warning("VerifySize"+ (getSize() < MagicNumbers.MaxTransactionSize.Value));
        assert getSize() < MagicNumbers.MaxTransactionSize.Value;
        Log.warning("VerifyInputsFees>0"+ (getFees() > 0));
        assert getFees() > 0;
        Log.warning("VerifyInputs>0"+ (getInputTotal() > 0));
        assert getInputTotal() > 0;
        Log.warning("VerifyOutputs>0"+ (getOutputTotal() > 0));
        assert getOutputTotal() > 0;
    }

    public void DebugVerifyCoinbase(int BlockHeight){
        assert VerifyCoinbaseInputs();
        assert VerifyOutputs();
        assert Inputs.size() == 1;
        assert Inputs.get(0).IndexNumber == BlockHeight;
        assert Outputs.size() > 0;
        assert TimeStamp < Long.MAX_VALUE;
        assert getSize() < MagicNumbers.MaxTransactionSize.Value;
        assert Arrays.equals(Inputs.get(0).InputHash, new byte[64]);
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

    private boolean VerifyCoinbaseInputs(){
        for(TransactionInput input:Inputs){
            if(input.UnlockingScript.length > MagicNumbers.MaxUnlockingScriptLength.Value) return false;
        }
        return true;
    }

    public boolean VerifyInputsUnspent(){
        for(TransactionInput input:Inputs){
            if(BlockChain.get().getUnspentTransactionOutput(input.InputHash, input.IndexNumber) == null) {
                Log.info("Could not verify that transaction "+Util.Base64Encode(getHash())+" input "+Util.Base64Encode(input.InputHash)+" "+input.IndexNumber+" was UTXO!");
                return false;
            }
        }
        return true;
    }

    public long getInputTotal() {
        long amount = 0;
        for(TransactionInput input:Inputs){
           if(!Arrays.equals(input.InputHash, new byte[64])) // in case of coinbase transaction
            amount += BlockChain.get().getTransactionOutput(input.InputHash, input.IndexNumber).Amount;// Blockchain lookup : input.InputHash
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
