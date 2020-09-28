package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.node.PeerPacketBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NodeP2PSendGreetingHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new PeerPacketBuilder(5).putGreeting(MagicBytes.ProtocolVersion.Value).get());
        System.out.println("Sending greeting!");
    }
}
