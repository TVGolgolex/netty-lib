package de.mariokurz.nettylib.test;

/*
 * MIT License
 *
 * Copyright (c) 2024 23:28 Mario Pascal K.
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

import de.golgolex.quala.json.document.JsonDocument;
import de.golgolex.quala.scheduler.Scheduler;
import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.network.ChannelIdentity;
import de.mariokurz.nettylib.network.channel.InactiveAction;
import de.mariokurz.nettylib.network.client.NetworkClient;
import de.mariokurz.nettylib.test.packet.*;

import java.util.UUID;

public class Client {
    public static void main(String[] args) {

        NettyLib.DEV_MODE = true;

        var client = new NetworkClient(
                new ChannelIdentity(
                        "Test-1",
                        UUID.randomUUID()
                ),
                InactiveAction.RETRY,
                false);

        client.connect("0.0.0.0", 9985);

        Scheduler.runtimeScheduler().schedule(() -> {
            client.thisNetworkChannel().sendPacket(new TestPacket("Hallo", new JsonDocument("Test", "Test")));
            System.out.println("test");

            TestPacket result = client.thisNetworkChannel().sendQuery(new TestPacket("Test", new JsonDocument("abc", "fick")));
            System.out.println(result.message());

            System.out.println(result.jsonDocument().readString("abc"));

/*            TestRoutingPacket testRoutingPacket = new TestRoutingPacket(StringUtils.generateRandomString(8),
                    UUID.randomUUID(), new ArrayList<>());

            for (int i = 0; i < 25; i++) {
                testRoutingPacket.strings().add(StringUtils.generateRandomString(Quala.randomNumber(3, 15)));
            }

            var resultd = client.networkChannel().sendRoutedPacket(testRoutingPacket,
                    client.clientChannelTransmitter().getNetworkChannel("Test-2").channelIdentity());
            System.out.println(resultd.name());*/

/*            client.networkChannel().sendPacket(new ServerPacket(new Java()));
            client.networkChannel().sendPacket(new ServerPacket(new Proxy()));*/

        }, 4000);

    }
}
