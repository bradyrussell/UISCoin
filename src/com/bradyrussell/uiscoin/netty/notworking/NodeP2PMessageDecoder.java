package com.bradyrussell.uiscoin.netty.notworking;

import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.node.PeerPacketBuilder;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import com.bradyrussell.uiscoin.transaction.Transaction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.net.InetAddress;
import java.util.List;

public class NodeP2PMessageDecoder extends ReplayingDecoder<Void>{
/*
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new PeerPacketBuilder(8).putPing(1));
    }
*/

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        PeerPacketType packetType = PeerPacketType.getByHeader(byteBuf.readByte());
        System.out.println("Decoding "+packetType+" packet.");

        if (packetType != null) {
            switch (packetType){
                case DISCONNECT -> {
                    channelHandlerContext.disconnect();
                    list.add(null);
                }
                case GREETING -> {
                    int Version = byteBuf.readInt();
                    if(Version != MagicBytes.ProtocolVersion.Value) {
                        channelHandlerContext.disconnect();
                        list.add(null);
                        return;
                    }
                    channelHandlerContext.write(new PeerPacketBuilder(8).putHandshake(MagicBytes.ProtocolVersion.Value).get());
                    list.add(null);
                }
                case HANDSHAKE -> {
                    int Version = byteBuf.readInt();
                    if(Version != MagicBytes.ProtocolVersion.Value) {
                        channelHandlerContext.disconnect();
                        list.add(null);
                        return;
                    }
                    list.add(null);
                }
                case PING -> {
                    list.add(null);
                }
                case PEER -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    InetAddress address = InetAddress.getByAddress(Bytes);
                    // Node.addPeer()

                    list.add(address);
                }
                case TRANSACTION -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    Transaction transaction = new Transaction();
                    transaction.setBinaryData(Bytes);

                    list.add(transaction);
                }
                case BLOCK -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    Block block = new Block();
                    block.setBinaryData(Bytes);

                    list.add(block);
                }
            }
        }
    }
}
