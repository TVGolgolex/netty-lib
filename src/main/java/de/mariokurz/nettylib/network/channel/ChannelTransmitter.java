package de.mariokurz.nettylib.network.channel;

/*
 * MIT License
 *
 * Copyright (c) 2024 18:14 Mario Pascal K.
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

import de.mariokurz.nettylib.network.ChannelIdentity;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

public interface ChannelTransmitter {

    /**
     * Send a packet to all authorized network channels except those that match the given predicate.
     *
     * @param packet The packet to send.
     * @param ifNot  Predicate specifying which network channels should be excluded from receiving the packet.
     */
    void sendPacketToAll(@NonNull Object packet, @Nullable  Predicate<NetworkChannel> ifNot);

    /**
     * Retrieves a collection of all network channels that are authorized.
     *
     * @return A collection of authorized network channels.
     */
    Collection<NetworkChannel> getNetworkChannels();

    /**
     * Retrieves the network channel associated with the given channel object.
     *
     * @param channel The channel object to look up.
     * @return The associated network channel, or null if not found.
     */
    NetworkChannel getNetworkChannel(@NonNull Channel channel);

    /**
     * Retrieves the network channel associated with the given namespace.
     *
     * @param namespace The namespace to look up.
     * @return The associated network channel, or null if not found.
     */
    NetworkChannel getNetworkChannel(@NonNull String namespace);

    /**
     * Retrieves the network channel associated with the given unique ID.
     *
     * @param uniqueId The unique ID to look up.
     * @return The associated network channel, or null if not found.
     */
    NetworkChannel getNetworkChannel(@NonNull UUID uniqueId);

    /**
     * Retrieves the network channel associated with the given channel identity.
     *
     * @param channelIdentity The identity of the channel.
     * @return The associated network channel, or null if not found.
     */
    NetworkChannel getNetworkChannel(@NonNull ChannelIdentity channelIdentity);

    /**
     * Dispatches a packet object to the appropriate handlers.
     *
     * @param packetObj           The packet object to dispatch.
     * @param channelHandlerContext The channel handler context associated with the packet object.
     */
    void dispatchPacketObject(@NonNull Object packetObj, @NonNull ChannelHandlerContext channelHandlerContext);

}
