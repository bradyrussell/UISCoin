package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.node.*;
import com.bradyrussell.uiscoin.transaction.Transaction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

public class NodeP2PMessageDecoder extends ReplayingDecoder<Void>{

    Node node;

    public NodeP2PMessageDecoder(Node node) {
        this.node = node;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        System.out.println("1 Decode");
        PeerPacketType packetType = PeerPacketType.getByHeader(byteBuf.readByte());
        System.out.println("2 Decoding "+packetType+" packet.");

        if (packetType != null) {
            switch (packetType){
                case DISCONNECT -> {
                    System.out.println("3 Received disconnect!");
                    channelHandlerContext.writeAndFlush(new PeerPacketBuilder(2).putDisconnect().get());
                    channelHandlerContext.disconnect().addListener((ChannelFutureListener) channelFuture -> System.out.println("Disconnected"));
                    list.add(true);
                }
                case GREETING -> {
                    System.out.println("3 Received greeting!");
                    int Version = byteBuf.readInt();
                    if(Version != MagicBytes.ProtocolVersion.Value) {
                        System.out.println("Protocol version mismatch, disconnecting!");
                        channelHandlerContext.disconnect();
                        list.add(true);
                        return;
                    }

                    System.out.println("4 Broadcasting new peer");
                    node.BroadcastPeerToPeers(((InetSocketAddress)channelHandlerContext.channel().remoteAddress()).getAddress());

                    ByteBuf wrappedBuffer = Unpooled.wrappedBuffer(new PeerPacketBuilder(5).putHandshake(1).get());
                    ChannelFuture channelFuture = channelHandlerContext.writeAndFlush(wrappedBuffer);
                    //wrappedBuffer.release();
                    channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
                        if(!channelFuture1.isSuccess())
                            channelFuture1.cause().printStackTrace();
                    });

                    list.add(true);
                }
                case HANDSHAKE -> {
                    System.out.println("3 Received handshake!");
                    int Version = byteBuf.readInt();
                    if(Version != MagicBytes.ProtocolVersion.Value) {
                        System.out.println("Protocol version mismatch, disconnecting!");
                        channelHandlerContext.disconnect();
                        list.add(true);
                        return;
                    }

                    channelHandlerContext.fireUserEventTriggered(true);

                    list.add(true);
                }
                case PING -> {
                    System.out.println("3 Received ping!");
                    list.add(true);
                }
                case PEER -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    System.out.println("3 Received peer "+Util.Base64Encode(Bytes));

                    InetAddress address = InetAddress.getByAddress(Bytes);

                    list.add(address);
                }
                case TRANSACTION -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    Transaction transaction = new Transaction();
                    transaction.setBinaryData(Bytes);

                    System.out.println("3 Received transaction "+Util.Base64Encode(transaction.getHash()));
                    list.add(transaction);
                }
                case BLOCK -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    Block block = new Block();
                    block.setBinaryData(Bytes);

                    System.out.println("3 Received block "+Util.Base64Encode(block.getHash()));
                    list.add(block);
                }
                case HEADER -> {
                    //this assumes headers are fixed size
                    BlockHeader blockHeader = new BlockHeader();

                    byte[] Hash = new byte[64];
                    byte[] Bytes = new byte[blockHeader.getSize()];

                    byteBuf.readBytes(Hash);
                    byteBuf.readBytes(Bytes);

                    blockHeader.setBinaryData(Bytes);

                    System.out.println("3 Received block header "+Util.Base64Encode(Hash));
                    list.add(new BlockHeaderResponse(Hash, blockHeader));
                }
                case HEIGHT -> {
                    int BlockHeight = byteBuf.readInt();

                    System.out.println("3 Received block height "+BlockHeight);

                    if(BlockHeight > node.HighestSeenBlockHeight) {
                        System.out.println("4 This is a longer chain! Syncing...");
                        node.HighestSeenBlockHeight = BlockHeight;

                        ByteBuf buffer = Unpooled.buffer();
                        buffer.writeByte(PeerPacketType.SYNC.Header);
                        buffer.writeBoolean(false);
                        buffer.writeInt(BlockChain.get().BlockHeight+1); // start from next block after ours
                        channelHandlerContext.writeAndFlush(buffer);
                    }

                }
                case REQUEST -> {
                    boolean bOnlyHeader = byteBuf.readBoolean();
                    byte[] Bytes = new byte[64];
                    byteBuf.readBytes(Bytes);

                    System.out.println("3 Received block request "+Util.Base64Encode(Bytes));
                    list.add(new BlockRequest(Bytes, bOnlyHeader));
                }
                case SYNC -> {
                    boolean bOnlyHeader = byteBuf.readBoolean();
                    int BlockHeight = byteBuf.readInt();

                    System.out.println("3 Received sync request "+BlockHeight);

                    //if(BlockHeight > BlockChain.get().BlockHeight) BlockHeight = BlockChain.get().BlockHeight;

                    List<Block> blockChainFromHeight = BlockChain.get().getBlockChainFromHeight(BlockHeight);
                    for (int i = 0; i < blockChainFromHeight.size(); i++) {
                        Block block = blockChainFromHeight.get(i);
                        System.out.println("4 Sending blockchain "+i+"/"+blockChainFromHeight.size());
                        ChannelFuture channelFuture = channelHandlerContext.write(bOnlyHeader ? block.Header : block);

                        channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
                            if(!channelFuture1.isSuccess())
                                channelFuture1.cause().printStackTrace();
                        });
                    }
                    channelHandlerContext.flush();
                    System.out.println("5 Flushing");
                    list.add(true);
                }
                case MEMPOOL -> {
                    System.out.println("3 Received mempool query");
                    for (Transaction transaction : BlockChain.get().getMempool()) {
                        channelHandlerContext.write(transaction);
                    }
                    channelHandlerContext.flush();
                    System.out.println("4 Sent mempool");
                    list.add(true);
                }
                case HEIGHTQUERY -> {
                    System.out.println("3 Received blockheight query");
                    ByteBuf buf = Unpooled.buffer();
                    buf.writeByte(PeerPacketType.HEIGHT.Header);
                    buf.writeInt(BlockChain.get().BlockHeight);

                    System.out.println("4 Sending blockheight "+BlockChain.get().BlockHeight);

                    ChannelFuture channelFuture = channelHandlerContext.writeAndFlush(buf);

                    channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
                        if(!channelFuture1.isSuccess())
                            channelFuture1.cause().printStackTrace();
                    });
                    list.add(true);
                }
                default -> {
                    System.out.println("3 Received invalid request, disconnecting.");
                    channelHandlerContext.disconnect();
                    list.add(true);
                }
            }
        }
    }
}
