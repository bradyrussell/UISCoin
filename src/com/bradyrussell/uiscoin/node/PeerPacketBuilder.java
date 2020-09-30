package com.bradyrussell.uiscoin.node;

import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.transaction.Transaction;

import java.net.InetAddress;
import java.nio.ByteBuffer;

@Deprecated
public class PeerPacketBuilder {
    ByteBuffer buffer;

    public PeerPacketBuilder(int BufferSize) {
        buffer = ByteBuffer.allocate(BufferSize);
    }

    public PeerPacketBuilder putPacketHeader(PeerPacketType type){
        buffer.put(type.Header);
        return this;
    }

    public PeerPacketBuilder putData(byte[] Data){
        buffer.put(Data);
        return this;
    }

    public PeerPacketBuilder putSync(boolean bHeadersOnly, int BlockHeight){
        buffer.put(PeerPacketType.SYNC.Header);
        buffer.put((byte) (bHeadersOnly ? 1 : 0));
        buffer.putInt(BlockHeight);
        return this;
    }

    public PeerPacketBuilder putTransaction(Transaction transaction){
        buffer.put(PeerPacketType.TRANSACTION.Header);
        buffer.putInt(transaction.getSize());
        buffer.put(transaction.getBinaryData());
        return this;
    }

    public PeerPacketBuilder putBlock(Block block){
        buffer.put(PeerPacketType.BLOCK.Header);
        buffer.putInt(block.getSize());
        buffer.put(block.getBinaryData());
        return this;
    }

    public PeerPacketBuilder putPeer(InetAddress PeerAddress){
        buffer.put(PeerPacketType.PEER.Header);
        buffer.put((byte)PeerAddress.getAddress().length);
        buffer.put(PeerAddress.getAddress());
        return this;
    }

    public PeerPacketBuilder putGreeting(int Version){
        buffer.put(PeerPacketType.GREETING.Header);
        buffer.putInt(Version);
        return this;
    }

    public PeerPacketBuilder putHandshake(int Version){
        buffer.put(PeerPacketType.HANDSHAKE.Header);
        buffer.putInt(Version);
        return this;
    }

    public PeerPacketBuilder putPing(int Value){
        buffer.put(PeerPacketType.PING.Header);
        //buffer.putInt(Value);
        return this;
    }

    public PeerPacketBuilder putDisconnect(){
        buffer.put(PeerPacketType.DISCONNECT.Header);
        return this;
    }

    public byte[] get(){
        byte[] ret = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, ret, 0, buffer.position());
        return ret;
    }
}
