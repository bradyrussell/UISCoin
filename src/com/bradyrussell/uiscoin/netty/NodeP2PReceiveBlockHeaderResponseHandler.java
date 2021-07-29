package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.blockchain.storage.Blockchain;
import com.bradyrussell.uiscoin.node.BlockHeaderResponse;
import com.bradyrussell.uiscoin.node.Node;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Logger;

public class NodeP2PReceiveBlockHeaderResponseHandler extends SimpleChannelInboundHandler<BlockHeaderResponse> {
    private static final Logger Log = Logger.getLogger(NodeP2PReceiveBlockHeaderResponseHandler.class.getName());

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
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, BlockHeaderResponse blockHeaderResponse) throws Exception {
        Log.info("Handler Received block header "+ BytesUtil.base64Encode(blockHeaderResponse.BlockHash));

        if(Blockchain.get().getBlockHeader(blockHeaderResponse.BlockHash) != null){
            Log.info("Already have. Discarding...");
            return;
        }

        if(!blockHeaderResponse.blockHeader.verify()) {
            Log.info("Invalid blockheader! Discarding.");
            return;
        }

        Log.info("Storing blockheader...");
        Blockchain.get().putBlockHeader(blockHeaderResponse.blockHeader);
    }
}
