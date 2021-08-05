/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import java.util.logging.Logger;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import com.bradyrussell.uiscoin.node.UISCoinNode;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NodeP2PReceiveBlockHandler extends SimpleChannelInboundHandler<Block> {
    private static final Logger Log = Logger.getLogger(NodeP2PReceiveBlockHandler.class.getName());
    private final UISCoinNode thisNode;

    public NodeP2PReceiveBlockHandler(UISCoinNode thisNode) {
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
        Log.info("Handler Received block " + BytesUtil.base64Encode(block.Header.getHash()));

        int currentBlockHeight = thisNode.getBlockchain().getBlockHeight();
        if (currentBlockHeight >= block.Header.BlockHeight) {
            if (thisNode.getBlockchain().hasBlock(block.Header.getHash())) {
                Log.info("Already have. Discarding...");
            } else {
                Log.info("Block is on a shorter chain. Discarding...");

                // inform them of our longer chain
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(PeerPacketType.HEIGHT.Header);
                buf.writeInt(currentBlockHeight);
                channelHandlerContext.writeAndFlush(buf);
            }
        } else {
            // todo handle reorgs, right now they will only be accepted on restart

            if (!block.verify(thisNode.getBlockchain())) {
                block.debugVerify(thisNode.getBlockchain());
                Log.info("Invalid block! Discarding.");
                return;
            }

            if (!block.verifyTransactionsUnspent(thisNode.getBlockchain())) {
                Log.warning("Block contains spent transactions! Discarding!");
                return;
            }

            Log.info("Storing block...");
            thisNode.getBlockchain().putBlock(block);

            Log.info("Rebroadcasting...");
            thisNode.broadcastBlockToPeers(block);
        }
    }
}
