package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.node.Node;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Logger;

public class NodeP2PReceiveTransactionHandler extends SimpleChannelInboundHandler<Transaction> {
    private static final Logger Log = Logger.getLogger(NodeP2PReceiveTransactionHandler.class.getName());
    private final Node thisNode;

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
        Log.info("Handler Received transaction "+ BytesUtil.base64Encode(transaction.getHash()));

        if(BlockChain.get().getMempool().contains(transaction)){
            Log.info("Already have. Discarding...");
            return;
        }

        if(!transaction.verifyInputsUnspent()) {
            transaction.debugVerify();
            Log.info("Spent input! Discarding.");
            return;
        }

        if(!transaction.verify()) {
            transaction.debugVerify();
            Log.info("Invalid transaction! Discarding.");
            return;
        }

        for (TransactionInput input : transaction.Inputs) {
            if(BytesUtil.doTransactionsContainTXO(input.InputHash, input.IndexNumber, BlockChain.get().getMempool())) {
                Log.info("Transaction contains outputs that are already in another mempool transaction! Discarding...");
                return;
            }
        }

        System.out.println("Storing transaction in mempool...");
        BlockChain.get().addToMempool(transaction);

        System.out.println("Rebroadcasting...");
        thisNode.broadcastTransactionToPeers(transaction);
    }
}
