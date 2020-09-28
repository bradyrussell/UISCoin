package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.node.PeerPacketType;
import com.bradyrussell.uiscoin.transaction.Transaction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.net.InetAddress;

public class NodeP2PPeerEncoder extends MessageToByteEncoder<InetAddress> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, InetAddress inetAddress, ByteBuf byteBuf) throws Exception {
        byteBuf.writeByte(PeerPacketType.PEER.Header);
        byte[] address = inetAddress.getAddress();
        byteBuf.writeInt(address.length);
        byteBuf.writeBytes(address);
    }
}
