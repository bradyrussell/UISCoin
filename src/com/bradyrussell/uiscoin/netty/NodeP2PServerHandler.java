package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.node.Node;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NodeP2PServerHandler extends ChannelInboundHandlerAdapter {
    Node node;
    public NodeP2PServerHandler(Node node) {
        this.node = node;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        if(node != null) {
            node.nodeClients.add(ctx.channel());
            System.out.println("Added new peer. Peer count: " + node.getPeers().size());
        }
    }
}
