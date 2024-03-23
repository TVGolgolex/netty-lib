package de.mario.nettylib.network.channel;

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
import de.mario.nettylib.NettyLib;
import de.mario.nettylib.network.ChannelIdentity;
import de.mario.nettylib.network.protocol.authorize.AuthorizePacket;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

@Getter
public class ChannelTransmitter {

    private final Map<SocketAddress, Channel> unauthorized = new HashMap<>();
    private final Map<UUID, Pair<NetworkChannel, Long>> authorized = new HashMap<>();

    public void active(ChannelHandlerContext ctx) {
        for (Pair<NetworkChannel, Long> value : authorized.values()) {
            if (value.first().channel().remoteAddress().equals(ctx.channel().remoteAddress())) {
                value.first().inactive(false);
                break;
            }
        }
        unauthorized.put(ctx.channel().remoteAddress(), ctx.channel());
    }

    public void inactive(ChannelHandlerContext ctx) {
        for (Pair<NetworkChannel, Long> value : authorized.values()) {
            if (value.first().channel().remoteAddress().equals(ctx.channel().remoteAddress())) {
                value.first().inactive(true);
                break;
            }
         }
    }

    public void authorize(ChannelHandlerContext ctx, AuthorizePacket authorizePacket) {
        if (!unauthorized.containsKey(ctx.channel().remoteAddress())) {
            NettyLib.log(Level.SEVERE, this.getClass(), "Channel: " + ctx.channel().remoteAddress() + " is not marked: Waiting for authorization");
            return;
        }
        authorized.put(authorizePacket.channelIdentity().uniqueId(), new Pair<>(
                new NetworkChannel(
                        authorizePacket.channelIdentity(),
                        ctx.channel(),
                        false
                ),
                System.currentTimeMillis()
        ));
        NettyLib.debug(Level.INFO, this.getClass(), "Authorized Channel: "
                + ctx.channel().remoteAddress() + " - "
                + authorizePacket.channelIdentity().namespace() + ":"
                + authorizePacket.channelIdentity().uniqueId());
    }

}
