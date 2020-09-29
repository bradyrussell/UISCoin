package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.blockchain.BlockChainStorageBase;
import com.bradyrussell.uiscoin.blockchain.BlockChainStorageFile;
import com.bradyrussell.uiscoin.node.BlockHeaderResponse;
import com.bradyrussell.uiscoin.node.BlockRequest;
import com.bradyrussell.uiscoin.node.PeerPacketBuilder;
import com.bradyrussell.uiscoin.transaction.Transaction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ReceiveBlockRequestHandler extends SimpleChannelInboundHandler<BlockRequest> {
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
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, BlockRequest blockRequest) throws Exception {
        System.out.println("Handler Received block request " + Util.Base64Encode(blockRequest.BlockHash));
        if (!BlockChain.get().exists(blockRequest.BlockHash, blockRequest.bOnlyHeader ? BlockChainStorageFile.BlockHeadersDatabase : BlockChainStorageFile.BlocksDatabase)) {
            System.out.println("Not found in database! Discarding.");
            return;
        }

        System.out.println("Sending block" + (blockRequest.bOnlyHeader ? "header" : "") + "...");

        ChannelFuture channelFuture = channelHandlerContext.writeAndFlush(blockRequest.bOnlyHeader ? new BlockHeaderResponse(blockRequest.BlockHash, BlockChain.get().getBlockHeader(blockRequest.BlockHash)) : BlockChain.get().getBlock(blockRequest.BlockHash));
        channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
            if (!channelFuture1.isSuccess())
                channelFuture1.cause().printStackTrace();
        });
    }
}
