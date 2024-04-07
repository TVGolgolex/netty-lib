package de.mariokurz.nettylib.network.protocol.codec.dynamic;

/*
 * MIT License
 *
 * Copyright (c) 2024 02:08 Mario Pascal K.
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

import de.mariokurz.nettylib.Codec;
import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.network.protocol.Packet;
import de.mariokurz.nettylib.network.protocol.codec.PacketBuffer;
import de.mariokurz.nettylib.network.protocol.codec.nettyextras.serialization.ClassResolvers;
import de.mariokurz.nettylib.network.protocol.codec.nettyextras.serialization.ObjectDecoder;
import de.mariokurz.nettylib.network.protocol.codec.nettyextras.serialization.ObjectEncoder;
import de.mariokurz.nettylib.network.protocol.codec.osgan.OsganMessageCodec;
import de.mariokurz.nettylib.network.protocol.codec.selfbuild.SelfBuild;
import de.mariokurz.nettylib.network.protocol.codec.selfbuild.SelfBuildMessageCodec;
import de.mariokurz.nettylib.network.protocol.register.PacketRegistry;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageCodec;

import java.io.Serializable;
import java.util.logging.Level;

public class DynamicMessageCodec extends ByteToMessageCodec<Object> {

    private final Codec codec;
    private final SelfBuildMessageCodec selfBuildMessageCodec;
    private final OsganMessageCodec osganMessageCodec;
    private final ObjectDecoder objectDecoder;
    private final ObjectEncoder objectEncoder;

    public DynamicMessageCodec(Codec codec, PacketRegistry packetRegistry) {
        this.codec = codec;
        this.selfBuildMessageCodec = new SelfBuildMessageCodec(packetRegistry);
        this.osganMessageCodec = new OsganMessageCodec();
        this.objectDecoder = new ObjectDecoder(ClassResolvers.softCachingResolver(this.getClass().getClassLoader()));
        this.objectEncoder = new ObjectEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object o, Buffer out) throws Exception {
        try {
            if (o instanceof Packet msg) {
                PacketBuffer buffer = new PacketBuffer(out);
                if (msg instanceof SelfBuild) {
                    buffer.writeInt(1);
                    selfBuildMessageCodec.encode(ctx, msg, buffer);
                    NettyLib.debug(Level.INFO, this.getClass(), "Using Encoder: " + selfBuildMessageCodec.getClass().getName());
                } else {
                    switch (codec) {
                        case DYNAMIC_SELF_NETTY -> {
                            buffer.writeInt(2);
                            objectEncoder.actionEncode(ctx, msg, buffer.buffer());
                            NettyLib.debug(Level.INFO, this.getClass(), "Using Encoder: " + objectEncoder.getClass().getName());
                        }
                        case DYNAMIC_SELF_OSGAN -> {
                            buffer.writeInt(3);
                            osganMessageCodec.encode(ctx, msg, buffer);
                            NettyLib.debug(Level.INFO, this.getClass(), "Using Encoder: " + osganMessageCodec.getClass().getName());
                        }
                    }
                }
            } else {
                 if (o instanceof Serializable serializable) {
                     out.writeInt(2);
                     objectEncoder.actionEncode(ctx, serializable, out);
                     NettyLib.debug(Level.INFO, this.getClass(), "Using Encoder: " + objectEncoder.getClass().getName());
                 } else {
                     out.writeInt(-1);
                     NettyLib.debug(Level.INFO, this.getClass(), "No decoder for: " + o.getClass().getName());
                 }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, Buffer buffer) throws Exception {
        var encoder = buffer.readInt();

        switch (encoder) {
            case 1 -> {
                selfBuildMessageCodec.decode(
                        channelHandlerContext,
                        new PacketBuffer(buffer)
                );
                NettyLib.debug(Level.INFO, this.getClass(), "Using Decode: " + selfBuildMessageCodec.getClass().getName());
            }
            case 2 -> {
                var o = objectDecoder.actionDecode(
                        channelHandlerContext,
                        buffer
                );
                channelHandlerContext.fireChannelRead(o);
                NettyLib.debug(Level.INFO, this.getClass(), "Using Decode: " + objectDecoder.getClass().getName());
            }
            case 3 -> {
                osganMessageCodec.decode(
                        channelHandlerContext,
                        new PacketBuffer(buffer)
                );
                NettyLib.debug(Level.INFO, this.getClass(), "Using Decode: " + osganMessageCodec.getClass().getName());
            }
        }

        buffer.resetOffsets();
    }
}
