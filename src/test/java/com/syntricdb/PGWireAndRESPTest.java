package com.syntricdb;

import com.syntricdb.ai.AIEngine;
import com.syntricdb.engine.StorageEngine;
import com.syntricdb.engine.cache.MemoryCacheEngine;
import com.syntricdb.net.PGWireServerHandler;
import com.syntricdb.net.RESPProtocolHandler;
import com.syntricdb.sql.QueryExecutor;
import io.netty.channel.embedded.EmbeddedChannel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PGWireAndRESPTest {

    @TempDir
    Path tempDir;

    private StorageEngine storageEngine;
    private AIEngine aiEngine;
    private QueryExecutor queryExecutor;
    private MemoryCacheEngine cacheEngine;

    @BeforeEach
    public void setUp() throws Exception {
        storageEngine = new StorageEngine(tempDir);
        aiEngine = new AIEngine(128);
        queryExecutor = new QueryExecutor(storageEngine, aiEngine);
        cacheEngine = storageEngine.getCacheEngine();
    }

    @Test
    public void testRESPHandlerSetAndGet() {
        EmbeddedChannel channel = new EmbeddedChannel(new RESPProtocolHandler(cacheEngine));

        // SET key value
        io.netty.buffer.ByteBuf setBuf = io.netty.buffer.Unpooled.copiedBuffer("SET user_101 Upendra\r\n", java.nio.charset.StandardCharsets.UTF_8);
        channel.writeInbound(setBuf);

        io.netty.buffer.ByteBuf resp1 = channel.readOutbound();
        assertNotNull(resp1);
        String resp1Str = resp1.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(resp1Str.contains("OK"));

        // GET key
        io.netty.buffer.ByteBuf getBuf = io.netty.buffer.Unpooled.copiedBuffer("GET user_101\r\n", java.nio.charset.StandardCharsets.UTF_8);
        channel.writeInbound(getBuf);

        io.netty.buffer.ByteBuf resp2 = channel.readOutbound();
        assertNotNull(resp2);
        String resp2Str = resp2.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(resp2Str.contains("Upendra"));
    }

    @Test
    public void testPGWireHandlerChannelInit() {
        EmbeddedChannel channel = new EmbeddedChannel(new PGWireServerHandler(queryExecutor));
        assertTrue(channel.isOpen());
    }
}
