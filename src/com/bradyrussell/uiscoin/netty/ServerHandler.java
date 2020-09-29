package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.node.Node;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    Node node;
    public ServerHandler(Node node) {
        this.node = node;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        if(node != null) {
            node.nodeClients.add(ctx.channel());
            System.out.println("Added new peer." + node.nodeClients.size());
        }
    }
}
