package de.mariokurz.nettylib.network.channel;

/*
 * MIT License
 *
 * Copyright (c) 2024 23:43 Mario Pascal K.
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

import com.github.golgolex.eventum.EventManager;
import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.event.NetworkChannelPacketSendEvent;
import de.mariokurz.nettylib.network.ChannelIdentity;
import de.mariokurz.nettylib.network.protocol.Packet;
import de.mariokurz.nettylib.network.protocol.query.QueryPacketManager;
import de.mariokurz.nettylib.network.protocol.routing.RoutingPacketManager;
import de.mariokurz.nettylib.network.protocol.routing.RoutingResult;
import de.mariokurz.nettylib.utils.NettyUtils;
import io.netty5.channel.Channel;
import io.netty5.util.concurrent.Future;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Getter
@AllArgsConstructor
public class NetworkChannel {

    protected final ChannelIdentity channelIdentity;
    protected final QueryPacketManager queryPacketManager;
    protected final RoutingPacketManager routingPacketManager;
    @Setter
    protected Channel channel;
    @Setter
    protected boolean inactive;

    /**
     * Sends one or more packets asynchronously over the network channel.
     *
     * @param packets The packets to be sent.
     */
    public void sendPacket(
            @NonNull Object... packets
    ) {
        // Iterate through each packet in the packets array
        for (var packet : packets) {
            // Write the packet to the channel without waiting for completion
            this.writePacket(packet, false);
        }
        // Flush the channel to reduce I/O load
        this.channel.flush();
    }

    /**
     * Sends one or more packets synchronously over the network channel.
     *
     * @param packets The packets to be sent.
     */
    public void sendPacketSync(
            @NonNull Object... packets
    ) {
        // Iterate through each packet in the packets array
        for (var packet : packets) {
            // Write the packet to the channel and wait for its completion
            var future = this.writePacket(packet, false);
            if (future != null) {
                NettyUtils.awaitFuture(future);
            }
        }
        // Flush the channel to reduce I/O load
        this.channel.flush();
    }

    /**
     * Sends a packet asynchronously over the network channel.
     *
     * @param packet The packet to be sent.
     */
    public void sendPacket(
            @NonNull Object packet
    ) {
        // Check if the current thread is in the event loop
        if (this.channel.executor().inEventLoop()) {
            // If yes, write the packet to the channel directly
            this.writePacket(packet, true);
        } else {
            // If no, execute the write operation in the event loop
            this.channel.executor().execute(() -> this.writePacket(packet, true));
        }
    }

    /**
     * Sends a packet synchronously over the network channel.
     *
     * @param packet The packet to be sent.
     */
    public void sendPacketSync(
            @NonNull Object packet
    ) {
        // Write the packet to the channel and wait for its completion
        var future = this.writePacket(packet, true);
        if (future != null) {
            NettyUtils.awaitFuture(future);
        }
    }

    /**
     * Sends a query packet asynchronously and returns a CompletableFuture for the response.
     *
     * @param packet The query packet to be sent.
     * @return A CompletableFuture representing the response packet.
     */
    public <T extends Packet> T sendQuery(
            @NonNull Object packet
    ) {
        // Delegate sending the query packet to the query packet manager
        return this.queryPacketManager.sendQuery(packet, this);
    }

    /**
     * Sends a query packet asynchronously and returns a CompletableFuture for the response.
     *
     * @param packet The query packet to be sent.
     * @return A CompletableFuture representing the response packet.
     */
    public <T extends Packet> CompletableFuture<T> sendQueryFuture(
            @NonNull Object packet
    ) {
        // Delegate sending the query packet to the query packet manager
        return this.queryPacketManager.sendQueryFuture(packet, this);
    }

    /**
     * Sends a packet to a specified receiver and waits for a routing result synchronously.
     *
     * @param packet           The packet to send.
     * @param receiverIdentity The identity of the receiver.
     * @return The routing result.
     */
    public RoutingResult sendRoutedPacket(
            @NonNull Object packet,
            @NonNull ChannelIdentity receiverIdentity
    ) {
        // Delegate the task to the routing packet manager
        return this.routingPacketManager.sendRoutedPacket(packet, this, receiverIdentity);
    }

    /**
     * Sends a packet to a specified receiver and waits for a routing result asynchronously.
     *
     * @param packet           The packet to send.
     * @param receiverIdentity The identity of the receiver.
     * @return A CompletableFuture containing the routing result.
     */
    public CompletableFuture<RoutingResult> sendRoutedPacketFuture(
            @NonNull Object packet,
            @NonNull ChannelIdentity receiverIdentity
    ) {
        // Delegate the task to the routing packet manager
        return this.routingPacketManager.sendRoutedPacketFuture(packet, this, receiverIdentity);
    }


    /**
     * Writes a packet to the network channel and optionally flushes the channel.
     *
     * @param packet      The packet to be written to the channel.
     * @param flushAfter  Indicates whether to flush the channel after writing the packet.
     * @return A Future representing the result of the write operation.
     */
    private Future<Void> writePacket(
            @NonNull Object packet,
            boolean flushAfter
    ) {
        // Trigger a packet send event before writing the packet to the channel
        EventManager.call(new NetworkChannelPacketSendEvent(this, packet));
        // Debug
        NettyLib.debug(Level.INFO, this.getClass(), "Write Packet: " + packet.getClass().getSimpleName() + ": Flush: " + flushAfter);
        // Write the packet to the channel and return the write operation's Future
        return flushAfter ? this.channel.writeAndFlush(packet) : this.channel.write(packet);
    }

}
