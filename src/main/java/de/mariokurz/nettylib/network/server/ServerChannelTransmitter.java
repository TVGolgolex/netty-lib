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
import de.mariokurz.nettylib.network.ChannelIdentity;
import de.mariokurz.nettylib.network.channel.ChannelTransmitter;
import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.Packet;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelAuthenticatedPacket;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelAuthorizePacket;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelInactivePacket;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelInitPacket;
import de.mariokurz.nettylib.network.protocol.query.QueryPacketManager;
import de.mariokurz.nettylib.network.protocol.receiver.PacketReceiverManager;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.NonNull;

import javax.annotation.Nullable;
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
    public void sendPacketToAll(
            @NonNull Object packet,
            @Nullable Predicate<NetworkChannel> ifNot
    ) {
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
    public NetworkChannel getNetworkChannel(
            @NonNull Channel channel
    ) {
        // Iterate through each authorized entry
        for (var value : authorized.values()) {
            // Check if the remote address of the channel matches
            if (value.first().channel().remoteAddress().equals(channel.remoteAddress()) && !value.first().inactive()) {
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
    public NetworkChannel getNetworkChannel(
            @NonNull String namespace
    ) {
        // Iterate through each authorized entry
        for (var value : authorized.values()) {
            // Check if the namespace matches
            if (value.first().channelIdentity().namespace().equalsIgnoreCase(namespace) && !value.first().inactive()) {
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
    public NetworkChannel getNetworkChannel(
            @NonNull UUID uniqueId
    ) {
        // Iterate through each authorized entry
        for (var value : authorized.values()) {
            // Check if the unique ID matches
            if (value.first().channelIdentity().uniqueId().equals(uniqueId) && !value.first().inactive()) {
                // Return the associated network channel
                return value.first();
            }
        }
        return null; // Network channel not found
    }

    /**
     * Retrieves the network channel associated with the given channel identity.
     *
     * @param channelIdentity The identity of the channel.
     * @return The associated network channel, or null if not found.
     */
    @Override
    public NetworkChannel getNetworkChannel(
            @NonNull ChannelIdentity channelIdentity
    ) {
        // Iterate through each authorized entry
        for (var value : authorized.values()) {
            // Check if the channelIdentity matches
            if (value.first().channelIdentity().equals(channelIdentity) && !value.first().inactive()) {
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
    public void dispatchPacketObject(
            @NonNull Object packetObj,
            @NonNull ChannelHandlerContext channelHandlerContext
    ) {
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
    public void active(
            @NonNull ChannelHandlerContext ctx
    ) {
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
    public void inactive(
            @NonNull ChannelHandlerContext ctx
    ) {
        // Iterate through each authorized entry
        for (Pair<NetworkChannel, Long> value : authorized.values()) {
            // Check if the remote address of the network channel matches
            if (value.first().channel().remoteAddress().equals(ctx.channel().remoteAddress())) {
                // Send a packet to all network channels indicating the channel is no longer authenticated
                sendPacketToAll(new NetworkChannelInactivePacket(value.first().channelIdentity()),
                        networkChannels -> networkChannels.channelIdentity().equals(value.first().channelIdentity()));
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
    public void authorize(
            @NonNull ChannelHandlerContext ctx,
            @NonNull NetworkChannelAuthorizePacket networkChannelAuthorizePacket
    ) {
        // Check if the channel is not marked as waiting for authorization
        if (!unauthorized.containsKey(ctx.channel().remoteAddress())) {
            // Log a severe error if the channel is not marked as waiting for authorization
            NettyLib.log(Level.SEVERE, this.getClass(), "Channel: " + ctx.channel().remoteAddress() + " is not marked: Waiting for authorization");
            return;
        }
        // Create a new network channel with the provided channel identity and context
        var networkChannel = new NetworkChannel(
                networkChannelAuthorizePacket.channelIdentity(),
                queryPacketManager,
                null, // null argument to be filled later
                ctx.channel(),
                false
        );
        // Send an authentication packet to all network channels
        sendPacketToAll(new NetworkChannelAuthenticatedPacket(networkChannelAuthorizePacket.channelIdentity()), null);
        // Send a network channel initialization packet to the newly authorized network channel
        networkChannel.sendPacket(new NetworkChannelInitPacket(this.getNetworkChannels().stream()
                .filter(networkChannels -> !networkChannels.inactive())
                .map(NetworkChannel::channelIdentity)
                .toList()));
        // Add the authorized network channel to the authorized map
        authorized.put(networkChannelAuthorizePacket.channelIdentity().uniqueId(), new Pair<>(networkChannel,
                System.currentTimeMillis()
        ));
        // Log the successful authorization of the network channel
        NettyLib.debug(Level.INFO, this.getClass(), "Authorized Channel: "
                + ctx.channel().remoteAddress() + " - "
                + networkChannelAuthorizePacket.channelIdentity().namespace() + ":"
                + networkChannelAuthorizePacket.channelIdentity().uniqueId());
    }

}
