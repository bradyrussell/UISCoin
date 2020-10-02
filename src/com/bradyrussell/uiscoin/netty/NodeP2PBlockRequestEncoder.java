package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.node.BlockRequest;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NodeP2PBlockRequestEncoder extends MessageToByteEncoder<BlockRequest> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, BlockRequest blockRequest, ByteBuf byteBuf) throws Exception {
        System.out.println("Encoding block request");
        byteBuf.writeByte(PeerPacketType.REQUEST.Header);
        byteBuf.writeBoolean(blockRequest.bOnlyHeader);
        byteBuf.writeBytes(blockRequest.BlockHash);
    }
}
