package com.bradyrussell.uiscoin.netty.notworking;

import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.node.PeerPacketBuilder;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class NodeP2PSendGreetingHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
       // ByteBuf byteBuf = Unpooled.wrappedBuffer(new PeerPacketBuilder(5).putGreeting(MagicBytes.ProtocolVersion.Value).get());
        ChannelFuture channelFuture = ctx.writeAndFlush(PeerPacketType.GREETING);
        System.out.println("Sending greeting!");

        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if(!channelFuture.isSuccess()) {
                    System.out.println("FAIL");
                    channelFuture.cause().printStackTrace();
                }
                System.out.println("Greeting sent successfully!");
                //ctx.close();
            }
        });
        super.channelActive(ctx);
    }
}
