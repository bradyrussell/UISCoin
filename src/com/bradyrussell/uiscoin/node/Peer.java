package com.bradyrussell.uiscoin.node;

import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.netty.ClientHandler;
import com.bradyrussell.uiscoin.netty.NodeP2PClientInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

public class Peer {
    public InetAddress Address;
    boolean isConnected;

    public void Connect(){
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new NodeP2PClientInitializer());

            // Make a new connection.
            ChannelFuture sync = null;
            try {
                sync = b.connect(Address, MagicNumbers.NodeP2PPort.Value).sync();

                ChannelFuture closeFuture = sync.channel().closeFuture();

                // Get the handler instance to retrieve the answer.
                ClientHandler handler =
                        (ClientHandler) sync.channel().pipeline().last();

                while (!closeFuture.isDone()) {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        handler.SendBlockRequest(new BlockRequest(Util.Base64Decode("UIRTCXb5LIKUQMJuU5dM18OoNdlHztGJMRv0KUM3FbzhxHk9_rJyphibpcTT40NfjmE4GN5AZrGDQo1X2c8mJg==")));
                    }
                    Thread.sleep(1000);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            // Print out the answer.
            //   System.err.format("Factorial of %,d is: %,d", COUNT, handler.getFactorial());

        } finally {
            group.shutdownGracefully();
        }
    }

    void Disconnect(){

    }
}
