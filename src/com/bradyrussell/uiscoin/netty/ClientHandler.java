package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Hash;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockBuilder;
import com.bradyrussell.uiscoin.node.PeerPacketBuilder;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionBuilder;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private ChannelHandlerContext ctx;
    final BlockingQueue<BigInteger> answer = new LinkedBlockingQueue<>();

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
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        super.channelActive(ctx);
        System.out.println("ACTIVE");
        ByteBuf wrappedBuffer = Unpooled.wrappedBuffer(new PeerPacketBuilder(5).putGreeting(1).get());
        ChannelFuture channelFuture = ctx.writeAndFlush(wrappedBuffer);
        // wrappedBuffer.release();
        channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
            if(!channelFuture1.isSuccess())
                channelFuture1.cause().printStackTrace();
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Inactive");
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void sendNumbers() {
        // Do not send more than 4096 numbers.

        switch (ThreadLocalRandom.current().nextInt(4)){
            case 0 -> {
                Transaction transaction = new TransactionBuilder().setVersion(1).setLockTime(0).addOutput(new TransactionOutputBuilder().setAmount(ThreadLocalRandom.current().nextInt()).setPayToPublicKeyHash(Base64.getDecoder().decode("UISxUisdl8E31ksaCZvw3RKR9biwgXPi/m6lUTyN4E9K0n2vI+Xc5QFVtWpPz9+8fr2DwE5T40qLVbEj7QFsEyve3YteiPg=")).get()).get();
                ctx.writeAndFlush(transaction).addListener(numberSender);
            }
            case 1 -> {
                System.out.println("Mining...");
                BlockBuilder blockBuilder = new BlockBuilder().setVersion(1).setTimestamp(Instant.now().getEpochSecond()).setDifficultyTarget(2).setBlockHeight(0)
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
            case 3 -> {
                ByteBuf b = Unpooled.buffer();
                b.writeByte(PeerPacketType.REQUEST.Header);
                b.writeBytes(Hash.getSHA512Bytes("oof"));

                ctx.writeAndFlush(b).addListener(numberSender);
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

    private final ChannelFutureListener numberSender = future -> {
        if (future.isSuccess()) {
            sendNumbers();
        } else {
            future.cause().printStackTrace();
            future.channel().close();
        }
    };

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        System.out.println("READ");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("USER EVENT");
        sendNumbers();
    }
}