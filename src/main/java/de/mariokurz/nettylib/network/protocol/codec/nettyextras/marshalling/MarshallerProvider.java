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

import io.netty5.channel.ChannelHandlerContext;
import org.jboss.marshalling.Marshaller;

/**
 * This provider is responsible to get a {@link Marshaller} for the given {@link ChannelHandlerContext}.
 */
public interface MarshallerProvider {

    /**
     * Get a {@link Marshaller} for the given {@link ChannelHandlerContext}
     */
    Marshaller getMarshaller(ChannelHandlerContext ctx) throws Exception;
}
