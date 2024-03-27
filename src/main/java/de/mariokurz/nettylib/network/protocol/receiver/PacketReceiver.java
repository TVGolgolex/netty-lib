package de.mariokurz.nettylib.network.protocol.receiver;

/*
 * MIT License
 *
 * Copyright (c) 2024 17:25 Mario Pascal K.
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

import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.Packet;
import lombok.NonNull;

import java.util.UUID;

public abstract class PacketReceiver<T extends Packet> {

    protected UUID queryId; // Declaration of a protected member variable queryId of type UUID. This variable is accessible within this class and its subclasses.

    public abstract void receivePacket(T packet, NetworkChannel networkChannel);
    // Abstract method declaration that defines a contract for classes extending this one. It specifies that any subclass must implement this method.
    // Parameters:
    //   - packet: The packet received.
    //   - networkChannel: The network channel through which the packet was received.

    public void respond(@NonNull Packet packet, @NonNull NetworkChannel networkChannel) {
        // Method to respond to a received packet.
        // Parameters:
        //   - packet: The packet to respond with.
        //   - networkChannel: The network channel through which the response will be sent.
        packet.queryId(queryId); // Set the queryId of the response packet to match the queryId of the original packet.
        networkChannel.sendPacket(packet); // Send the response packet through the provided network channel.
    }


}
