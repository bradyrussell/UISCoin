package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.node.Node;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;

public class NodeP2PReceivePeerHandler extends SimpleChannelInboundHandler<InetAddress> {
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
        System.out.println("Handler Received peer "+ Util.Base64Encode(inetAddress.getAddress()));

        if(thisNode.getPeers().contains(inetAddress)) {
            System.out.println("4 Already known, discarding...");
            return;
        }

        thisNode.ConnectToPeer(inetAddress);

        System.out.println("Rebroadcasting...");
        thisNode.BroadcastPeerToPeers(inetAddress);
    }
}
