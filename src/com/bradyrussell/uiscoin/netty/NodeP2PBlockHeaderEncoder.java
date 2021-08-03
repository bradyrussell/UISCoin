/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.node.BlockHeaderResponse;
import com.bradyrussell.uiscoin.node.PeerPacketType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NodeP2PBlockHeaderEncoder extends MessageToByteEncoder<BlockHeaderResponse> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, BlockHeaderResponse blockHeaderResponse, ByteBuf byteBuf) throws Exception {
        //System.out.println("Encoding blockheader "+ Util.Base64Encode(blockHeaderResponse.BlockHash));
        byteBuf.writeByte(PeerPacketType.HEADER.Header);
        byteBuf.writeBytes(blockHeaderResponse.BlockHash);
        byteBuf.writeBytes(blockHeaderResponse.blockHeader.getBinaryData());
    }
}
