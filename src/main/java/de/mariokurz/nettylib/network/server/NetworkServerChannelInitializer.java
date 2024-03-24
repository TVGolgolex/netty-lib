package de.mariokurz.nettylib.network.server;

/*
 * MIT License
 *
 * Copyright (c) 2024 23:09 Mario Pascal K.
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
import io.netty.contrib.handler.codec.serialization.ClassResolvers;
import io.netty.contrib.handler.codec.serialization.ObjectDecoder;
import io.netty.contrib.handler.codec.serialization.ObjectEncoder;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelInitializer;
import lombok.AllArgsConstructor;

import java.util.logging.Level;

@AllArgsConstructor
public class NetworkServerChannelInitializer extends ChannelInitializer<Channel> {

    protected final NetworkServer networkServer;
    protected final String host;
    protected final int port;

    @Override
    protected void initChannel(Channel channel) throws Exception {
        NettyLib.log(Level.INFO, "Channel initialized: {0}", channel.remoteAddress().toString());

        if (networkServer.sslCtx != null) {
            channel.pipeline().addLast(networkServer.sslCtx.newHandler(channel.bufferAllocator()));
        }

        channel.pipeline()
//                .addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()))
                .addLast(new ObjectDecoder(ClassResolvers.softCachingResolver(NetworkServer.class.getClassLoader())))
                .addLast(new ObjectEncoder())
                .addLast(new NetworkServerHandler(networkServer.serverChannelTransmitter));

    }

}