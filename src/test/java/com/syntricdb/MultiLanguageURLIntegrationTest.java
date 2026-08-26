package com.syntricdb;

import com.syntricdb.ai.AIEngine;
import com.syntricdb.config.SyntricConfig;
import com.syntricdb.engine.StorageEngine;
import com.syntricdb.net.NettyServer;
import com.syntricdb.security.SecurityManager;
import com.syntricdb.sql.QueryExecutor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class MultiLanguageURLIntegrationTest {

    @TempDir
    Path tempDir;

    private NettyServer nettyServer;
    private StorageEngine storageEngine;
    private QueryExecutor queryExecutor;

    @BeforeEach
    public void setUp() throws Exception {
        storageEngine = new StorageEngine(tempDir);
        AIEngine aiEngine = new AIEngine(128);
        queryExecutor = new QueryExecutor(storageEngine, aiEngine);

        SyntricConfig config = new SyntricConfig();
        SecurityManager securityManager = new SecurityManager("admin", "syntricdb_secret_pass");
        nettyServer = new NettyServer(8898, storageEngine, aiEngine, queryExecutor, new com.syntricdb.cluster.ClusterState(), securityManager, config);
        nettyServer.start();
    }

    @AfterEach
    public void tearDown() {
        if (nettyServer != null) {
            nettyServer.stop();
        }
    }

    @Test
    public void testJavaNativeDriverURL() throws Exception {
        String connUrl = "jdbc:syntricdb://admin:syntricdb_secret_pass@localhost:8898/default";
        try (Connection conn = DriverManager.getConnection(connUrl);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE java_test (id VARCHAR PRIMARY KEY, val INT)");
            stmt.execute("INSERT INTO java_test VALUES ('j1', 100)");
            
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM java_test")) {
                assertTrue(rs.next());
                assertEquals("j1", rs.getString("id"));
                assertEquals(100, rs.getInt("val"));
            }
        }
    }

    @Test
    public void testPythonClientURL() throws Exception {
        String script = 
            "import sys\n" +
            "sys.path.insert(0, 'deploy/clients/python')\n" +
            "from syntricdb.client import SyntricDBClient\n" +
            "client = SyntricDBClient('syntricdb://admin:syntricdb_secret_pass@localhost:8898/default')\n" +
            "client.query('CREATE TABLE py_test (id VARCHAR PRIMARY KEY, msg VARCHAR)')\n" +
            "res = client.query(\"INSERT INTO py_test VALUES ('p1', 'hello_python')\")\n" +
            "res_sel = client.query('SELECT * FROM py_test')\n" +
            "assert res_sel['data'][0]['msg'] == 'hello_python'\n" +
            "print('PYTHON_URL_TEST_SUCCESS')\n";

        Process proc = new ProcessBuilder("python3", "-c", script).start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line = reader.readLine();
        int exitCode = proc.waitFor();
        
        assertEquals(0, exitCode, "Python script failed");
        assertEquals("PYTHON_URL_TEST_SUCCESS", line);
    }

    @Test
    public void testNodeJSClientURL() throws Exception {
        String script = 
            "const { SyntricDBClient } = require('./deploy/clients/nodejs/index.js');\n" +
            "(async () => {\n" +
            "  try {\n" +
            "    const client = new SyntricDBClient('syntricdb://admin:syntricdb_secret_pass@localhost:8898/default');\n" +
            "    await client.query('CREATE TABLE node_test (id VARCHAR PRIMARY KEY, num INT)');\n" +
            "    await client.query(\"INSERT INTO node_test VALUES ('n1', 200)\");\n" +
            "    const sel = await client.query('SELECT * FROM node_test');\n" +
            "    if (sel && sel.data && sel.data.length > 0) {\n" +
            "      console.log('NODEJS_URL_TEST_SUCCESS');\n" +
            "    }\n" +
            "  } catch (e) {\n" +
            "    console.error(e);\n" +
            "  }\n" +
            "})();\n";

        Process proc = new ProcessBuilder("node", "-e", script).start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line = reader.readLine();
        int exitCode = proc.waitFor();
        
        assertEquals(0, exitCode, "Node.js script failed");
        assertEquals("NODEJS_URL_TEST_SUCCESS", line);
    }

    @Test
    public void testCurlClientURL() throws Exception {
        Process proc = new ProcessBuilder("curl", "-s", "-u", "admin:syntricdb_secret_pass", "-X", "POST", "http://localhost:8898/api/sql",
                "-H", "Content-Type: application/json", "-d", "{\"database\": \"default\", \"sql\": \"CREATE TABLE curl_test (id VARCHAR PRIMARY KEY)\"}").start();
        int exitCode = proc.waitFor();
        assertEquals(0, exitCode, "cURL command failed");
    }
}
