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

import io.netty.contrib.handler.codec.marshalling.DefaultUnmarshallerProvider;
import io.netty.contrib.handler.codec.marshalling.UnmarshallerProvider;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandler;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.util.Attribute;
import io.netty5.util.AttributeKey;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

/**
 * {@link UnmarshallerProvider} which store a reference to the {@link Unmarshaller} in the
 * {@link Channel} via the {@link Channel#attr(AttributeKey)}
 * method. So the same {@link Unmarshaller} will be used during the life-time of a {@link Channel}
 * for the {@link ChannelHandler}'s {@link ChannelHandlerContext}.
 */
public class ContextBoundUnmarshallerProvider extends DefaultUnmarshallerProvider {

    private static final AttributeKey<Unmarshaller> UNMARSHALLER = AttributeKey.valueOf(
            ContextBoundUnmarshallerProvider.class, "UNMARSHALLER");

    public ContextBoundUnmarshallerProvider(MarshallerFactory factory, MarshallingConfiguration config) {
        super(factory, config);
    }

    @Override
    public Unmarshaller getUnmarshaller(ChannelHandlerContext ctx) throws Exception {
        Attribute<Unmarshaller> attr = ctx.channel().attr(UNMARSHALLER);
        Unmarshaller unmarshaller = attr.get();
        if (unmarshaller == null) {
            unmarshaller = super.getUnmarshaller(ctx);
            attr.set(unmarshaller);
        }
        return unmarshaller;
    }
}
