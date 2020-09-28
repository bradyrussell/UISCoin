package com.bradyrussell.uiscoin.netty.notworking;

import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.node.PeerPacketBuilder;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class NodeP2PSendGreetingHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ChannelFuture channelFuture = ctx.writeAndFlush(new PeerPacketBuilder(5).putGreeting(MagicBytes.ProtocolVersion.Value).get());
        System.out.println("Sending greeting!");

        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                System.out.println("Message sent");
                //ctx.close();
            }
        });
    }
}
