/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import java.util.logging.Logger;

import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.node.BlockRequest;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import com.bradyrussell.uiscoin.node.UISCoinNode;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NodeP2PClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger Log = Logger.getLogger(NodeP2PClientHandler.class.getName());
    private ChannelHandlerContext ctx;
    private final UISCoinNode node;

    public NodeP2PClientHandler(UISCoinNode node) {
        this.node = node;
    }

    @Deprecated
    public void SendBlockRequest(BlockRequest request){ // I feel like this is a threading issue
        ChannelFuture channelFuture = ctx.writeAndFlush(request);
        // wrappedBuffer.release();
        channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
            if(!channelFuture1.isSuccess())
                channelFuture1.cause().printStackTrace();
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        super.channelActive(ctx);
        Log.info("Client connection active.");
        ByteBuf wrappedBuffer = Unpooled.buffer();
        wrappedBuffer.writeByte(PeerPacketType.GREETING.Header);
        wrappedBuffer.writeInt(MagicBytes.ProtocolVersion.Value);
        wrappedBuffer.writeLong(node.nodeId);

        ChannelFuture channelFuture = ctx.writeAndFlush(wrappedBuffer);
        // wrappedBuffer.release();
        channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
            if(!channelFuture1.isSuccess())
                channelFuture1.cause().printStackTrace();
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Log.info("Client connection inactive.");
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
       // System.out.println("READ");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Log.info("Client connection is ready."); // using this to signal we are accepted and ready to send requests
    }
}