package de.mario.nettylib.network.client;

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

import de.mario.nettylib.NettyLib;
import de.mario.nettylib.network.channel.NetworkChannel;
import de.mario.nettylib.network.protocol.authorize.AuthorizePacket;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;

import java.util.logging.Level;

@AllArgsConstructor
public class NetworkClientHandler extends SimpleChannelInboundHandler<Object> {

    private final NetworkChannel networkChannel;

    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        NettyLib.debug(Level.INFO, this.getClass(), "MessageReceived: " + o.getClass().getSimpleName());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        networkChannel.channel(ctx.channel());
        networkChannel.sendPacket(new AuthorizePacket(networkChannel.channelIdentity()));
    }
}
