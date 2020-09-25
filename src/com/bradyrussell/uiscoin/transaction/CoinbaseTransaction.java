package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.IBinaryData;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class CoinbaseTransaction implements IBinaryData {
    public int Version; // 4
    public ArrayList<TransactionOutput> Outputs;
    public long TimeStamp; // 8

    public CoinbaseTransaction() {
        Outputs = new ArrayList<>();
    }

    public CoinbaseTransaction(int version, long timeStamp) {
        Version = version;
        TimeStamp = timeStamp;
        Outputs = new ArrayList<>();
    }

    public CoinbaseTransaction(int version, long timeStamp, ArrayList<TransactionOutput> outputs) {
        Version = version;
        Outputs = outputs;
        TimeStamp = timeStamp;
    }

    public CoinbaseTransaction addOutput(TransactionOutput Output){
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


    @Override
    public byte[] getBinaryData() {
        ByteBuffer buf = ByteBuffer.allocate(getSize());

        buf.putInt(Version);

        buf.putInt(Outputs.size());

        for(TransactionOutput transactionOutput:Outputs){
            buf.putInt(transactionOutput.getSize());
            buf.put(transactionOutput.getBinaryData());
        }

        buf.putLong(TimeStamp);

        return buf.array();
    }

    @Override
    public void setBinaryData(byte[] Data) {
        ByteBuffer buffer = ByteBuffer.wrap(Data);

        Version = buffer.getInt();

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
    }

    @Override
    public int getSize() {
        return 28 + getOutputSize();
    }

    @Override
    public byte[] getHash() {
        return Hash.getSHA512Bytes(getBinaryData());
    }
}
