package de.mario.nettylib.network.client;

/*
 * MIT License
 *
 * Copyright (c) 2024 23:25 Mario Pascal K.
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

import de.mario.nettylib.network.server.NettyServer;
import de.mario.nettylib.network.server.NetworkServerHandler;
import io.netty.contrib.handler.codec.serialization.ClassResolvers;
import io.netty.contrib.handler.codec.serialization.ObjectDecoder;
import io.netty.contrib.handler.codec.serialization.ObjectEncoder;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelInitializer;
import io.netty5.handler.codec.DelimiterBasedFrameDecoder;
import io.netty5.handler.codec.Delimiters;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NetworkClientChannelInitializer extends ChannelInitializer<Channel> {

    protected final NettyClient nettyClient;
    protected final String host;
    protected final int port;

    @Override
    protected void initChannel(Channel channel) throws Exception {

        if (nettyClient.sslCtx != null) {
            channel.pipeline().addLast(nettyClient.sslCtx.newHandler(channel.bufferAllocator(), host, port));
        }

        channel.pipeline()
//                .addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()))
                .addLast(new ObjectDecoder(ClassResolvers.softCachingResolver(NettyClient.class.getClassLoader())))
                .addLast(new ObjectEncoder())
                .addLast(new NetworkClientHandler(nettyClient.networkChannel));

    }
}
