package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import com.bradyrussell.uiscoin.transaction.Transaction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NodeP2PTransactionEncoder extends MessageToByteEncoder<Transaction> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Transaction transaction, ByteBuf byteBuf) throws Exception {
        System.out.println("Encoding transaction "+ Util.Base64Encode(transaction.getHash()));
        byteBuf.writeByte(PeerPacketType.TRANSACTION.Header);
        byteBuf.writeInt(transaction.getSize());
        byteBuf.writeBytes(transaction.getBinaryData());
    }
}
