package de.mariokurz.nettylib.network.protocol.routing;

/*
 * MIT License
 *
 * Copyright (c) 2024 21:03 Mario Pascal K.
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

import de.golgolex.quala.scheduler.Scheduler;
import de.golgolex.quala.utils.data.Value;
import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.network.ChannelIdentity;
import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.Packet;
import lombok.NonNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class RoutingPacketManager {

    private static final Map<UUID, Value<RoutingResult>> waitingHandlers = new ConcurrentHashMap<>(0);
    private final Scheduler executorService = new Scheduler(1);

    /**
     * Sends a packet to a specified receiver and waits for a routing result asynchronously.
     *
     * @param packet              The packet to send.
     * @param senderNetworkChannel The network channel sending the packet.
     * @param receiverIdentity    The identity of the receiver.
     * @return A CompletableFuture containing the routing result.
     */
    public CompletableFuture<RoutingResult> sendRoutedPacketFuture(
            @NonNull Object packet,
            @NonNull NetworkChannel senderNetworkChannel,
            @NonNull ChannelIdentity receiverIdentity
    ) {
        // Check if the packet is a valid RoutingPacket instance
        if (!(packet instanceof RoutingPacket packetObj)) {
            NettyLib.log(Level.INFO, this.getClass(), "Packet Object is not a valid Packet instance: " + packet.getClass().getName());
            return null;
        }

        // Generate a unique query ID for the packet
        var queryUniqueId = UUID.randomUUID();
        packetObj.queryId(queryUniqueId);
        packetObj.receiverIdentity(receiverIdentity);

        // Create a CompletableFuture to hold the result of the routing
        var resultFuture = new CompletableFuture<RoutingResult>();
        // Put a placeholder value into the waiting handlers map for the query ID
        waitingHandlers.put(queryUniqueId, new Value<>(RoutingResult.IDLE));
        // Send the packet asynchronously
        executorService.schedule(() -> senderNetworkChannel.sendPacket(packetObj));

        // Schedule a task to handle the result after a timeout
        executorService.schedule(() -> {
            // Retrieve the result from the waiting handlers map
            Value<RoutingResult> result = waitingHandlers.get(queryUniqueId);
            // Remove the entry from the waiting handlers map
            waitingHandlers.remove(queryUniqueId);
            // Complete the CompletableFuture with the retrieved result or NO_RESULT if none found
            if (result == null || result.value() == null) {
                resultFuture.complete(RoutingResult.NO_RESULT);
            } else {
                resultFuture.complete(result.value());
            }
        }, 5000);
        return resultFuture;
    }

    /**
     * Sends a packet to a specified receiver and waits for a routing result synchronously.
     *
     * @param packet              The packet to send.
     * @param senderNetworkChannel The network channel sending the packet.
     * @param receiverIdentity    The identity of the receiver.
     * @return The routing result.
     */
    public RoutingResult sendRoutedPacket(
            @NonNull Object packet,
            @NonNull NetworkChannel senderNetworkChannel,
            @NonNull ChannelIdentity receiverIdentity
    ) {
        // Check if the packet is a valid RoutingPacket instance
        if (!(packet instanceof RoutingPacket packetObj)) {
            NettyLib.log(Level.INFO, this.getClass(), "Packet Object is not a valid Packet instance: " + packet.getClass().getName());
            return null;
        }

        // Generate a unique query ID for the packet
        var queryUniqueId = UUID.randomUUID();
        packetObj.queryId(queryUniqueId);
        packetObj.receiverIdentity(receiverIdentity);

        // Put a placeholder value into the waiting handlers map for the query ID
        waitingHandlers.put(queryUniqueId, new Value<>(RoutingResult.IDLE));
        // Send the packet asynchronously
        executorService.schedule(() -> senderNetworkChannel.sendPacket(packetObj));

        // Wait for the result to be updated in the waiting handlers map
        var i = 0;
        while (waitingHandlers.get(queryUniqueId).value() == RoutingResult.IDLE && i++ < 5000) {
            try {
                Thread.sleep(0, 500000);
            } catch (InterruptedException ignored) {
            }
        }

        // If the timeout is reached, set the result to NO_RESULT
        if (i >= 4999) {
            waitingHandlers.get(queryUniqueId).value(RoutingResult.NO_RESULT);
        }

        // Retrieve the result from the waiting handlers map
        var result = waitingHandlers.get(queryUniqueId);
        // Remove the entry from the waiting handlers map
        waitingHandlers.remove(queryUniqueId);
        // Return the retrieved result
        if (result == null || result.value() == null) {
            return null;
        }
        return result.value();
    }

    /**
     * Dispatches a packet to handle the routing result.
     *
     * @param packet The packet to dispatch.
     */
    public void dispatch(
            @NonNull Packet packet
    ) {
        // Check if the packet contains a query ID and if there's a waiting handler for it
        NettyLib.debug(Level.INFO, this.getClass(), "Checking Packet: " + packet.queryId() + "/" + packet.getClass().getSimpleName());
        if (packet instanceof RoutingResultPacket routingResultPacket
                && packet.queryId() != null
                && waitingHandlers.containsKey(packet.queryId())) {
            // Log debug information
            NettyLib.debug(Level.INFO, this.getClass(), "Processing Query: " + packet.queryId() + "/" + packet.getClass().getSimpleName());
            // Retrieve the waiting handler for the query ID
            var waitingHandler = waitingHandlers.get(packet.queryId());
            // Log debug information
            NettyLib.debug(Level.INFO, this.getClass(), "Setting Value of Query to: " + packet.queryId() + "/" + packet.getClass().getSimpleName());
            // Set the value of the waiting handler to the received routing result
            waitingHandler.value(routingResultPacket.result());
        }
    }

}
