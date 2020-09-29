package com.bradyrussell.uiscoin.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;


public class NodeP2PServerInitializer extends ChannelInitializer<SocketChannel> {


   // private final SslContext sslCtx;

   // public FactorialServerInitializer(SslContext sslCtx) {
       // this.sslCtx = sslCtx;
  //  }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

/*
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
*/
        // Enable stream compression (you can remove these two if unnecessary)
        pipeline.addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        pipeline.addLast(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));

        // Add the number codec first,
        pipeline.addLast(new NodeP2PMessageDecoder());
/*        pipeline.addLast(new Decoder());
        pipeline.addLast(new Encoder());*/
        pipeline.addLast(new NodeP2PTransactionEncoder());
        pipeline.addLast(new NodeP2PBlockEncoder());
        pipeline.addLast(new NodeP2PPeerEncoder());
       // pipeline.addLast(new NodeP2PPacketEncoder());

        // and then business logic.
        // Please note we create a handler for every new channel
        // because it has stateful properties.
        pipeline.addLast(new ReceiveBlockHandler());
        pipeline.addLast(new ReceiveTransactionHandler());
        pipeline.addLast(new ReceivePeerHandler());

        pipeline.addLast(new ServerHandler());
    }
}