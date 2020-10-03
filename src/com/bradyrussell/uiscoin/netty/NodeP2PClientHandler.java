package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.node.BlockRequest;
import com.bradyrussell.uiscoin.node.PeerPacketBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Logger;

public class NodeP2PClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger Log = Logger.getLogger(NodeP2PClientHandler.class.getName());
    private ChannelHandlerContext ctx;

    @Deprecated
    public void SendBlockRequest(BlockRequest request){ // i feel like this is a threading issue
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
        ByteBuf wrappedBuffer = Unpooled.wrappedBuffer(new PeerPacketBuilder(5).putGreeting(1).get());
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