package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.node.PeerPacketType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.net.InetAddress;

public class NodeP2PPacketEncoder extends MessageToByteEncoder<PeerPacketType> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, PeerPacketType peerPacketType, ByteBuf byteBuf) throws Exception {
        System.out.println("Encoding packet "+peerPacketType);
        byteBuf.writeByte(peerPacketType.Header);
        //byte[] address = inetAddress.getAddress();
        //byteBuf.writeInt(address.length);
        //byteBuf.writeBytes(address);
    }
}
