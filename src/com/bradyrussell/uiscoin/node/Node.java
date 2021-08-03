/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.node;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.blockchain.storage.Blockchain;
import com.bradyrussell.uiscoin.netty.NodeP2PClientInitializer;
import com.bradyrussell.uiscoin.netty.NodeP2PServerInitializer;
import com.bradyrussell.uiscoin.transaction.Transaction;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

public class Node {
    private static final Logger Log = Logger.getLogger(Node.class.getName());

    public ArrayList<InetAddress> peersEverSeen = new ArrayList<>();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private final EventLoopGroup peerGroup = new NioEventLoopGroup();
    private final Bootstrap peerBootstrap = new Bootstrap().group(peerGroup).channel(NioSocketChannel.class);

    public ChannelGroup nodeClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); // nodes connections to me
    public ChannelGroup peerClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); // my connection to other nodes

    public final long nodeId = ThreadLocalRandom.current().nextLong();

    public int HighestSeenBlockHeight;

    public Node() {
        this.HighestSeenBlockHeight = Blockchain.get().getBlockHeight(); // we have not seen another nodes blockheight yet
    }

    public void connectToPeer(InetAddress Address){
        if(getPeers().contains(Address)) {
            Log.info("Already connected to this peer!");
            return;
        }

        peerBootstrap.handler(new NodeP2PClientInitializer(this));

        // Make a new connection.
        ChannelFuture sync;
        sync = peerBootstrap.connect(Address, MagicNumbers.NodeP2PPort.Value).addListener((ChannelFutureListener) channelFuture -> {
            peerClients.add(channelFuture.channel());
            if(channelFuture.isSuccess()) {
                Log.info("Connection established with peer " + channelFuture.channel().remoteAddress().toString());
                if(!peersEverSeen.contains(Address)) peersEverSeen.add(Address);
            }
        })/*.sync()*/;
        // ChannelFuture closeFuture = sync.channel().closeFuture();
    }

    public void requestBlockHeightFromPeers(){
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(PeerPacketType.HEIGHTQUERY.Header);

        peerClients.writeAndFlush(buffer.copy());
        nodeClients.writeAndFlush(buffer);
    }

    @Deprecated
    public void requestBlockChainFromPeers(int BlockHeight){
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(PeerPacketType.SYNC.Header);
        buffer.writeBoolean(false);
        buffer.writeInt(BlockHeight);

        peerClients.writeAndFlush(buffer.copy());
        nodeClients.writeAndFlush(buffer);
    }

    public void requestBlockFromPeers(BlockRequest request){
        peerClients.writeAndFlush(request);
        nodeClients.writeAndFlush(request);
    }

    public void broadcastBlockToPeers(Block block){
        peerClients.writeAndFlush(block);
        nodeClients.writeAndFlush(block);
    }

    public void requestMemPoolFromPeers(){
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(PeerPacketType.MEMPOOL.Header);
        peerClients.writeAndFlush(buffer.copy());
        nodeClients.writeAndFlush(buffer);
    }

    public void broadcastBlockHeaderToPeers(BlockHeaderResponse blockHeaderResponse){
        peerClients.writeAndFlush(blockHeaderResponse);
        nodeClients.writeAndFlush(blockHeaderResponse);
    }

    public void broadcastTransactionToPeers(Transaction transaction){
        peerClients.writeAndFlush(transaction);
        nodeClients.writeAndFlush(transaction);
    }

    public void broadcastPeerToPeers(InetAddress address){
        peerClients.writeAndFlush(address);
        nodeClients.writeAndFlush(address);
    }

    public void start(){
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new NodeP2PServerInitializer(this));

        try {
            serverChannel = b.bind(MagicNumbers.NodeP2PPort.Value).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        try {
            if(peerClients != null) peerClients.close().sync();
            if(nodeClients != null) nodeClients.close().sync();
            Log.info("Closed peer connections.");

            if(serverChannel != null) {
                serverChannel.close().sync();
                serverChannel.eventLoop().shutdownGracefully().sync();
            }

            if(bossGroup != null) bossGroup.shutdownGracefully();
            if(workerGroup != null) workerGroup.shutdownGracefully();
            peerGroup.shutdownGracefully().sync();
            Log.info("Closed channel and shutdown worker event groups.");
            Log.info("Shutting down...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void retryPeers(){
        ArrayList<InetAddress> peersToRetry = new ArrayList<>(peersEverSeen);

        peersToRetry.removeAll(getPeers());

        for (InetAddress inetAddress : peersToRetry) {
            connectToPeer(inetAddress);
        }
    }

    public List<InetAddress> getPeers(){
        List<InetAddress> addresses = new ArrayList<>();

        if(nodeClients != null)
        nodeClients.forEach((channel -> {
            if(channel.isActive() && channel.isOpen()) {
                InetAddress address = ((InetSocketAddress) channel.remoteAddress()).getAddress();
                if (!addresses.contains(address)) {
                    addresses.add(address);
                }
            }
        }));

        if(peerClients != null)
        peerClients.forEach((channel -> {
            if(channel.isActive() && channel.isOpen()) {
                InetAddress address = ((InetSocketAddress) channel.remoteAddress()).getAddress();
                if (!addresses.contains(address)) {
                    addresses.add(address);
                }
            }
        }));

        return addresses;
    }
}
