package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.node.Node;
import com.bradyrussell.uiscoin.transaction.Transaction;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NodeP2PReceiveTransactionHandler extends SimpleChannelInboundHandler<Transaction> {
    private Node thisNode;

    public NodeP2PReceiveTransactionHandler(Node thisNode) {
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
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Transaction transaction) throws Exception {
        System.out.println("Handler Received transaction "+ Util.Base64Encode(transaction.getHash()));

        transaction.DebugVerify();
        if(!transaction.Verify()) {
            System.out.println("Invalid transaction! Discarding.");
            return;
        }

        System.out.println("Storing transaction in mempool...");
        Util.putMempool(transaction);

        System.out.println("Rebroadcasting...");
        thisNode.BroadcastTransactionToPeers(transaction);
    }
}
