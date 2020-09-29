package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Conversions;
import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.address.UISCoinAddress;
import com.bradyrussell.uiscoin.address.UISCoinKeypair;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockBuilder;
import com.bradyrussell.uiscoin.node.Peer;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionBuilder;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private ChannelHandlerContext ctx;
    private int receivedMessages;
    private int next = 1;
    final BlockingQueue<BigInteger> answer = new LinkedBlockingQueue<BigInteger>();

    public BigInteger getFactorial() {
        boolean interrupted = false;
        try {
            for (;;) {
                try {
                    return answer.take();
                } catch (InterruptedException ignore) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        //sendNumbers();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Inactive");
        super.channelInactive(ctx);
    }
/*
    @Override
    public void channelRead0(ChannelHandlerContext ctx, final BigInteger msg) {
        receivedMessages ++;
        if (receivedMessages == NodeP2PClient.COUNT) {
           // Offer the answer after closing the connection.
            ctx.channel().close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    boolean offered = answer.offer(msg);
                    assert offered;
                }
            }); }
    }*/

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void sendNumbers() {
        // Do not send more than 4096 numbers.

        switch (ThreadLocalRandom.current().nextInt(3)){
            case 0 -> {
                Transaction transaction = new TransactionBuilder().setVersion(1).setLockTime(0).addOutput(new TransactionOutputBuilder().setAmount(ThreadLocalRandom.current().nextInt()).setPayToPublicKeyHash(Base64.getDecoder().decode("UISxUisdl8E31ksaCZvw3RKR9biwgXPi/m6lUTyN4E9K0n2vI+Xc5QFVtWpPz9+8fr2DwE5T40qLVbEj7QFsEyve3YteiPg=")).get()).get();
                ctx.writeAndFlush(transaction).addListener(numberSender);
            }
            case 1 -> {
                System.out.println("Mining...");
                BlockBuilder blockBuilder = new BlockBuilder().setVersion(1).setTimestamp(Instant.now().getEpochSecond()).setDifficultyTarget(3).setBlockHeight(0)
                        .setHashPreviousBlock(Hash.getSHA512Bytes("Hello world from UISCoin."))
                        .addCoinbase(new TransactionBuilder().setVersion(1).setLockTime(0).addOutput(new TransactionOutputBuilder().setPayToPublicKeyHash(Base64.getDecoder().decode("UISxUisdl8E31ksaCZvw3RKR9biwgXPi/m6lUTyN4E9K0n2vI+Xc5QFVtWpPz9+8fr2DwE5T40qLVbEj7QFsEyve3YteiPg=")).setAmount(Block.CalculateBlockReward(0)).get()).get())
                        .CalculateMerkleRoot();

                while(!Hash.validateHash(blockBuilder.get().getHash(), blockBuilder.get().Header.DifficultyTarget)) {
                    blockBuilder.setNonce(ThreadLocalRandom.current().nextInt());
                }

                Block finishedBlock = blockBuilder.get();
                ctx.writeAndFlush(finishedBlock).addListener(numberSender);
            }
            case 2 -> {
                try {
                    ctx.writeAndFlush(InetAddress.getByName("127.0.0.1")).addListener(numberSender);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }


        /*ChannelFuture future = null;
        for (int i = 0; i < 4096 && next <= NodeP2PClient.COUNT; i++) {
            future = ctx.write(Integer.valueOf(next));
            next++;
        }
        if (next <= NodeP2PClient.COUNT) {
            assert future != null;
            future.addListener(numberSender);
        }
        ctx.flush();*/
    }

    private final ChannelFutureListener numberSender = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                sendNumbers();
            } else {
                future.cause().printStackTrace();
                future.channel().close();
            }
        }
    };

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        System.out.println("READ");
    }
}