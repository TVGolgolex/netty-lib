package de.mariokurz.nettylib.network.client;

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

import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.network.ChannelIdentity;
import de.mariokurz.nettylib.network.channel.ChannelTransmitter;
import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.Packet;
import de.mariokurz.nettylib.utils.NettyUtils;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;

@AllArgsConstructor
public class ClientChannelTransmitter implements ChannelTransmitter {

    private final NetworkClient networkClient;
    private final Map<UUID, NetworkChannel> networkChannels = new HashMap<>();

    /**
     * Send a packet to all authorized network channels except those that match the given predicate.
     *
     * @param packet The packet to send.
     * @param ifNot  Predicate specifying which network channels should be excluded from receiving the packet.
     */
    @Override
    public void sendPacketToAll(Object packet, Predicate<NetworkChannel> ifNot) {
        networkChannels.forEach((uuid, networkChannel) -> {
            if (ifNot != null && !ifNot.test(networkChannel) || networkChannel.inactive()) {
                return;
            }
            networkChannel.sendPacket(packet);
        });
    }

    /**
     * Retrieves a collection of all network channels that are authorized.
     *
     * @return A collection of authorized network channels.
     */
    @Override
    public Collection<NetworkChannel> getNetworkChannels() {
        return networkChannels.values().stream().toList();
    }

    /**
     * Retrieves the network channel associated with the given channel object.
     *
     * @param channel The channel object to look up.
     * @return The associated network channel, or null if not found.
     */
    @Override
    public NetworkChannel getNetworkChannel(Channel channel) {
        for (var value : networkChannels.values()) {
            if (value.channel().remoteAddress().equals(channel.remoteAddress())) {
                return value;
            }
        }
        return null;
    }

    /**
     * Retrieves the network channel associated with the given namespace.
     *
     * @param namespace The namespace to look up.
     * @return The associated network channel, or null if not found.
     */
    @Override
    public NetworkChannel getNetworkChannel(String namespace) {
        for (var value : networkChannels.values()) {
            if (value.channelIdentity().namespace().equalsIgnoreCase(namespace)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Retrieves the network channel associated with the given unique ID.
     *
     * @param uniqueId The unique ID to look up.
     * @return The associated network channel, or null if not found.
     */
    @Override
    public NetworkChannel getNetworkChannel(UUID uniqueId) {
        for (var value : networkChannels.values()) {
            if (value.channelIdentity().uniqueId().equals(uniqueId)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Dispatches a packet object to the appropriate handlers.
     *
     * @param packetObj           The packet object to dispatch.
     * @param channelHandlerContext The channel handler context associated with the packet object.
     */
    @Override
    public void dispatchPacketObject(Object packetObj, ChannelHandlerContext channelHandlerContext) {
        if (packetObj instanceof Packet packet) {
            var networkChannel = this.getNetworkChannel(channelHandlerContext.channel());
            this.networkClient.queryPacketManager.dispatch(packet);
            this.networkClient.packetReceiverManager.dispatch(packet, networkChannel, channelHandlerContext);
        }
    }

    /**
     * Creates a new network channel and adds it to the network channel map.
     *
     * @param channelIdentity      The identity of the channel.
     * @param channelHandlerContext The context of the channel.
     */
    public void createNetworkChannel(ChannelIdentity channelIdentity, ChannelHandlerContext channelHandlerContext) {
        // Creates a new network channel with the specified parameters
        networkChannels.put(channelIdentity.uniqueId(), new NetworkChannel(
                channelIdentity,
                networkClient.queryPacketManager,
                channelHandlerContext.channel(),
                false
        ));
        // Logs the creation of the new network channel
        NettyLib.log(Level.INFO, "Created new NetworkChannel for: {0} / {1}", channelHandlerContext.channel().remoteAddress(), channelIdentity.toString());
    }

    /**
     * Removes a network channel from the network channel map.
     *
     * @param channelHandlerContext The context of the channel to be removed.
     */
    public void removeNetworkChannel(ChannelHandlerContext channelHandlerContext) {
        // Retrieves the network channel associated with the given context
        var networkChannel = this.getNetworkChannel(channelHandlerContext.channel());
        // Checks if a network channel was found
        if (networkChannel == null) {
            // Logs a warning if no network channel was found
            NettyLib.log(Level.WARNING, this.getClass(), "No NetworkChannel for: {0} existing", channelHandlerContext.channel().remoteAddress());
            return;
        }
        // Removes the network channel from the network channel map
        networkChannels.remove(networkChannel.channelIdentity().uniqueId());
        // Logs the removal of the network channel
        NettyLib.log(Level.INFO, "Removed NetworkChannel for: {0} / {1}", channelHandlerContext.channel().remoteAddress(), networkChannel.channelIdentity().toString());
    }
}
