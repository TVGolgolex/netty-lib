package de.mariokurz.nettylib.network.protocol.codec.selfbuild;

/*
 * MIT License
 *
 * Copyright (c) 2024 01:19 Mario Pascal K.
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

import de.golgolex.quala.Quala;
import de.mariokurz.nettylib.network.protocol.codec.AbstractMessageCodec;
import de.mariokurz.nettylib.network.protocol.codec.PacketBuffer;
import de.mariokurz.nettylib.network.protocol.Packet;
import de.mariokurz.nettylib.network.protocol.register.PacketRegistry;
import io.netty5.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SelfBuildMessageCodec extends AbstractMessageCodec {

    private final PacketRegistry packetRegistry;

    @Override
    public void encode(ChannelHandlerContext ctx, Packet msg, PacketBuffer buffer) throws Exception {
        if (!(msg instanceof SelfBuild selfBuild)) {
            System.out.println("Objective: " + msg.getClass().getSimpleName() + " is not a instance of SelfBuild");
            return;
        }

        try {
            buffer.writeInt(selfBuild.registerId())
                    .writeUniqueId(msg.queryId() == null ? Quala.SYSTEM_UUID : msg.queryId());
            selfBuild.writeBuffer(buffer);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void decode(ChannelHandlerContext ctx, PacketBuffer buffer) throws Exception {
        try {
            var registerId = buffer.readInt();
            var packet = packetRegistry.construct(registerId);

            if (packet == null) {
                buffer.resetBuffer();
                return;
            }

            var queryId = buffer.readUniqueId();
            if (!queryId.equals(Quala.SYSTEM_UUID)) {
                packet.queryId(queryId);
            }

            if (packet instanceof SelfBuild selfBuild) {
                selfBuild.readBuffer(buffer);
            }

            buffer.resetBuffer();
            ctx.fireChannelRead(packet);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
