package com.bradyrussell.uiscoin.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

public class NodeP2PClientInitializer extends ChannelInitializer<SocketChannel> {

  //  private final SslContext sslCtx;

    public NodeP2PClientInitializer(/*SslContext sslCtx*/) {
       /* this.sslCtx = sslCtx;*/
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

/*        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc(), FactorialClient.HOST, FactorialClient.PORT));
        }*/

        // Enable stream compression (you can remove these two if unnecessary)
        pipeline.addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        pipeline.addLast(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));

        // Add the number codec first,
        pipeline.addLast(new NodeP2PMessageDecoder());

        pipeline.addLast(new NodeP2PTransactionEncoder());
        pipeline.addLast(new NodeP2PBlockEncoder());
        pipeline.addLast(new NodeP2PPeerEncoder());
        pipeline.addLast(new NodeP2PBlockRequestEncoder());

        pipeline.addLast(new ReceiveBlockRequestHandler());
        pipeline.addLast(new ReceiveBlockHandler());
     //   pipeline.addLast(new NodeP2PPacketEncoder());
      //  pipeline.addLast(new NodeP2PSendGreetingHandler());

        // and then business logic.
        pipeline.addLast(new ClientHandler());

    }
}
