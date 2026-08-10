package com.syntricdb;

import com.syntricdb.cluster.RaftConsensusEngine;
import com.syntricdb.engine.stream.CDCEventStream;
import com.syntricdb.engine.schema.Tuple;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

public class RaftConsensusAndCDCTest {

    @Test
    public void testRaftConsensusInitialization() {
        RaftConsensusEngine raft = new RaftConsensusEngine("node_1");
        raft.registerPeer("node_2", "127.0.0.1", 8081);
        raft.registerPeer("node_3", "127.0.0.1", 8082);

        assertEquals("node_1", raft.getLocalNodeId());
        assertEquals(2, raft.getPeers().size());

        raft.start();
        assertTrue(raft.getCurrentTerm() >= 1);
        raft.stop();
    }

    @Test
    public void testCDCEventPublishing() {
        CDCEventStream cdc = new CDCEventStream();
        AtomicInteger eventCounter = new AtomicInteger(0);

        cdc.registerListener(event -> {
            eventCounter.incrementAndGet();
            assertEquals("default", event.getDatabase());
            assertEquals("users", event.getTable());
            assertEquals("usr_99", event.getKey());
        });

        Tuple tuple = new Tuple();
        tuple.set("name", "Testing CDC");

        cdc.publishEvent("default", "users", CDCEventStream.EventType.INSERT, "usr_99", tuple);

        assertEquals(1, eventCounter.get());
    }
}
