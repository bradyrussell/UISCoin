package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.blockchain.BlockChainStorageBase;
import com.bradyrussell.uiscoin.node.Node;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Logger;

public class NodeP2PReceiveBlockHandler extends SimpleChannelInboundHandler<Block> {
    private static final Logger Log = Logger.getLogger(NodeP2PReceiveBlockHandler.class.getName());
    Node thisNode;

    public NodeP2PReceiveBlockHandler(Node thisNode) {
        this.thisNode = thisNode;
    }

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
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Block block) throws Exception {
        Log.info("Handler Received block "+ Util.Base64Encode(block.Header.getHash()));

        if(BlockChain.get().BlockHeight >= block.Header.BlockHeight && BlockChain.get().exists(block.Header.getHash(), BlockChainStorageBase.BlocksDatabase)){
            Log.info("Already have. Discarding...");
            return;
        }
        if(BlockChain.get().BlockHeight >= block.Header.BlockHeight) {
            Log.info("Block is on a shorter chain. Discarding...");

            // inform them of our longer chain
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(PeerPacketType.HEIGHT.Header);
            buf.writeInt(BlockChain.get().BlockHeight);
            channelHandlerContext.writeAndFlush(buf);

            return; // we are on a longer chain!
        }

        if(!block.Verify()) {
            block.DebugVerify();
            Log.info("Invalid block! Discarding.");
            return;
        }

        if (BlockChain.get().BlockHeight < block.Header.BlockHeight) {
            if(!block.VerifyTransactionsUnspent()) {
                Log.warning("Block contains spent transactions! Discarding!");
                return;
            }
        }

        Log.info("Storing block...");
        BlockChain.get().putBlock(block);

        Log.info("Rebroadcasting...");
        thisNode.BroadcastBlockToPeers(block);
    }
}
