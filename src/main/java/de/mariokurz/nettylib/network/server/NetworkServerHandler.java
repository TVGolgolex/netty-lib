package de.mariokurz.nettylib.network.server;

/*
 * MIT License
 *
 * Copyright (c) 2024 23:13 Mario Pascal K.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelAuthorizePacket;
import de.mariokurz.nettylib.network.protocol.routing.RoutingPacket;
import de.mariokurz.nettylib.network.protocol.routing.RoutingResult;
import de.mariokurz.nettylib.network.protocol.routing.RoutingResultPacket;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.logging.Level;

@AllArgsConstructor
public class NetworkServerHandler extends SimpleChannelInboundHandler<Object> {

    protected final ServerChannelTransmitter serverChannelTransmitter;

    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        NettyLib.debug(Level.INFO, this.getClass(), "MessageReceived: " + o.getClass().getSimpleName());

        if (o instanceof NetworkChannelAuthorizePacket networkChannelAuthorizePacket) {
            serverChannelTransmitter.authorize(channelHandlerContext, networkChannelAuthorizePacket);
            return;
        }

        if (o instanceof RoutingPacket routingPacket) {
            var networkChannel = serverChannelTransmitter.getNetworkChannel(routingPacket.receiverIdentity());
            RoutingResultPacket routingResultPacket = new RoutingResultPacket();
            routingResultPacket.queryId(routingPacket.queryId());
            if (networkChannel == null) {
                NettyLib.log(Level.SEVERE, "No NetworkChannel for {0} found.", routingPacket.receiverIdentity());
                routingResultPacket.result(RoutingResult.FAILED_NO_CHANNEL);
            } else {
                NettyLib.log(Level.INFO, "Send Packet " + routingPacket.getClass().getName() + " to NetworkChannel {0}", routingPacket.receiverIdentity());
                networkChannel.sendPacket(routingPacket);
                routingResultPacket.result(RoutingResult.SUCCESS);
            }
            serverChannelTransmitter.getNetworkChannel(channelHandlerContext.channel()).sendPacket(routingResultPacket);
        }

        serverChannelTransmitter.dispatchPacketObject(o, channelHandlerContext);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyLib.debug(Level.INFO, this.getClass(), "Channel active: " + ctx.channel().remoteAddress());
        serverChannelTransmitter.active(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if ((!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable())) {
            NettyLib.debug(Level.INFO, this.getClass(), "Channel inactive: " + ctx.channel().remoteAddress());
            serverChannelTransmitter.inactive(ctx);
        }
    }

    @Override
    public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof IOException)) {
            NettyLib.debug(Level.SEVERE, this.getClass(), "Channel: " + ctx.channel().remoteAddress() + " exception caught: " + cause.fillInStackTrace().getMessage());
        }
    }
}
