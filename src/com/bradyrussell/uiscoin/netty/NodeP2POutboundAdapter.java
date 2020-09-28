package com.bradyrussell.uiscoin.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class NodeP2POutboundAdapter extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        System.out.println("OutboundAdapter: " + msg.getClass());
    }
}

