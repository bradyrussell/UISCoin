package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.blockchain.storage.Blockchain;
import com.bradyrussell.uiscoin.node.BlockHeaderResponse;
import com.bradyrussell.uiscoin.node.BlockRequest;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Logger;

public class NodeP2PReceiveBlockRequestHandler extends SimpleChannelInboundHandler<BlockRequest> {
    private static final Logger Log = Logger.getLogger(NodeP2PReceiveBlockRequestHandler.class.getName());

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
        Log.info("Handler Received block request " + BytesUtil.base64Encode(blockRequest.BlockHash));

        if(!Blockchain.get().hasBlockHeader(blockRequest.BlockHash)) {
            System.out.println("Not found in database! Discarding.");
            return;
        }

        Log.info("Sending block" + (blockRequest.bOnlyHeader ? "header" : "") + "...");

        ChannelFuture channelFuture = channelHandlerContext.writeAndFlush(blockRequest.bOnlyHeader ? new BlockHeaderResponse(blockRequest.BlockHash, Blockchain.get().getBlockHeader(blockRequest.BlockHash)) : Blockchain.get().getBlock(blockRequest.BlockHash));
        channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
            if (!channelFuture1.isSuccess())
                channelFuture1.cause().printStackTrace();
        });
    }
}
