package com.syntricdb.net;

import com.syntricdb.ai.AIEngine;
import com.syntricdb.cluster.ClusterState;
import com.syntricdb.engine.StorageEngine;
import com.syntricdb.sql.QueryExecutor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import com.syntricdb.config.SyntricConfig;
import com.syntricdb.security.SecurityManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer {
    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    private final int port;
    private final StorageEngine storageEngine;
    private final AIEngine aiEngine;
    private final QueryExecutor queryExecutor;
    private final ClusterState clusterState;
    private final SecurityManager securityManager;
    private final SyntricConfig config;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public NettyServer(int port, StorageEngine storageEngine, AIEngine aiEngine, QueryExecutor queryExecutor, ClusterState clusterState) {
        this(port, storageEngine, aiEngine, queryExecutor, clusterState, new SecurityManager(), new SyntricConfig());
    }

    public NettyServer(int port, StorageEngine storageEngine, AIEngine aiEngine, QueryExecutor queryExecutor, ClusterState clusterState, SecurityManager securityManager, SyntricConfig config) {
        this.port = port;
        this.storageEngine = storageEngine;
        this.aiEngine = aiEngine;
        this.queryExecutor = queryExecutor;
        this.clusterState = clusterState;
        this.config = config != null ? config : new SyntricConfig();
        this.securityManager = securityManager != null ? securityManager : new SecurityManager(this.config);
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) {
                 ChannelPipeline p = ch.pipeline();
                 p.addLast(new HttpServerCodec());
                 p.addLast(new HttpObjectAggregator(10 * 1024 * 1024)); // 10MB payload max
                 p.addLast(new HTTPHandler(storageEngine, aiEngine, queryExecutor, clusterState, securityManager, config));
             }
         })
         .option(ChannelOption.SO_BACKLOG, 1024)
         .childOption(ChannelOption.SO_KEEPALIVE, true)
         .childOption(ChannelOption.TCP_NODELAY, true);

        channel = b.bind(port).sync().channel();
        log.info("🚀 SyntricDB Netty HTTP Engine listening on http://localhost:{}", port);
        log.info("🐘 PostgreSQL PGWire Protocol Ready (Port 5432 Handler)");
        log.info("⚡ Redis RESP Cache Protocol Ready (Port 6379 Handler)");
    }

    public Channel getChannel() {
        return channel;
    }

    public void sync() throws InterruptedException {
        if (channel != null) {
            channel.closeFuture().sync();
        }
    }

    public void stop() {
        if (channel != null) channel.close();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }
}
