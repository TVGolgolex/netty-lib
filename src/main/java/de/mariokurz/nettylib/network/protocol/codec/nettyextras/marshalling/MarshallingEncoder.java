/*
 * Copyright 2021 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package de.mariokurz.nettylib.network.protocol.codec.nettyextras.marshalling;

import io.netty.contrib.handler.codec.marshalling.MarshallerProvider;
import io.netty.contrib.handler.codec.marshalling.MarshallingDecoder;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.MessageToByteEncoder;
import org.jboss.marshalling.Marshaller;

/**
 * {@link MessageToByteEncoder} implementation which uses JBoss Marshalling to marshal
 * an Object. Be aware that this encoder is not compatible with another client that just use
 * JBoss Marshalling as it includes the size of every {@link Object} that gets serialized in
 * front of the {@link Object} itself.
 * <p>
 * Use this with {@link MarshallingDecoder}
 * <p>
 * See <a href="https://www.jboss.org/jbossmarshalling">JBoss Marshalling website</a>
 * for more information
 */
public class MarshallingEncoder extends MessageToByteEncoder<Object> {

    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
    private final io.netty.contrib.handler.codec.marshalling.MarshallerProvider provider;

    /**
     * Creates a new encoder.
     *
     * @param provider the {@link io.netty.contrib.handler.codec.marshalling.MarshallerProvider} to use
     */
    public MarshallingEncoder(MarshallerProvider provider) {
        this.provider = provider;
    }

    @Override
    protected Buffer allocateBuffer(ChannelHandlerContext ctx, Object o) {
        return ctx.bufferAllocator().allocate(256);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, Buffer out) throws Exception {
        Marshaller marshaller = provider.getMarshaller(ctx);
        int lengthPos = out.writerOffset();
        out.writeBytes(LENGTH_PLACEHOLDER);
        ChannelBufferByteOutput output = new ChannelBufferByteOutput(out);
        marshaller.start(output);
        marshaller.writeObject(msg);
        marshaller.finish();
        marshaller.close();

        out.setInt(lengthPos, out.writerOffset() - lengthPos - 4);
    }

    @Override
    public boolean isSharable() {
        return true;
    }
}
