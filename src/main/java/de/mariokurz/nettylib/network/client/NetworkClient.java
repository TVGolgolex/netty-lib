package de.mariokurz.nettylib.network.client;

/*
 * MIT License
 *
 * Copyright (c) 2024 23:04 Mario Pascal K.
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

import de.golgolex.quala.ConsoleColor;
import de.golgolex.quala.Quala;
import de.golgolex.quala.scheduler.Scheduler;
import de.mariokurz.nettylib.Codec;
import de.mariokurz.nettylib.ConnectionState;
import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.network.ChannelIdentity;
import de.mariokurz.nettylib.network.channel.InactiveAction;
import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.authorize.NetworkChannelStayActivePacket;
import de.mariokurz.nettylib.network.protocol.query.QueryPacketManager;
import de.mariokurz.nettylib.network.protocol.receiver.PacketReceiverManager;
import de.mariokurz.nettylib.network.protocol.register.PacketRegistry;
import de.mariokurz.nettylib.network.protocol.routing.RoutingPacketManager;
import de.mariokurz.nettylib.utils.NettyUtils;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.Getter;
import lombok.NonNull;

import javax.net.ssl.SSLException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Getter
public class NetworkClient implements AutoCloseable{

    protected final EventLoopGroup eventLoopGroup = new MultithreadEventLoopGroup(2, NettyUtils.createIoHandlerFactory());
    protected final QueryPacketManager queryPacketManager = new QueryPacketManager();
    protected final RoutingPacketManager routingPacketManager = new RoutingPacketManager();
    protected final PacketReceiverManager packetReceiverManager = new PacketReceiverManager();
    protected final PacketRegistry packetRegistry = new PacketRegistry();
    protected final ClientChannelTransmitter clientChannelTransmitter;
    protected final ChannelIdentity channelIdentity;
    protected final InactiveAction inactiveAction;
    protected final Codec codec;

    protected ConnectionState connectionState = ConnectionState.NOT_CONNECTED;
    protected SslContext sslCtx;
    protected Bootstrap bootstrap;

    public NetworkClient(
            @NonNull ChannelIdentity channelIdentity,
            @NonNull InactiveAction inactiveAction,
            @NonNull Codec codec,
            boolean ssl
    ) {
        this.codec = codec;
        this.clientChannelTransmitter = new ClientChannelTransmitter(this);
        this.channelIdentity = channelIdentity;
        this.inactiveAction = inactiveAction;

        if (ssl) {
            try {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public NetworkChannel thisNetworkChannel() {
        return this.clientChannelTransmitter.getNetworkChannel(this.channelIdentity.uniqueId());
    }

    public void connect(
            @NonNull String hostName,
            int port
    ) {

        this.bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channelFactory(NettyUtils.createChannelFactory())
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new NetworkClientChannelInitializer(this, hostName, port));

        var initThread = new InitThread(this.bootstrap, hostName, port);
        var thread = new Thread(initThread);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            NettyLib.debug(Level.SEVERE, this.getClass(), e.getMessage());
            return;
        }

        if (connectionState == ConnectionState.FAILED) {

            switch (inactiveAction) {
                case SHUTDOWN -> {
                    try {
                        this.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    System.exit(0);
                }
                case RETRY -> {
                    NettyLib.log(Level.INFO, ConsoleColor.RED.ansiCode() + "The connection will be tried again in 3 seconds.",
                            hostName, port);
                    Quala.sleepUninterruptedly(3000);
                    this.connect(hostName, port);
                }
            }

            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public void enableStayActive() {
        Scheduler.runtimeScheduler().schedule(() -> this.thisNetworkChannel().sendPacket(new NetworkChannelStayActivePacket(this.channelIdentity)), 60000, 60000);
    }

    @Override
    public void close() throws Exception {
        this.eventLoopGroup.shutdownGracefully();
    }

    @Getter
    public class InitThread implements Runnable {

        private final Bootstrap bootstrap;
        private final String host;
        private final int port;

        private Channel channel = null;

        public InitThread(Bootstrap bootstrap, String host, int port) {
            this.bootstrap = bootstrap;
            this.host = host;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                this.channel = bootstrap.connect(host, port)
                        .addListener(future -> {
                            if (future.isSuccess()) {
                                NetworkClient.this.connectionState = ConnectionState.CONNECTED;
                                NettyLib.log(Level.INFO, ConsoleColor.GREEN.ansiCode() + "Successfully connecting @" + host + ":" + port);
                            } else {
                                NetworkClient.this.connectionState = ConnectionState.FAILED;
                                NettyLib.log(Level.INFO, ConsoleColor.RED.ansiCode() + "Failed while connecting @" + host + ":" + port);
                            }
                        })
                        .asStage()
                        .get();
            } catch (InterruptedException | ExecutionException exception) {
                NettyLib.debug(Level.SEVERE, this.getClass(), exception.getMessage());
            }
        }
    }
}
