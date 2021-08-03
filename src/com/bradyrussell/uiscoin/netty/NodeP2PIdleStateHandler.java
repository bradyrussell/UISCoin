/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.node.PeerPacketType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class NodeP2PIdleStateHandler extends ChannelDuplexHandler {
      @Override
     public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
         if (evt instanceof IdleStateEvent) {
             IdleStateEvent e = (IdleStateEvent) evt;
             if (e.state() == IdleState.READER_IDLE) {
                 ctx.close();
             } else if (e.state() == IdleState.WRITER_IDLE) {
                 ByteBuf buffer = Unpooled.buffer();
                 buffer.writeByte(PeerPacketType.PING.Header);
                 ctx.writeAndFlush(buffer);
             }
         }
     }
 }