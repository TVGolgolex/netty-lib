package de.mariokurz.nettylib.network.protocol.query;

/*
 * MIT License
 *
 * Copyright (c) 2024 00:34 Mario Pascal K.
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
import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.Packet;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Getter
public class QueryPacketManager {

    private static final Map<UUID, Value<QueryResult>> waitingHandlers = new ConcurrentHashMap<>(0);
    private final Scheduler executorService = new Scheduler(1);

    /**
     * Sends a query asynchronously over the network channel and returns a CompletableFuture
     * that will be completed with the response packet or null if no response is received.
     *
     * @param packet          The packet to be sent.
     * @param networkChannel  The network channel to send the packet over.
     * @return                A CompletableFuture that will be completed with the response packet,
     *                        or null if no response is received within the specified time limit.
     */
    public <T extends Packet> CompletableFuture<T> sendQueryFuture(
            @NonNull Object packet,
            @NonNull NetworkChannel networkChannel
    ) {
        // Check if the provided packet is a valid Packet instance
        if (!(packet instanceof Packet packetObj)) {
            // Log an information message if the packet is not valid
            NettyLib.log(Level.INFO, this.getClass(), "Packet Object is not a valid Packet instance: " + packet.getClass().getName());
            // Return null as the packet is not valid
            return null;
        }

        // Generate a unique query ID
        var queryUniqueId = UUID.randomUUID();
        // Assign the query ID to the packet
        packetObj.queryId(queryUniqueId);

        // Create a CompletableFuture to hold the result of the query
        var resultFuture = new CompletableFuture<T>();
        // Store a placeholder in the waitingHandlers map for the result
        waitingHandlers.put(queryUniqueId, new Value<>(null));
        // Schedule sending the packet over the network channel
        executorService.schedule(() -> networkChannel.sendPacket(packetObj));
        // Schedule a task to handle the response after a delay of 5000 milliseconds
        executorService.schedule(() -> {
            // Retrieve the result from waitingHandlers based on the query ID
            Value<QueryResult> result = waitingHandlers.get(queryUniqueId);
            // Remove the entry from waitingHandlers as the response has been received
            waitingHandlers.remove(queryUniqueId);
            // Check if the result is null or the packet in the result is null
            if (result == null || result.value() == null || result.value().packet() == null) {
                // Complete the CompletableFuture with null if the result is null
                resultFuture.complete(null);
            } else {
                // Check if the packet in the result is a valid Packet instance
                if (!(result.value().packet() instanceof Packet castedPacket)) {
                    // Log an information message if the packet is not valid
                    NettyLib.log(Level.INFO, this.getClass(), "Object Packet is not a valid Packet instance: " + result.value().packet().getClass().getName());
                    // Complete the CompletableFuture with null if the packet is not valid
                    resultFuture.complete(null);
                } else {
                    // Complete the CompletableFuture with the valid packet
                    resultFuture.complete((T) castedPacket);
                }
            }
        }, 5000);
        return resultFuture;
    }

    /**
     * Sends a query synchronously over the network channel and returns the response packet,
     * or null if no response is received within the specified time limit.
     *
     * @param packet          The packet to be sent.
     * @param networkChannel  The network channel to send the packet over.
     * @return                The response packet, or null if no response is received within
     *                        the specified time limit.
     */
    public <T extends Packet> T sendQuery(
            @NonNull Object packet,
            @NonNull NetworkChannel networkChannel
    ) {
        // Check if the provided packet is a valid Packet instance
        if (!(packet instanceof Packet packetObj)) {
            // Log an information message if the packet is not valid
            NettyLib.log(Level.INFO, this.getClass(), "Packet Object is not a valid Packet instance: " + packet.getClass().getName());
            // Return null as the packet is not valid
            return null;
        }

        // Generate a unique query ID
        var queryUniqueId = UUID.randomUUID();
        // Assign the query ID to the packet
        packetObj.queryId(queryUniqueId);

        // Store a placeholder in the waitingHandlers map for the result
        waitingHandlers.put(queryUniqueId, new Value<>(null));
        NettyLib.debug(Level.INFO, this.getClass(), "Added to Waiting Handler: " + packet.getClass().getSimpleName() + ": " + queryUniqueId);

        // Schedule sending the packet over the network channel
        executorService.schedule(() -> {
            NettyLib.debug(Level.INFO, this.getClass(), "Sending Query: " + packet.getClass().getSimpleName() + ": " + queryUniqueId);
            networkChannel.sendPacket(packetObj);
        });

        var i = 0;
        // Wait for the response or timeout after 5000 iterations
        while (waitingHandlers.get(queryUniqueId).value() == null && i++ < 5000) {
            try {
                // Sleep for 0.5 milliseconds
                Thread.sleep(0, 500000);
            } catch (InterruptedException ignored) {
            }
        }

        // If timeout occurs, set the result to indicate timeout
        if (i >= 4999) {
            waitingHandlers.get(queryUniqueId).value(new QueryResult(queryUniqueId, null));
        }

        // Retrieve the result from waitingHandlers based on the query ID
        var result = waitingHandlers.get(queryUniqueId);
        // Remove the entry from waitingHandlers as the response has been received
        waitingHandlers.remove(queryUniqueId);
        // Check if the result is null or the packet in the result is null
        if (result == null || result.value() == null || result.value().packet() == null) {
            // Return null if the result is null
            return null;
        }
        // Check if the packet in the result is a valid Packet instance
        if (!(result.value().packet() instanceof Packet castedPacket)) {
            // Log an information message if the packet is not valid
            NettyLib.log(Level.INFO, this.getClass(), "Object Packet is not a valid Packet instance: " + result.value().packet().getClass().getName());
            // Return null if the packet is not valid
            return null;
        }
        // Return the valid packet
        return (T) castedPacket;
    }

    /**
     * Dispatches a received packet to the appropriate waiting handler, if applicable.
     *
     * @param packet The packet to be dispatched.
     */
    public void dispatch(
            @NonNull Packet packet
    ) {
        // Check if the packet contains a query ID and if there's a waiting handler for it
        NettyLib.debug(Level.INFO, this.getClass(), "Checken Packet: " + packet.queryId() + "/" + packet.getClass().getSimpleName());
        if (packet.queryId() != null && waitingHandlers.containsKey(packet.queryId())) {
            NettyLib.debug(Level.INFO, this.getClass(), "Processing Query: " + packet.queryId() + "/" + packet.getClass().getSimpleName());
            // Retrieve the waiting handler associated with the packet's query ID
            var waitingHandler = waitingHandlers.get(packet.queryId());
            // Create a QueryResult object containing the query ID and the received packet
            NettyLib.debug(Level.INFO, this.getClass(), "Setting Value of Query to: " + packet.queryId() + "/" + packet.getClass().getSimpleName());
            var queryResult = new QueryResult(packet.queryId(), packet);
            // Set the value of the waiting handler to the QueryResult
            waitingHandler.value(queryResult);
        }
    }

}
