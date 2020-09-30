package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockBuilder;
import com.bradyrussell.uiscoin.node.BlockRequest;
import com.bradyrussell.uiscoin.node.PeerPacketBuilder;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionBuilder;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class NodeP2PClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
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
        System.out.println("ACTIVE");
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
        System.out.println("Inactive");
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        System.out.println("READ");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("USER EVENT"); // using this to signal we are accepted and ready to send requests
    }
}