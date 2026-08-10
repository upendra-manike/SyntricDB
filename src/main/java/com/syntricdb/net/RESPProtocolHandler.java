package com.syntricdb.net;

import com.syntricdb.engine.cache.MemoryCacheEngine;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Netty ChannelHandler implementing Redis Serialization Protocol (RESP2).
 * Allows redis-cli, Jedis, and ioredis clients to query SyntricDB memory cache directly on port 6379.
 */
public class RESPProtocolHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger log = LoggerFactory.getLogger(RESPProtocolHandler.class);

    private final MemoryCacheEngine cacheEngine;

    public RESPProtocolHandler(MemoryCacheEngine cacheEngine) {
        this.cacheEngine = cacheEngine;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String input = msg.toString(StandardCharsets.UTF_8).trim();
        if (input.isEmpty()) return;

        List<String> tokens = parseRESPCommand(input);
        if (tokens.isEmpty()) return;

        String cmd = tokens.get(0).toUpperCase();
        log.info("Received RESP Command: {} (Args: {})", cmd, tokens.size() - 1);

        switch (cmd) {
            case "PING":
                sendSimpleString(ctx, "PONG");
                break;

            case "SET":
                if (tokens.size() >= 3) {
                    String key = tokens.get(1);
                    String val = tokens.get(2);
                    cacheEngine.put(key, val);
                    sendSimpleString(ctx, "OK");
                } else {
                    sendError(ctx, "ERR wrong number of arguments for 'set' command");
                }
                break;

            case "GET":
                if (tokens.size() >= 2) {
                    Object val = cacheEngine.get(tokens.get(1));
                    if (val != null) {
                        sendBulkString(ctx, val.toString());
                    } else {
                        sendNullBulkString(ctx);
                    }
                } else {
                    sendError(ctx, "ERR wrong number of arguments for 'get' command");
                }
                break;

            case "DEL":
                if (tokens.size() >= 2) {
                    cacheEngine.invalidate(tokens.get(1));
                    sendInteger(ctx, 1);
                } else {
                    sendError(ctx, "ERR wrong number of arguments for 'del' command");
                }
                break;

            case "KEYS":
            case "DBSIZE":
                sendInteger(ctx, cacheEngine.size());
                break;

            default:
                sendSimpleString(ctx, "OK");
                break;
        }
    }

    private List<String> parseRESPCommand(String input) {
        List<String> tokens = new ArrayList<>();
        String[] lines = input.split("\r\n|[\r\n]");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("*") || line.startsWith("$")) continue;
            String[] parts = line.split("\\s+");
            Collections.addAll(tokens, parts);
        }
        return tokens;
    }

    private void sendSimpleString(ChannelHandlerContext ctx, String msg) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes(("+ " + msg + "\r\n").getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(buf);
    }

    private void sendError(ChannelHandlerContext ctx, String msg) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes(("- " + msg + "\r\n").getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(buf);
    }

    private void sendBulkString(ChannelHandlerContext ctx, String val) {
        byte[] bytes = val.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        buf.writeBytes(bytes);
        buf.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(buf);
    }

    private void sendNullBulkString(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes("$-1\r\n".getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(buf);
    }

    private void sendInteger(ChannelHandlerContext ctx, int num) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes((":" + num + "\r\n").getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(buf);
    }
}
