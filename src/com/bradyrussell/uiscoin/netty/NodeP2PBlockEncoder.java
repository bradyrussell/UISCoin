/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.node.PeerPacketType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NodeP2PBlockEncoder extends MessageToByteEncoder<Block> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Block block, ByteBuf byteBuf) throws Exception {
        //System.out.println("Encoding block "+ Util.Base64Encode(block.Header.getHash()));
        byteBuf.writeByte(PeerPacketType.BLOCK.Header);
        byteBuf.writeInt(block.getSize());
        byteBuf.writeBytes(block.getBinaryData());
    }
}
