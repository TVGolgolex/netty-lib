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
package de.mariokurz.nettylib.network.protocol.codec.nettyextras.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.ChannelPipeline;
import io.netty5.handler.codec.ByteToMessageDecoder;
import io.netty5.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty5.handler.codec.LengthFieldPrepender;
import io.netty5.handler.codec.MessageToMessageDecoder;

import static java.util.Objects.requireNonNull;

/**
 * Decodes a received {@link Buffer} into a
 * <a href="https://github.com/google/protobuf">Google Protocol Buffers</a>
 * {@link Message} and {@link MessageLite}. Please note that this decoder must
 * be used with a proper {@link ByteToMessageDecoder} such as {@link ProtobufVarint32FrameDecoder}
 * or {@link LengthFieldBasedFrameDecoder} if you are using a stream-based
 * transport such as TCP/IP. A typical setup for TCP/IP would be:
 * <pre>
 * {@link ChannelPipeline} pipeline = ...;
 *
 * // Decoders
 * pipeline.addLast("frameDecoder",
 *                  new {@link LengthFieldBasedFrameDecoder}(1048576, 0, 4, 0, 4));
 * pipeline.addLast("protobufDecoder",
 *                  new {@link ProtobufDecoder}(MyMessage.getDefaultInstance()));
 *
 * // Encoder
 * pipeline.addLast("frameEncoder", new {@link LengthFieldPrepender}(4));
 * pipeline.addLast("protobufEncoder", new {@link ProtobufEncoder}());
 * </pre>
 * and then you can use a {@code MyMessage} instead of a {@link Buffer}
 * as a message:
 * <pre>
 * void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     MyMessage req = (MyMessage) msg;
 *     MyMessage res = MyMessage.newBuilder().setText(
 *                               "Did you say '" + req.getText() + "'?").build();
 *     ch.write(res);
 * }
 * </pre>
 */
public class ProtobufDecoder extends MessageToMessageDecoder<Buffer> {

    private static final boolean HAS_PARSER;

    static {
        boolean hasParser = false;
        try {
            // MessageLite.getParserForType() is not available until protobuf 2.5.0.
            MessageLite.class.getDeclaredMethod("getParserForType");
            hasParser = true;
        } catch (Throwable t) {
            // Ignore
        }

        HAS_PARSER = hasParser;
    }

    private final MessageLite prototype;
    private final ExtensionRegistryLite extensionRegistry;

    /**
     * Creates a new instance.
     */
    public ProtobufDecoder(MessageLite prototype) {
        this(prototype, null);
    }

    public ProtobufDecoder(MessageLite prototype, ExtensionRegistry extensionRegistry) {
        this(prototype, (ExtensionRegistryLite) extensionRegistry);
    }

    public ProtobufDecoder(MessageLite prototype, ExtensionRegistryLite extensionRegistry) {
        requireNonNull(prototype, "prototype");
        this.prototype = prototype.getDefaultInstanceForType();
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Buffer msg) throws Exception {
        final int length = msg.readableBytes();
        final byte[] array = new byte[length];
        final int offset;
        msg.copyInto(msg.readerOffset(), array, 0, length);
        offset = 0;

        if (extensionRegistry == null) {
            if (HAS_PARSER) {
                ctx.fireChannelRead(prototype.getParserForType().parseFrom(array, offset, length));
            } else {
                ctx.fireChannelRead(prototype.newBuilderForType().mergeFrom(array, offset, length).build());
            }
        } else {
            if (HAS_PARSER) {
                ctx.fireChannelRead(prototype.getParserForType().parseFrom(
                        array, offset, length, extensionRegistry));
            } else {
                ctx.fireChannelRead(prototype.newBuilderForType().mergeFrom(
                        array, offset, length, extensionRegistry).build());
            }
        }
    }

    @Override
    public boolean isSharable() {
        return true;
    }
}
