package de.mariokurz.nettylib.network.protocol.receiver;

/*
 * MIT License
 *
 * Copyright (c) 2024 17:36 Mario Pascal K.
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
import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.Packet;
import io.netty5.channel.ChannelHandlerContext;

import java.util.*;
import java.util.logging.Level;

public class PacketReceiverManager {

    private final Map<Class<? extends Packet>, List<Class<? extends PacketReceiver<?>>>> packetReceivers = new HashMap<>();

    /**
     * Registers a packet handler for a specific type of packet.
     *
     * @param packet         The class object representing the type of packet to handle.
     * @param packetReceiver The class object representing the packet receiver to register.
     *                       It must implement the PacketReceiver interface for the corresponding packet type.
     * @param <P>            The type parameter representing the packet.
     */
    public <P extends Packet> void registerPacketHandler(Class<? extends Packet> packet, Class<? extends PacketReceiver<P>> packetReceiver) {
        // Check if there is already a list of packet receivers registered for this packet type
        if (!this.packetReceivers.containsKey(packet)) {
            // If not, create a new list
            this.packetReceivers.put(packet, new ArrayList<>());
        }
        // Add the packet receiver to the list of packet receivers for this packet type
        this.packetReceivers.get(packet).add(packetReceiver);
    }

    /**
     * Unregisters a packet handler for a specific type of packet.
     *
     * @param packet The class object representing the type of packet to handle.
     * @param handler The class object representing the packet receiver to unregister.
     * @param <P>    The type parameter representing the packet.
     * @return       True if the packet handler is successfully unregistered, false otherwise.
     */
    public <P extends Packet> boolean unregisterPacketHandler(Class<? extends Packet> packet, Class<? extends PacketReceiver<P>> handler) {
        // Check if there are packet handlers registered for this packet type
        if (!this.packetReceivers.containsKey(packet)) {
            return false; // No handlers registered for this packet type
        }
        // Retrieve the list of packet handlers for this packet type
        Collection<Class<? extends PacketReceiver<?>>> handlers = this.packetReceivers.get(packet);
        // Remove the specified packet handler from the list
        handlers.remove(handler);
        // If there are no more packet handlers registered for this packet type, remove the packet type entry
        if (handlers.isEmpty()) {
            this.packetReceivers.remove(packet);
        }
        return true; // Successfully unregistered the packet handler
    }

    /**
     * Retrieves the packet receivers registered for a specific type of packet.
     *
     * @param packet The packet for which to retrieve the packet receivers.
     * @param <P>    The type parameter representing the packet.
     * @return       A collection of packet receivers for the specified packet type.
     */
    public <P extends Packet> Collection<PacketReceiver<P>> getReceivers(P packet) {
        // Create a new collection to store the packet receivers
        Collection<PacketReceiver<P>> handlers = new LinkedList<>();
        // Check if there are packet receivers registered for the class of the given packet
        if (packetReceivers.containsKey(packet.getClass())) {
            // Iterate through each packet receiver class registered for the packet type
            for (Class<? extends PacketReceiver<?>> aClass : packetReceivers.get(packet.getClass())) {
                try {
                    // Instantiate a new instance of the packet receiver and add it to the collection
                    handlers.add((PacketReceiver<P>) aClass.newInstance());
                } catch (InstantiationException | IllegalAccessException | ClassCastException exception) {
                    // Log an error if instantiation fails or if there's a class cast exception
                    NettyLib.log(Level.SEVERE, this.getClass(), exception.getMessage());
                }
            }
        }
        return handlers; // Return the collection of packet receivers
    }

    /**
     * Dispatches a packet to all registered packet receivers for its type.
     *
     * @param packet             The packet to dispatch.
     * @param networkChannel     The network channel associated with the packet.
     * @param channelHandlerContext The channel handler context associated with the packet.
     * @param <P>                The type parameter representing the packet.
     * @return                   The number of packet receivers that were called.
     */
    public <P extends Packet> int dispatch(P packet, NetworkChannel networkChannel, ChannelHandlerContext channelHandlerContext) {
        var calledCount = 0; // Initialize the count of called packet receivers
        // Iterate through each packet receiver registered for the packet type
        for (var listener : this.getReceivers(packet)) {
            calledCount++; // Increment the count of called packet receivers
            // Set the query ID of the packet receiver if the packet contains a query ID
            if (packet.queryId() != null) {
                listener.queryId = packet.queryId();
            }
            // Call the receivePacket method of the packet receiver
            listener.receivePacket(packet, networkChannel);
        }
        return calledCount; // Return the number of packet receivers that were called
    }
}
