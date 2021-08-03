/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import java.net.InetAddress;
import java.util.logging.Logger;

import com.bradyrussell.uiscoin.node.UISCoinNode;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NodeP2PReceivePeerHandler extends SimpleChannelInboundHandler<InetAddress> {
    private static final Logger Log = Logger.getLogger(NodeP2PReceivePeerHandler.class.getName());
    private final UISCoinNode thisNode;

    public NodeP2PReceivePeerHandler(UISCoinNode thisNode) {
        this.thisNode = thisNode;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, InetAddress inetAddress) throws Exception {
        Log.info("Handler Received peer "+ inetAddress.getHostAddress());

        if(thisNode.getPeers().contains(inetAddress) || inetAddress.isLoopbackAddress() || thisNode.peersEverSeen.contains(inetAddress)) {
            Log.info("4 Already known, discarding...");
            return;
        }

        thisNode.connectToPeer(inetAddress);

        Log.info("Rebroadcasting...");
        thisNode.broadcastPeerToPeers(inetAddress);
    }
}
