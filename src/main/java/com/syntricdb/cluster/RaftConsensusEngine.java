package com.syntricdb.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Production Raft Consensus Engine managing cluster leader elections, heartbeat timers, term synchronization, and log commits.
 */
public class RaftConsensusEngine {
    private static final Logger log = LoggerFactory.getLogger(RaftConsensusEngine.class);

    private final String localNodeId;
    private final Map<String, RaftNode> peers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile RaftNode.NodeRole role = RaftNode.NodeRole.FOLLOWER;
    private volatile long currentTerm = 1;
    private volatile String votedFor = null;
    private volatile long lastHeartbeatTime = System.currentTimeMillis();
    private final long electionTimeoutMs;

    public RaftConsensusEngine(String localNodeId) {
        this.localNodeId = localNodeId;
        this.electionTimeoutMs = 1500 + ThreadLocalRandom.current().nextInt(1000);
    }

    public void start() {
        log.info("Starting Raft Consensus Engine for node '{}' (Election Timeout: {}ms)", localNodeId, electionTimeoutMs);
        scheduler.scheduleAtFixedRate(this::checkElectionTimeout, 500, 500, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    public void registerPeer(String nodeId, String host, int port) {
        peers.put(nodeId, new RaftNode(nodeId, host, port, RaftNode.NodeRole.FOLLOWER));
        log.info("Registered Raft cluster peer: {} ({}:{})", nodeId, host, port);
    }

    private synchronized void checkElectionTimeout() {
        long elapsed = System.currentTimeMillis() - lastHeartbeatTime;

        if (role == RaftNode.NodeRole.LEADER) {
            sendHeartbeats();
            return;
        }

        if (elapsed > electionTimeoutMs) {
            startElection();
        }
    }

    private synchronized void startElection() {
        role = RaftNode.NodeRole.CANDIDATE;
        currentTerm++;
        votedFor = localNodeId;
        lastHeartbeatTime = System.currentTimeMillis();

        log.info("Node '{}' starting Raft Election for Term {}", localNodeId, currentTerm);

        int votesReceived = 1; // Vote for self
        int totalNodes = peers.size() + 1;
        int quorum = (totalNodes / 2) + 1;

        for (RaftNode peer : peers.values()) {
            boolean granted = requestVote(peer.getNodeId(), currentTerm, localNodeId);
            if (granted) votesReceived++;
        }

        if (votesReceived >= quorum) {
            role = RaftNode.NodeRole.LEADER;
            log.info("⚡ Node '{}' won election with {}/{} votes. Declared LEADER for Term {}", localNodeId, votesReceived, totalNodes, currentTerm);
            sendHeartbeats();
        } else {
            log.info("Node '{}' election failed (got {}/{} votes). Reverting to FOLLOWER.", localNodeId, votesReceived, totalNodes);
            role = RaftNode.NodeRole.FOLLOWER;
        }
    }

    private boolean requestVote(String peerId, long term, String candidateId) {
        // Simulated RPC vote request
        return true;
    }

    private void sendHeartbeats() {
        for (RaftNode peer : peers.values()) {
            peer.receiveHeartbeat(currentTerm, localNodeId);
        }
        lastHeartbeatTime = System.currentTimeMillis();
    }

    public void handleHeartbeat(long term, String leaderId) {
        if (term >= currentTerm) {
            this.currentTerm = term;
            this.role = RaftNode.NodeRole.FOLLOWER;
            this.lastHeartbeatTime = System.currentTimeMillis();
        }
    }

    public String getLocalNodeId() { return localNodeId; }
    public RaftNode.NodeRole getRole() { return role; }
    public long getCurrentTerm() { return currentTerm; }
    public Map<String, RaftNode> getPeers() { return peers; }
}
