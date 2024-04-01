package de.mariokurz.nettylib.network.protocol.register;

/*
 * MIT License
 *
 * Copyright (c) 2024 01:45 Mario Pascal K.
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
import de.golgolex.quala.utils.handler.IConstructionExecutor;
import de.mariokurz.nettylib.network.protocol.Packet;
import de.mariokurz.nettylib.network.protocol.authorize.*;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PacketRegistry {

    private final Map<Pair<Class<? extends Packet>, Integer>, IConstructionExecutor<? extends Packet, Integer>> cache = new HashMap<>();

    public PacketRegistry() {
        this.register(NetworkChannelAuthenticatedPacket.class, -1, integer -> new NetworkChannelAuthenticatedPacket(null));
        this.register(NetworkChannelAuthorizePacket.class, -2, integer -> new NetworkChannelAuthorizePacket(null));
        this.register(NetworkChannelInactivePacket.class, -3, integer -> new NetworkChannelInactivePacket(null));
        this.register(NetworkChannelInitPacket.class, -4, integer -> new NetworkChannelInitPacket());
        this.register(NetworkChannelStayActivePacket.class, -5, integer -> new NetworkChannelStayActivePacket(null));
    }

    public <T extends Packet> void register(
            @NonNull Class<T> packetClazz,
            int registryId,
            @NonNull IConstructionExecutor<T, Integer> construction
    ) {
        for (var classIntegerPair : cache.keySet()) {
            if (classIntegerPair.second().equals(registryId)) {
                System.out.println(packetClazz.getSimpleName() + " cannot be registered on the Id: " + registryId + " because: it has: " + classIntegerPair.first().getSimpleName());
                return;
            }
        }

        this.cache.put(new Pair<>(packetClazz, registryId), construction);
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet> T construct(int registryId) {
        for (var entry : this.cache.entrySet()) {
            if (entry.getKey().second() == registryId) {
                return (T) entry.getValue().construct(registryId);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet> T construct(Class<?> clazz) {
        for (var entry : this.cache.entrySet()) {
            if (entry.getKey().first().equals(clazz)) {
                return (T) entry.getValue().construct(entry.getKey().second());
            }
        }
        return null;
    }

}
