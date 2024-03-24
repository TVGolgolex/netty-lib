package de.mariokurz.nettylib.network.server;

/*
 * MIT License
 *
 * Copyright (c) 2024 23:05 Mario Pascal K.
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
import de.mariokurz.nettylib.utils.NettyUtils;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.util.SelfSignedCertificate;
import lombok.Getter;
import lombok.NonNull;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Getter
public class NetworkServer implements AutoCloseable{

    protected final EventLoopGroup bossEventLoopGroup = new MultithreadEventLoopGroup(1, NettyUtils.createIoHandlerFactory());
    protected final EventLoopGroup workerEventLoopGroup = new MultithreadEventLoopGroup(1, NettyUtils.createIoHandlerFactory());
    protected final ServerChannelTransmitter serverChannelTransmitter;

    protected ServerBootstrap serverBootstrap;
    protected SslContext sslCtx;

    public NetworkServer(
            boolean ssl
    ) {
        this.serverChannelTransmitter = new ServerChannelTransmitter();
        if (ssl) {
            try {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder
                        .forServer(ssc.certificate(), ssc.privateKey())
                        .build();
            } catch (SSLException | CertificateException e) {
                throw new RuntimeException(e);
            }
        }
//        URLConnection.setDefaultUseCaches("tcp", false);
    }

    public void connect(
            @NonNull String hostName,
            int port
    ) {

        this.serverBootstrap = new ServerBootstrap()
                .group(bossEventLoopGroup, workerEventLoopGroup)
                .channelFactory(NettyUtils.createServerChannelFactory())
                .childHandler(new NetworkServerChannelInitializer(this, hostName, port))
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.AUTO_READ, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        var channelFuture = serverBootstrap
                .bind(
                        hostName,
                        port)
                .addListener(future -> {
                    if (future.isSuccess()) {
                        NettyLib.log(Level.INFO, ConsoleColor.GREEN.ansiCode() + "Opened network listener @" + hostName + ":" + port);
                    } else {
                        NettyLib.log(Level.INFO, ConsoleColor.RED.ansiCode() + "Failed while opening network listener @" + hostName + ":" + port);
                    }
                });

        if (channelFuture.isFailed()) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        new Thread(() -> {
            try {
                channelFuture.asStage().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void close() throws Exception {
        bossEventLoopGroup.shutdownGracefully();
        workerEventLoopGroup.shutdownGracefully();
    }
}
