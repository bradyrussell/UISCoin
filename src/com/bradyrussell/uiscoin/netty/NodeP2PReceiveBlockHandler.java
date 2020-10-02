package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.blockchain.BlockChainStorageBase;
import com.bradyrussell.uiscoin.node.Node;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NodeP2PReceiveBlockHandler extends SimpleChannelInboundHandler<Block> {
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
        System.out.println("Handler Received block "+ Util.Base64Encode(block.getHash()));

        if(BlockChain.get().exists(block.getHash(), BlockChainStorageBase.BlocksDatabase)){
            System.out.println("Already have. Discarding...");
            return;
        }
        if(BlockChain.get().BlockHeight >= block.Header.BlockHeight) {
            System.out.println("Block is on a shorter chain. Discarding...");

            // inform them of our longer chain
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(PeerPacketType.HEIGHT.Header);
            buf.writeInt(BlockChain.get().BlockHeight);
            channelHandlerContext.writeAndFlush(buf);

            return; // we are on a longer chain!
        }
        if(!block.Verify()) {
            block.DebugVerify();
            System.out.println("Invalid block! Discarding.");
            return;
        }

        System.out.println("Storing block...");
        BlockChain.get().putBlock(block);

        System.out.println("Rebroadcasting...");
        thisNode.BroadcastBlockToPeers(block);
    }
}
