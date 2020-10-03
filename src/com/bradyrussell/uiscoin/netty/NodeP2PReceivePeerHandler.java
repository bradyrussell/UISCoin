package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.node.Node;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;
import java.util.logging.Logger;

public class NodeP2PReceivePeerHandler extends SimpleChannelInboundHandler<InetAddress> {
    private static final Logger Log = Logger.getLogger(NodeP2PReceivePeerHandler.class.getName());
    private final Node thisNode;

    public NodeP2PReceivePeerHandler(Node thisNode) {
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
        Log.info("Handler Received peer "+ Util.Base64Encode(inetAddress.getAddress()));

        if(thisNode.getPeers().contains(inetAddress)) {
            Log.info("4 Already known, discarding...");
            return;
        }

        thisNode.ConnectToPeer(inetAddress);

        Log.info("Rebroadcasting...");
        thisNode.BroadcastPeerToPeers(inetAddress);
    }
}
