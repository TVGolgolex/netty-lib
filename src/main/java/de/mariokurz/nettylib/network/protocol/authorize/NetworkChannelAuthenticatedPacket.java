package de.mariokurz.nettylib.network.protocol.authorize;

/*
 * MIT License
 *
 * Copyright (c) 2024 20:13 Mario Pascal K.
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

import de.mariokurz.nettylib.network.ChannelIdentity;
import de.mariokurz.nettylib.network.protocol.codec.PacketBuffer;
import de.mariokurz.nettylib.network.protocol.codec.osgan.annotation.PacketObjectSerial;
import de.mariokurz.nettylib.network.protocol.codec.selfbuild.SelfBuild;
import de.mariokurz.nettylib.network.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@PacketObjectSerial
public class NetworkChannelAuthenticatedPacket extends Packet implements Serializable, SelfBuild {
    private ChannelIdentity connectedChannel;

    @Override
    public int registerId() {
        return -1;
    }

    @Override
    public void writeBuffer(PacketBuffer packetBuffer) {
        packetBuffer.writeString(connectedChannel.namespace()).writeUniqueId(connectedChannel.uniqueId());
    }

    @Override
    public void readBuffer(PacketBuffer packetBuffer) {
        this.connectedChannel = new ChannelIdentity(packetBuffer.readString(), packetBuffer.readUniqueId());
    }
}
