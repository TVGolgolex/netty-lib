package de.mario.nettylib.network.channel;

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
import de.mario.nettylib.event.NetworkChannelPacketSendEvent;
import de.mario.nettylib.network.ChannelIdentity;
import de.mario.nettylib.network.protocol.Packet;
import de.mario.nettylib.utils.NettyUtils;
import io.netty5.channel.Channel;
import io.netty5.util.concurrent.Future;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;

@Getter
@AllArgsConstructor
public class NetworkChannel {

    protected final ChannelIdentity channelIdentity;
    @Setter
    protected Channel channel;
    @Setter
    protected boolean inactive;

    public void sendPacket(@NonNull Object... packets) {
        for (var packet : packets) {
            this.writePacket(packet, false);
        }
        this.channel.flush(); // reduces i/o load
    }

    public void sendPacketSync(@NonNull Object... packets) {
        for (var packet : packets) {
            var future = this.writePacket(packet, false);
            if (future != null) {
                NettyUtils.awaitFuture(future);
            }
        }
        this.channel.flush(); // reduces i/o load
    }

    public void sendPacket(@NonNull Object packet) {
        if (this.channel.executor().inEventLoop()) {
            this.writePacket(packet, true);
        } else {
            this.channel.executor().execute(() -> this.writePacket(packet, true));
        }
    }

    public void sendPacketSync(@NonNull Object packet) {
        var future = this.writePacket(packet, true);
        if (future != null) {
            NettyUtils.awaitFuture(future);
        }
    }

    public Packet sendQuery(@NonNull Object packet) {
        return null;
    }

    public CompletableFuture<Packet> sendQuery(@NonNull Object packet) {
        return null;
    }

    private Future<Void> writePacket(@NonNull Object packet, boolean flushAfter) {
        EventManager.call(new NetworkChannelPacketSendEvent(this, packet));
        return flushAfter ? this.channel.writeAndFlush(packet) : this.channel.write(packet);
    }

}
