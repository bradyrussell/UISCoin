/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.bradyrussell.uiscoin.node.PeerPacketType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NodeP2PPeerEncoder extends MessageToByteEncoder<InetAddress> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, InetAddress inetAddress, ByteBuf byteBuf) throws Exception {
        if(((InetSocketAddress)channelHandlerContext.channel().remoteAddress()).getAddress().equals(inetAddress)) return; // dont send a peer to itself
        //System.out.println("Encoding peer");
        byteBuf.writeByte(PeerPacketType.PEER.Header);
        byte[] address = inetAddress.getAddress();
        byteBuf.writeInt(address.length);
        byteBuf.writeBytes(address);
    }
}
