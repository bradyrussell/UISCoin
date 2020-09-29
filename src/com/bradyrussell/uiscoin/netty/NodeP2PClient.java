package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.blockchain.BlockChainStorageFile;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class NodeP2PClient {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8322"));
    static final int COUNT = Integer.parseInt(System.getProperty("count", "1000"));

    public static void main(String[] args) throws Exception {
/*        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }*/
        BlockChain.Initialize(BlockChainStorageFile.class);

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new NodeP2PClientInitializer());

            // Make a new connection.
            ChannelFuture sync = b.connect(HOST, PORT).sync();
            ChannelFuture closeFuture = sync.channel().closeFuture();


            // Get the handler instance to retrieve the answer.
            ClientHandler handler =
                    (ClientHandler) sync.channel().pipeline().last();

            while (!closeFuture.isDone()) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    handler.SendBlockRequest(Util.Base64Decode("UIRTCXb5LIKUQMJuU5dM18OoNdlHztGJMRv0KUM3FbzhxHk9_rJyphibpcTT40NfjmE4GN5AZrGDQo1X2c8mJg=="));
                }
                Thread.sleep(1000);
            }

            // Print out the answer.
            //   System.err.format("Factorial of %,d is: %,d", COUNT, handler.getFactorial());

        } finally {
            group.shutdownGracefully();
        }
    }
}
