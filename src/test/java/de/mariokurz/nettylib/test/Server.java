package de.mariokurz.nettylib.test;

/*
 * MIT License
 *
 * Copyright (c) 2024 23:20 Mario Pascal K.
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

import de.mariokurz.nettylib.Codec;
import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.network.channel.InactiveAction;
import de.mariokurz.nettylib.network.server.NetworkServer;
import de.mariokurz.nettylib.test.packet.AbstractPacket;
import de.mariokurz.nettylib.test.packet.ServerPacket;
import de.mariokurz.nettylib.test.packet.TestPacket;
import de.mariokurz.nettylib.test.receiver.AbstractPacketReceiver;
import de.mariokurz.nettylib.test.receiver.ServerPacketReceiver;
import de.mariokurz.nettylib.test.receiver.TestPacketReceiver;

public class Server {
    public static void main(String[] args) {

        NettyLib.DEV_MODE = true;

        var server = new NetworkServer(false, InactiveAction.RETRY, Codec.DYNAMIC_SELF_NETTY);

        server.connect("0.0.0.0", 9985);

        server.serverChannelTransmitter().packetReceiverManager().registerPacketHandler(TestPacket.class, TestPacketReceiver.class);
        server.serverChannelTransmitter().packetReceiverManager().registerPacketHandler(ServerPacket.class, ServerPacketReceiver.class);
        server.serverChannelTransmitter().packetReceiverManager().registerPacketHandler(AbstractPacket.class, AbstractPacketReceiver.class);

    }
}
