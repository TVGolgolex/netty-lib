package de.mariokurz.nettylib.network.client;

/*
 * MIT License
 *
 * Copyright (c) 2024 23:42 Mario Pascal K.
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
import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelAuthenticatedPacket;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelAuthorizePacket;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelInactivePacket;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelInitPacket;
import de.mariokurz.nettylib.network.protocol.routing.RoutingResultPacket;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.logging.Level;

@AllArgsConstructor
public class NetworkClientHandler extends SimpleChannelInboundHandler<Object> {

    private final NetworkClient networkClient;

    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        NettyLib.debug(Level.INFO, this.getClass(), "MessageReceived: " + o.getClass().getSimpleName());

        if (o instanceof NetworkChannelAuthenticatedPacket packet) {
            networkClient.clientChannelTransmitter.createNetworkChannel(packet.connectedChannel(), channelHandlerContext);
        }

        if (o instanceof NetworkChannelInactivePacket) {
            networkClient.clientChannelTransmitter.removeNetworkChannel(channelHandlerContext);
        }

        if (o instanceof NetworkChannelInitPacket packet) {
            for (var channelIdentity : packet.connectedChannel()) {
                if (channelIdentity != null) {
                    networkClient.clientChannelTransmitter.createNetworkChannel(channelIdentity, channelHandlerContext);
                }
            }
        }

        if (o instanceof RoutingResultPacket routingResultPacket) {
            networkClient.routingPacketManager.dispatch(routingResultPacket);
            return;
        }

        networkClient.clientChannelTransmitter.dispatchPacketObject(o, channelHandlerContext);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        networkClient.clientChannelTransmitter.createNetworkChannel(networkClient.channelIdentity, ctx);
        networkClient.thisNetworkChannel().sendPacket(new NetworkChannelAuthorizePacket(this.networkClient.channelIdentity));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if ((!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable())) {
            ctx.channel().close();
            switch (networkClient.inactiveAction()) {
                case SHUTDOWN -> {
                    networkClient.close();
                    System.exit(0);
                }
                case RETRY -> {

                }
            }
        }
    }

    @Override
    public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof IOException)) {
            NettyLib.debug(Level.SEVERE, this.getClass(), "Channel: " + ctx.channel().remoteAddress() + " exception caught: " + cause.fillInStackTrace().getMessage());
        }
    }
}
