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
package de.mariokurz.nettylib.network.protocol.codec.nettyextras.serialization;

import io.netty5.buffer.BufferInputStream;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

/**
 * A decoder which deserializes the received {@link Buffer}s into Java
 * objects.
 * <p>
 * Please note that the serialized form this decoder expects is not
 * compatible with the standard {@link ObjectOutputStream}.  Please use
 * {@link ObjectEncoder} or {@link ObjectEncoderOutputStream} to ensure the
 * interoperability with this decoder.
 */
public class ObjectDecoder extends LengthFieldBasedFrameDecoder {

    private final ClassResolver classResolver;

    /**
     * Creates a new decoder whose maximum object size is {@code 1048576}
     * bytes.  If the size of the received object is greater than
     * {@code 1048576} bytes, a {@link StreamCorruptedException} will be
     * raised.
     *
     * @param classResolver the {@link ClassResolver} to use for this decoder
     */
    public ObjectDecoder(ClassResolver classResolver) {
        this(1048576, classResolver);
    }

    /**
     * Creates a new decoder with the specified maximum object size.
     *
     * @param maxObjectSize the maximum byte length of the serialized object.
     *                      if the length of the received object is greater
     *                      than this value, {@link StreamCorruptedException}
     *                      will be raised.
     * @param classResolver the {@link ClassResolver} which will load the class
     *                      of the serialized object
     */
    public ObjectDecoder(int maxObjectSize, ClassResolver classResolver) {
        super(maxObjectSize, 0, 4, 0, 4);
        this.classResolver = classResolver;
    }

    @Override
    protected Object decode0(ChannelHandlerContext ctx, Buffer in) throws Exception {
        Buffer frame = (Buffer) super.decode0(ctx, in);
        if (frame == null) {
            return null;
        }

        try (ObjectInputStream ois = new CompactObjectInputStream(new BufferInputStream(frame.send()), classResolver)) {
            return ois.readObject();
        }
    }

    public Object actionDecode(ChannelHandlerContext ctx, Buffer in) throws Exception {
        return this.decode0(ctx, in);
    }
}
