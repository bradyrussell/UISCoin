/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.blockchain.storage.Blockchain;
import com.bradyrussell.uiscoin.node.UISCoinNode;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NodeP2PReceiveTransactionHandler extends SimpleChannelInboundHandler<Transaction> {
    private static final Logger Log = Logger.getLogger(NodeP2PReceiveTransactionHandler.class.getName());
    private final UISCoinNode thisNode;

    public NodeP2PReceiveTransactionHandler(UISCoinNode thisNode) {
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

        if (Blockchain.get().hasMempoolTransaction(transaction.getHash())) {
            Log.info("Already have. Discarding...");
            return;
        }

        if(!transaction.verifyInputsUnspent()) {
            transaction.debugVerify();
            Log.warning("Spent input! Discarding.");
            return;
        }

        if(!transaction.verify()) {
            transaction.debugVerify();
            Log.warning("Invalid transaction! Discarding.");
            return;
        }

        for (TransactionInput input : transaction.Inputs) {
            if(BytesUtil.doTransactionsContainTXO(input.InputHash, input.IndexNumber, new ArrayList<>(Blockchain.get().getMempoolTransactions()))) {
                Log.warning("Transaction contains outputs that are already in another mempool transaction! Discarding..."); // todo this might not be necessary as long as utxos are checked to be unique on adding to candidate block
                return;
            }
        }

        System.out.println("Storing transaction in mempool...");
        Blockchain.get().putMempoolTransaction(transaction);

        System.out.println("Rebroadcasting...");
        thisNode.broadcastTransactionToPeers(transaction);
    }
}
