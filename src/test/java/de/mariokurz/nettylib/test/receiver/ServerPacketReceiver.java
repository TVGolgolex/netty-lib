package de.mariokurz.nettylib.test.receiver;

/*
 * MIT License
 *
 * Copyright (c) 2024 22:43 Mario Pascal K.
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

import de.golgolex.quala.json.JsonUtils;
import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.receiver.PacketReceiver;
import de.mariokurz.nettylib.test.packet.ServerPacket;

public class ServerPacketReceiver extends PacketReceiver<ServerPacket> {
    @Override
    public void receivePacket(ServerPacket packet, NetworkChannel networkChannel) {
        System.out.println(packet.abstractServer().toString());
    }
}
