package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.node.Node;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;

public class ReceivePeerHandler extends SimpleChannelInboundHandler<InetAddress> {
/*    private Node thisNode;

    public ReceivePeerHandler(Node thisNode) {
        this.thisNode = thisNode;
    }*/

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //System.err.printf("Factorial of %,d is: %,d%n", lastMultiplier, factorial);
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
       // thisNode.AddPeer(inetAddress);
    }
}
