package de.mariokurz.nettylib.network.server;

/*
 * MIT License
 *
 * Copyright (c) 2024 23:31 Mario Pascal K.
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

import de.golgolex.quala.utils.data.Pair;
import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.network.channel.ChannelTransmitter;
import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.Packet;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelAuthenticatedPacket;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelAuthorizePacket;
import de.mariokurz.nettylib.network.protocol.query.QueryPacketManager;
import de.mariokurz.nettylib.network.protocol.receiver.PacketReceiverManager;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;

@Getter
public class ServerChannelTransmitter implements ChannelTransmitter {

    private final PacketReceiverManager packetReceiverManager = new PacketReceiverManager();
    private final QueryPacketManager queryPacketManager = new QueryPacketManager();
    private final Map<SocketAddress, Channel> unauthorized = new HashMap<>();
    private final Map<UUID, Pair<NetworkChannel, Long>> authorized = new HashMap<>();

    /**
     * Send a packet to all authorized network channels except those that match the given predicate.
     *
     * @param packet The packet to send.
     * @param ifNot  Predicate specifying which network channels should be excluded from receiving the packet.
     */
    @Override
    public void sendPacketToAll(Object packet, Predicate<NetworkChannel> ifNot) {
        // Iterate over each authorized network channel
        authorized.forEach((uuid, networkChannelLongPair) -> {
            // Check if a predicate is provided and the network channel does not match the predicate,
            // or if the network channel is inactive
            if (ifNot != null && !ifNot.test(networkChannelLongPair.first()) || networkChannelLongPair.first().inactive()) {
                return; // Skip sending the packet to this network channel
            }
            // Send the packet to the network channel
            networkChannelLongPair.first().sendPacket(packet);
        });
    }

    /**
     * Retrieves a collection of all network channels that are authorized.
     *
     * @return A collection of authorized network channels.
     */
    @Override
    public Collection<NetworkChannel> getNetworkChannels() {
        // Retrieve the network channels from the authorized map and convert them to a collection
        return authorized.values().stream()
                .map(Pair::first)
                .toList();
    }

    /**
     * Retrieves the network channel associated with the given channel object.
     *
     * @param channel The channel object to look up.
     * @return The associated network channel, or null if not found.
     */
    @Override
    public NetworkChannel getNetworkChannel(Channel channel) {
        // Iterate through each authorized entry
        for (var value : authorized.values()) {
            // Check if the remote address of the channel matches
            if (value.first().channel().remoteAddress().equals(channel.remoteAddress())) {
                // Return the associated network channel
                return value.first();
            }
        }
        return null; // Network channel not found
    }

    /**
     * Retrieves the network channel associated with the given namespace.
     *
     * @param namespace The namespace to look up.
     * @return The associated network channel, or null if not found.
     */
    @Override
    public NetworkChannel getNetworkChannel(String namespace) {
        // Iterate through each authorized entry
        for (var value : authorized.values()) {
            // Check if the namespace matches
            if (value.first().channelIdentity().namespace().equalsIgnoreCase(namespace)) {
                // Return the associated network channel
                return value.first();
            }
        }
        return null; // Network channel not found
    }

    /**
     * Retrieves the network channel associated with the given unique ID.
     *
     * @param uniqueId The unique ID to look up.
     * @return The associated network channel, or null if not found.
     */
    @Override
    public NetworkChannel getNetworkChannel(UUID uniqueId) {
        // Iterate through each authorized entry
        for (var value : authorized.values()) {
            // Check if the unique ID matches
            if (value.first().channelIdentity().uniqueId().equals(uniqueId)) {
                // Return the associated network channel
                return value.first();
            }
        }
        return null; // Network channel not found
    }

    /**
     * Dispatches a packet object to the appropriate handlers.
     *
     * @param packetObj           The packet object to dispatch.
     * @param channelHandlerContext The channel handler context associated with the packet object.
     */
    @Override
    public void dispatchPacketObject(Object packetObj, ChannelHandlerContext channelHandlerContext) {
        // Check if the packet object is an instance of Packet
        if (packetObj instanceof Packet packet) {
            // Retrieve the network channel associated with the channel handler context
            var networkChannel = this.getNetworkChannel(channelHandlerContext.channel());
            // Dispatch the packet to the query packet manager and packet receiver manager
            this.queryPacketManager.dispatch(packet);
            this.packetReceiverManager.dispatch(packet, networkChannel, channelHandlerContext);
        }
    }

    /**
     * Marks a network channel as active.
     *
     * @param ctx The channel handler context associated with the active network channel.
     */
    public void active(ChannelHandlerContext ctx) {
        // Iterate through each authorized entry
        for (Pair<NetworkChannel, Long> value : authorized.values()) {
            // Check if the remote address of the network channel matches
            if (value.first().channel().remoteAddress().equals(ctx.channel().remoteAddress())) {
                // Set the network channel as active
                value.first().inactive(false);
                break;
            }
        }
        // Move the channel from the unauthorized map to the authorized map
        unauthorized.put(ctx.channel().remoteAddress(), ctx.channel());
    }

    /**
     * Marks a network channel as inactive.
     *
     * @param ctx The channel handler context associated with the inactive network channel.
     */
    public void inactive(ChannelHandlerContext ctx) {
        // Iterate through each authorized entry
        for (Pair<NetworkChannel, Long> value : authorized.values()) {
            // Check if the remote address of the network channel matches
            if (value.first().channel().remoteAddress().equals(ctx.channel().remoteAddress())) {
                // Send a packet to all network channels indicating the channel is no longer authenticated
                sendPacketToAll(new NetworkChannelAuthenticatedPacket(value.first().channelIdentity()), null);
                // Set the network channel as inactive
                value.first().inactive(true);
                break;
            }
        }
    }

    /**
     * Authorizes a network channel.
     *
     * @param ctx                        The channel handler context associated with the network channel.
     * @param networkChannelAuthorizePacket The authorization packet containing the channel identity.
     */
    public void authorize(ChannelHandlerContext ctx, NetworkChannelAuthorizePacket networkChannelAuthorizePacket) {
        // Check if the network channel is not found in the unauthorized map
        if (!unauthorized.containsKey(ctx.channel().remoteAddress())) {
            // Log an error if the channel is not marked as waiting for authorization
            NettyLib.log(Level.SEVERE, this.getClass(), "Channel: " + ctx.channel().remoteAddress() + " is not marked: Waiting for authorization");
            return;
        }
        // Add the authorized network channel to the authorized map
        authorized.put(networkChannelAuthorizePacket.channelIdentity().uniqueId(), new Pair<>(
                new NetworkChannel(
                        networkChannelAuthorizePacket.channelIdentity(),
                        queryPacketManager,
                        ctx.channel(),
                        false
                ),
                System.currentTimeMillis()
        ));
        // Send a packet to all network channels indicating the channel is authenticated
        sendPacketToAll(new NetworkChannelAuthenticatedPacket(networkChannelAuthorizePacket.channelIdentity()), null);
        // Log the successful authorization of the network channel
        NettyLib.debug(Level.INFO, this.getClass(), "Authorized Channel: "
                + ctx.channel().remoteAddress() + " - "
                + networkChannelAuthorizePacket.channelIdentity().namespace() + ":"
                + networkChannelAuthorizePacket.channelIdentity().uniqueId());
    }

}
