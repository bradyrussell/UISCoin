/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import java.util.logging.Logger;

import com.bradyrussell.uiscoin.node.UISCoinNode;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NodeP2PServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger Log = Logger.getLogger(NodeP2PServerHandler.class.getName());
    final UISCoinNode node;
    public NodeP2PServerHandler(UISCoinNode node) {
        this.node = node;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        if(node != null) {
            node.nodeClients.add(ctx.channel());
            Log.info("Added new peer. Peer count: " + node.getPeers().size());
        }
    }
}
