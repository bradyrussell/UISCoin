/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import java.net.InetSocketAddress;

import com.bradyrussell.uiscoin.node.PeerAddress;
import com.bradyrussell.uiscoin.node.PeerPacketType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NodeP2PPeerEncoder extends MessageToByteEncoder<PeerAddress> {
    @Override
    protected void encode(ChannelHandlerContext ctx, PeerAddress msg, ByteBuf out) throws Exception {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        if(inetSocketAddress.getAddress().equals(msg.getAddress()) && inetSocketAddress.getPort() == msg.getPort()) return; // don't send a peer to itself

        out.writeByte(PeerPacketType.PEER.Header);
        byte[] address = msg.getAddress().getAddress();
        out.writeInt(address.length);
        out.writeBytes(address);
        out.writeInt(msg.getPort());
    }
}
