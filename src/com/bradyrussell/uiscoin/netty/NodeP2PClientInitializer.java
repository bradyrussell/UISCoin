package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.node.Node;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.timeout.IdleStateHandler;

public class NodeP2PClientInitializer extends ChannelInitializer<SocketChannel> {
Node node;

    public NodeP2PClientInitializer(Node node) {
        this.node = node;
    }
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


        pipeline.addLast(new IdleStateHandler(60, 30, 0));
        pipeline.addLast(new NodeP2PIdleStateHandler());
        // Add the number codec first,

        pipeline.addLast(new NodeP2PTransactionEncoder()); // need encoders before decoder so it can send blocks back
        pipeline.addLast(new NodeP2PBlockEncoder());
        pipeline.addLast(new NodeP2PBlockHeaderEncoder());
        pipeline.addLast(new NodeP2PPeerEncoder());
        pipeline.addLast(new NodeP2PBlockRequestEncoder());

        pipeline.addLast(new NodeP2PMessageDecoder(node));
        // and then business logic.
        // Please note we create a handler for every new channel
        // because it has stateful properties.
        pipeline.addLast(new NodeP2PReceiveBlockHandler(node));
        pipeline.addLast(new NodeP2PReceiveBlockHeaderResponseHandler(node));
        pipeline.addLast(new NodeP2PReceiveTransactionHandler(node));
        pipeline.addLast(new NodeP2PReceivePeerHandler(node));
        pipeline.addLast(new NodeP2PReceiveBlockRequestHandler());

        // and then business logic.
        pipeline.addLast(new NodeP2PClientHandler());

    }
}
//