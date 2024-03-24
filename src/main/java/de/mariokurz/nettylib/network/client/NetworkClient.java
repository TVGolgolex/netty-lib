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
import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.network.ChannelIdentity;
import de.mariokurz.nettylib.network.channel.NetworkChannel;
import de.mariokurz.nettylib.network.protocol.query.QueryPacketManager;
import de.mariokurz.nettylib.network.protocol.receiver.PacketReceiverManager;
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
    protected final PacketReceiverManager packetReceiverManager = new PacketReceiverManager();
    protected final ClientChannelTransmitter clientChannelTransmitter;
    protected final ChannelIdentity channelIdentity;
    protected final InactiveAction inactiveAction;
    protected final NetworkChannel networkChannel;

    protected SslContext sslCtx;
    protected Bootstrap bootstrap;

    public NetworkClient(
            @NonNull ChannelIdentity channelIdentity,
            @NonNull InactiveAction inactiveAction,
            boolean ssl
    ) {
        this.clientChannelTransmitter = new ClientChannelTransmitter(this);
        this.channelIdentity = channelIdentity;
        this.inactiveAction = inactiveAction;
        this.networkChannel = new NetworkChannel(
                this.channelIdentity,
                new QueryPacketManager(),
                null,
                false
        );

        if (ssl) {
            try {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
        }
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

        if (initThread.channel() == null) {
            NettyLib.debug(Level.SEVERE, this.getClass(), ConsoleColor.RED.ansiCode() + "Channel is null");
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

    @Override
    public void close() throws Exception {
        this.eventLoopGroup.shutdownGracefully();
    }

    @Getter
    public static class InitThread implements Runnable {

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
                                NettyLib.log(Level.INFO, ConsoleColor.GREEN.ansiCode() + "Successfully connecting @" + host + ":" + port);
                            } else {
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
