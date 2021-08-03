/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;

import com.bradyrussell.uiscoin.BytesUtil;
import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.blockchain.storage.Blockchain;
import com.bradyrussell.uiscoin.node.*;
import com.bradyrussell.uiscoin.transaction.Transaction;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class NodeP2PMessageDecoder extends ReplayingDecoder<Void>{
    private static final Logger Log = Logger.getLogger(NodeP2PMessageDecoder.class.getName());

    Node node;

    public NodeP2PMessageDecoder(Node node) {
        this.node = node;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        Log.fine("1 Decode");
        PeerPacketType packetType = PeerPacketType.getByHeader(byteBuf.readByte());
        Log.fine("2 Decoding "+packetType+" packet.");

        if (packetType != null) {
            switch (packetType){
                case DISCONNECT -> {
                    Log.fine("3 Received disconnect!");
                    channelHandlerContext.writeAndFlush(Unpooled.wrappedBuffer(new byte[]{PeerPacketType.DISCONNECT.Header}));
                    channelHandlerContext.disconnect().addListener((ChannelFutureListener) channelFuture -> Log.info("Disconnected from peer "+channelFuture.channel().remoteAddress().toString()+" at their request."));
                    list.add(true);
                }
                case GREETING -> {
                    Log.fine("3 Received greeting!");
                    int Version = byteBuf.readInt();
                    long nodeId = byteBuf.readLong();
                    if(Version != MagicBytes.ProtocolVersion.Value) {
                        Log.info("Protocol version mismatch, disconnecting!");
                        channelHandlerContext.disconnect();
                        list.add(true);
                        return;
                    }

                    if(node.nodeId == nodeId) {
                        Log.info("Connection to self not allowed!");
                        channelHandlerContext.disconnect();
                        list.add(true);
                        return;
                    }

                    Log.fine("4 Broadcasting new peer");
                    node.broadcastPeerToPeers(((InetSocketAddress)channelHandlerContext.channel().remoteAddress()).getAddress());
/*                    for (InetAddress peer : node.getPeers()) {
                        if(!peer.equals(((InetSocketAddress)channelHandlerContext.channel().remoteAddress()).getAddress())) channelHandlerContext.writeAndFlush(peer);
                    }*/

                    ByteBuf wrappedBuffer = Unpooled.buffer();
                    wrappedBuffer.writeByte(PeerPacketType.HANDSHAKE.Header);
                    wrappedBuffer.writeInt(MagicBytes.ProtocolVersion.Value);
                    ChannelFuture channelFuture = channelHandlerContext.writeAndFlush(wrappedBuffer);
                    //wrappedBuffer.release();
                    channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
                        if(!channelFuture1.isSuccess())
                            channelFuture1.cause().printStackTrace();
                    });

                    list.add(true);
                }
                case HANDSHAKE -> {
                    Log.fine("3 Received handshake!");
                    int Version = byteBuf.readInt();
                    if(Version != MagicBytes.ProtocolVersion.Value) {
                        Log.info("Protocol version mismatch, disconnecting!");
                        channelHandlerContext.disconnect();
                        list.add(true);
                        return;
                    }

                    channelHandlerContext.fireUserEventTriggered(true);

                    list.add(true);
                }
                case PING -> {
                    Log.info("3 Received ping from "+((InetSocketAddress)channelHandlerContext.channel().remoteAddress()).getAddress().getHostAddress());
                    list.add(true);
                }
                case PEER -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    InetAddress address = InetAddress.getByAddress(Bytes);
                    Log.fine("3 Received peer "+ address.toString());

                    list.add(address);
                }
                case TRANSACTION -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    Transaction transaction = new Transaction();
                    transaction.setBinaryData(Bytes);

                    Log.fine("3 Received transaction "+ BytesUtil.base64Encode(transaction.getHash()));
                    list.add(transaction);
                }
                case BLOCK -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    Block block = new Block();
                    block.setBinaryData(Bytes);

                    Log.fine("3 Received block "+ BytesUtil.base64Encode(block.Header.getHash()));
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

                    Log.fine("3 Received block header "+ BytesUtil.base64Encode(Hash));
                    list.add(new BlockHeaderResponse(Hash, blockHeader));
                }
                case HEIGHT -> {
                    int BlockHeight = byteBuf.readInt();

                    Log.info("3 Received block height "+BlockHeight);

                    if(BlockHeight > node.HighestSeenBlockHeight) {
                        Log.info("4 This is a longer chain! Syncing...");
                        node.HighestSeenBlockHeight = BlockHeight;

                        ByteBuf buffer = Unpooled.buffer();
                        buffer.writeByte(PeerPacketType.SYNC.Header);
                        buffer.writeBoolean(false);
                        buffer.writeInt(Blockchain.get().getBlockHeight()+1); // start from next block after ours
                        channelHandlerContext.writeAndFlush(buffer);

                        Log.info("Requesting blockchain from height "+(Blockchain.get().getBlockHeight()+1));
                    }
                    list.add(true);
                }
                case REQUEST -> {
                    boolean bOnlyHeader = byteBuf.readBoolean();
                    byte[] Bytes = new byte[64];
                    byteBuf.readBytes(Bytes);

                    Log.fine("3 Received block request "+ BytesUtil.base64Encode(Bytes));
                    list.add(new BlockRequest(Bytes, bOnlyHeader));
                }
                case SYNC -> {
                    boolean bOnlyHeader = byteBuf.readBoolean();
                    int BlockHeight = byteBuf.readInt();

                    Log.info("3 Received sync request "+BlockHeight);

                    //if(BlockHeight > BlockChain.get().BlockHeight) BlockHeight = BlockChain.get().BlockHeight;

                    if(BlockHeight >= Blockchain.get().getBlockHeight()) {
                        Log.info("3 Cannot serve requested block height "+BlockHeight);
                        list.add(true);
                    }

                    List<Block> blockChainFromHeight = Blockchain.get().getBlockchainRange(BlockHeight, Blockchain.get().getBlockHeight());
                    for (int i = 0; i < blockChainFromHeight.size(); i++) {
                        Block block = blockChainFromHeight.get(i);
                        Log.info("4 Sending blockchain "+i+"/"+blockChainFromHeight.size());
                        ChannelFuture channelFuture = channelHandlerContext.write(bOnlyHeader ? block.Header : block);

                        channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
                            if(!channelFuture1.isSuccess())
                                channelFuture1.cause().printStackTrace();
                        });
                    }
                    channelHandlerContext.flush();
                    Log.fine("5 Flushing");
                    list.add(true);
                }
                case MEMPOOL -> {
                    Log.fine("3 Received mempool query");
                    for (Transaction transaction : Blockchain.get().getMempoolTransactions()) {
                        channelHandlerContext.write(transaction);
                    }
                    channelHandlerContext.flush();
                    Log.fine("4 Sent mempool");
                    list.add(true);
                }
                case HEIGHTQUERY -> {
                    Log.fine("3 Received blockheight query");
                    ByteBuf buf = Unpooled.buffer();
                    buf.writeByte(PeerPacketType.HEIGHT.Header);
                    buf.writeInt(Blockchain.get().getBlockHeight());

                    Log.fine("4 Sending blockheight "+Blockchain.get().getBlockHeight());

                    ChannelFuture channelFuture = channelHandlerContext.writeAndFlush(buf);

                    channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
                        if(!channelFuture1.isSuccess())
                            channelFuture1.cause().printStackTrace();
                    });
                    list.add(true);
                }
                default -> {
                    Log.info("3 Received invalid request, disconnecting.");
                    channelHandlerContext.disconnect();
                    list.add(true);
                }
            }
        }
    }
}
