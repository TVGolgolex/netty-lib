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

import io.netty.contrib.handler.codec.marshalling.DefaultMarshallerProvider;
import io.netty.contrib.handler.codec.marshalling.UnmarshallerProvider;
import io.netty5.channel.ChannelHandlerContext;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

/**
 * Default implementation of {@link io.netty.contrib.handler.codec.marshalling.UnmarshallerProvider} which will just create a new {@link Unmarshaller}
 * on every call to {@link #getUnmarshaller(ChannelHandlerContext)}
 */
public class DefaultUnmarshallerProvider implements UnmarshallerProvider {

    private final MarshallerFactory factory;
    private final MarshallingConfiguration config;

    /**
     * Create a new instance of {@link DefaultMarshallerProvider}
     *
     * @param factory the {@link MarshallerFactory} to use to create {@link Unmarshaller}
     * @param config  the {@link MarshallingConfiguration}
     */
    public DefaultUnmarshallerProvider(MarshallerFactory factory, MarshallingConfiguration config) {
        this.factory = factory;
        this.config = config;
    }

    @Override
    public Unmarshaller getUnmarshaller(ChannelHandlerContext ctx) throws Exception {
        return factory.createUnmarshaller(config);
    }
}
