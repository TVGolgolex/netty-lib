package de.mariokurz.nettylib.network.protocol.codec;

import de.mariokurz.nettylib.network.protocol.Packet;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageCodec;

public abstract class AbstractMessageCodec extends ByteToMessageCodec<Packet> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, Buffer out) throws Exception {
        this.encode(ctx, msg, new PacketBuffer(out));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Buffer buffer) throws Exception {
        this.decode(ctx, new PacketBuffer(buffer));
    }

    public abstract void encode(ChannelHandlerContext ctx, Packet msg, PacketBuffer buffer) throws Exception;

    public abstract void decode(ChannelHandlerContext ctx, PacketBuffer buffer) throws Exception;
}
