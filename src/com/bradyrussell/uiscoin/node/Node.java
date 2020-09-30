package com.bradyrussell.uiscoin.node;

import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.netty.NodeP2PClientInitializer;
import com.bradyrussell.uiscoin.netty.NodeP2PServerInitializer;
import com.bradyrussell.uiscoin.transaction.Transaction;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

public class Node {
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    Channel serverChannel;

    EventLoopGroup peerGroup = new NioEventLoopGroup();
    Bootstrap peerBootstrap = new Bootstrap();

    public ChannelGroup nodeClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); // nodes connections to me
    public ChannelGroup peerClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); // my connection to other nodes

    int Version;

    public int HighestSeenBlockHeight;

    public Node(int Version) {
        this.Version = Version;
        this.HighestSeenBlockHeight = BlockChain.get().BlockHeight;
    }

    public void ConnectToPeer(InetAddress Address){
        if(getPeers().contains(Address)) {
            System.out.println("Already connected to this peer!");
            return;
        }

        peerBootstrap.group(peerGroup)
                .channel(NioSocketChannel.class)
                .handler(new NodeP2PClientInitializer(this));

        // Make a new connection.
        ChannelFuture sync;
        try {
            sync = peerBootstrap.connect(Address, MagicNumbers.NodeP2PPort.Value).sync();
            peerClients.add(sync.channel());

           // ChannelFuture closeFuture = sync.channel().closeFuture();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void RequestBlockHeightFromPeers(){
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(PeerPacketType.HEIGHTQUERY.Header);

        peerClients.writeAndFlush(buffer.copy());
        nodeClients.writeAndFlush(buffer);
    }

    @Deprecated
    public void RequestBlockChainFromPeers(int BlockHeight){
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(PeerPacketType.SYNC.Header);
        buffer.writeBoolean(false);
        buffer.writeInt(BlockHeight);

        peerClients.writeAndFlush(buffer.copy());
        nodeClients.writeAndFlush(buffer);
    }

    public void RequestBlockFromPeers(BlockRequest request){
        peerClients.writeAndFlush(request);
        nodeClients.writeAndFlush(request);
    }

    public void BroadcastBlockToPeers(Block block){
        peerClients.writeAndFlush(block);
        nodeClients.writeAndFlush(block);
    }

    public void BroadcastBlockHeaderToPeers(BlockHeaderResponse blockHeaderResponse){
        peerClients.writeAndFlush(blockHeaderResponse);
        nodeClients.writeAndFlush(blockHeaderResponse);
    }

    public void BroadcastTransactionToPeers(Transaction transaction){
        peerClients.writeAndFlush(transaction);
        nodeClients.writeAndFlush(transaction);
    }

    public void BroadcastPeerToPeers(InetAddress address){
        peerClients.writeAndFlush(address);
        nodeClients.writeAndFlush(address);
    }

    public void Start(){
        //startup the node server
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
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

    public void Stop(){
        try {
            if(bossGroup != null) bossGroup.shutdownGracefully().sync();
            if(workerGroup != null) workerGroup.shutdownGracefully().sync();
            if(peerGroup != null) peerGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<InetAddress> getPeers(){
        List<InetAddress> addresses = new ArrayList<>();

        nodeClients.forEach((channel -> {
            InetAddress address = ((InetSocketAddress) channel.remoteAddress()).getAddress();
            if(!addresses.contains(address)) {
                addresses.add(address);
            }
        }));
        peerClients.forEach((channel -> {
            InetAddress address = ((InetSocketAddress) channel.remoteAddress()).getAddress();
            if(!addresses.contains(address)) {
                addresses.add(address);
            }
        }));

        return addresses;
    }
}
