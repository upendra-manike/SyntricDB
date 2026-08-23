package com.syntricdb;

import com.syntricdb.ai.AIEngine;
import com.syntricdb.cli.SyntricCLI;
import com.syntricdb.cluster.ClusterState;
import com.syntricdb.engine.StorageEngine;
import com.syntricdb.engine.schema.Tuple;
import com.syntricdb.net.NettyServer;
import com.syntricdb.sql.QueryExecutor;

import com.syntricdb.config.SyntricConfig;
import com.syntricdb.security.SecurityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SyntricDBServer {
    private static final Logger log = LoggerFactory.getLogger(SyntricDBServer.class);

    private final SyntricConfig config;
    private final SecurityManager securityManager;
    private final StorageEngine storageEngine;
    private final AIEngine aiEngine;
    private final QueryExecutor queryExecutor;
    private final ClusterState clusterState;
    private final NettyServer nettyServer;

    public SyntricDBServer(int port, Path dataDir) throws Exception {
        this.config = new SyntricConfig();
        this.securityManager = new SecurityManager(config);
        this.storageEngine = new StorageEngine(dataDir);
        this.aiEngine = new AIEngine(128);
        this.queryExecutor = new QueryExecutor(storageEngine, aiEngine);
        this.clusterState = new ClusterState();
        this.nettyServer = new NettyServer(port, storageEngine, aiEngine, queryExecutor, clusterState, securityManager, config);
    }

    public void start(boolean startCli) throws Exception {
        log.info("Starting SyntricDB AI-Native Database Server...");

        // 1. Initialize Default Database & Schema
        queryExecutor.execute("CREATE TABLE users (id VARCHAR PRIMARY KEY, name VARCHAR, city VARCHAR, age INT, role VARCHAR, bio VARCHAR, embedding FLOAT_VECTOR(128))");

        // 2. Initialize Sample Secondary Database 'production'
        queryExecutor.execute("CREATE DATABASE production");
        queryExecutor.execute("CREATE TABLE production.products (id VARCHAR PRIMARY KEY, title VARCHAR, category VARCHAR, price DOUBLE, embedding FLOAT_VECTOR(128))");

        // 3. Seed Initial Sample Data with Embeddings & AI Functions
        seedSampleData();

        // 3. Start Netty HTTP API Server & Web Management Console
        nettyServer.start();

        log.info("==========================================================================");
        log.info("⚡ SyntricDB Next-Gen Unified AI-Native Engine is Ready ⚡");
        log.info("🔑 Configured Admin User : {}", config.getAdminUser());
        log.info("🌐 Web Console & Studio  : http://localhost:{}/", config.getPort());
        log.info("📡 REST Query API        : POST http://localhost:{}/api/sql", config.getPort());
        log.info("🔍 HNSW Vector API       : POST http://localhost:{}/api/vector/search", config.getPort());
        log.info("==========================================================================");

        if (startCli) {
            SyntricCLI cli = new SyntricCLI(queryExecutor);
            cli.startInteractiveRepl();
        } else {
            Thread.currentThread().join();
        }
    }

    private void seedSampleData() throws Exception {
        log.info("Seeding initial AI-native data and vectors...");

        Object[][] usersData = new Object[][]{
            {"usr_101", "Upendra Kumar", "Hyderabad", 29, "Principal AI & Systems Architect", "Specializes in high throughput distributed databases, Raft consensus, Netty, Java 21, and vector embeddings.", "Java Engineer"},
            {"usr_102", "Ananya Sharma", "Hyderabad", 31, "Lead Systems Engineer", "Passionate about low-latency LSM Trees, memory caching, RocksDB, Rust, and streaming data pipelines.", "Java Engineer"},
            {"usr_103", "Rahul Verma", "Bengaluru", 34, "Senior AI Researcher", "Focused on HNSW graphs, vector search, LLM retrieval augmented generation (RAG), and neural semantic search.", "AI Researcher"},
            {"usr_104", "Sophia Chen", "San Francisco", 28, "Distributed Systems Engineer", "Building high availability cloud databases, horizontal sharding, Kubernetes operators, and Raft replication.", "Cloud Database Specialist"},
            {"usr_105", "Vikram Malhotra", "Hyderabad", 32, "Staff Data Platform Engineer", "Expert in DuckDB, columnar storage format Apache Arrow, query optimizers, and high write throughput.", "Java Engineer"},
            {"usr_106", "Elena Rostova", "London", 30, "AI Infrastructure Engineer", "Specializes in deep learning inference optimization, vector indexing, GPU caching, and full-text search.", "AI Researcher"}
        };

        for (Object[] u : usersData) {
            Tuple tuple = new Tuple();
            tuple.set("id", u[0]);
            tuple.set("name", u[1]);
            tuple.set("city", u[2]);
            tuple.set("age", u[3]);
            tuple.set("role", u[4]);
            tuple.set("bio", u[5]);
            tuple.set("embedding", aiEngine.aiEmbed(u[6].toString()));
            storageEngine.insert("default", "users", tuple);
        }

        // Seed products into 'production' database
        Object[][] productsData = new Object[][]{
            {"prod_01", "SyntricDB Enterprise Cluster", "Database", 2999.0, "AI-Native Vector Database"},
            {"prod_02", "Neural Embedder Accelerator", "AI Hardware", 1499.0, "High-Speed Vector Embedding Unit"},
            {"prod_03", "Raft Consensus Inspector", "Developer Tool", 499.0, "Distributed State Inspector"}
        };

        for (Object[] p : productsData) {
            Tuple tuple = new Tuple();
            tuple.set("id", p[0]);
            tuple.set("title", p[1]);
            tuple.set("category", p[2]);
            tuple.set("price", p[3]);
            tuple.set("embedding", aiEngine.aiEmbed(p[4].toString()));
            storageEngine.insert("production", "products", tuple);
        }

        // Seed stream event topic
        queryExecutor.execute("PUBLISH INTO system_events VALUES {\"event\": \"CLUSTER_INITIALIZED\", \"status\": \"HEALTHY\"}");
    }

    public static void main(String[] args) {
        try {
            SyntricConfig config = new SyntricConfig();
            int port = config.getPort();
            boolean cli = args.length > 0 && "--cli".equalsIgnoreCase(args[0]);
            Path dataDir = Paths.get(config.getDataDir());

            SyntricDBServer server = new SyntricDBServer(port, dataDir);
            server.start(cli);
        } catch (Exception e) {
            log.error("Fatal error starting SyntricDB Server", e);
            System.exit(1);
        }
    }
}
