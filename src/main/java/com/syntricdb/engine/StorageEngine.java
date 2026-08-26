package com.syntricdb.engine;

import com.syntricdb.engine.cache.MemoryCacheEngine;
import com.syntricdb.engine.fulltext.InvertedIndex;
import com.syntricdb.engine.lsm.LSMTree;
import com.syntricdb.engine.schema.*;
import com.syntricdb.engine.stream.StreamEngine;
import com.syntricdb.engine.vector.HNSWIndex;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class StorageEngine implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(StorageEngine.class);

    public static final String DEFAULT_DB = "default";

    private final Path baseDataDir;
    private final Map<String, Database> databases = new ConcurrentHashMap<>();

    private final MemoryCacheEngine cacheEngine;
    private final StreamEngine streamEngine;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    private final AtomicLong writeOpsCount = new AtomicLong(0);
    private final AtomicLong readOpsCount = new AtomicLong(0);

    public StorageEngine(Path baseDataDir) {
        this.baseDataDir = baseDataDir;
        this.cacheEngine = new MemoryCacheEngine(50000);
        this.streamEngine = new StreamEngine();
        getOrCreateDatabase(DEFAULT_DB);
    }

    // --- DATABASE MANAGEMENT ---

    public synchronized Database createDatabase(String dbName) {
        if (dbName == null || dbName.isBlank()) {
            dbName = DEFAULT_DB;
        }
        String key = dbName.toLowerCase();
        if (databases.containsKey(key)) {
            throw new IllegalArgumentException("Database '" + dbName + "' already exists.");
        }
        Database db = new Database(key, baseDataDir.resolve("data"));
        databases.put(key, db);
        log.info("Database '{}' created successfully.", key);
        return db;
    }

    public synchronized Database getOrCreateDatabase(String dbName) {
        if (dbName == null || dbName.isBlank()) {
            dbName = DEFAULT_DB;
        }
        String key = dbName.toLowerCase();
        return databases.computeIfAbsent(key, k -> new Database(k, baseDataDir.resolve("data")));
    }

    public Database getDatabase(String dbName) {
        if (dbName == null || dbName.isBlank()) {
            dbName = DEFAULT_DB;
        }
        return databases.get(dbName.toLowerCase());
    }

    public synchronized void dropDatabase(String dbName) {
        if (dbName == null || dbName.isBlank()) return;
        String key = dbName.toLowerCase();
        if (DEFAULT_DB.equalsIgnoreCase(key)) {
            throw new IllegalArgumentException("Cannot drop default system database 'default'.");
        }
        Database db = databases.remove(key);
        if (db != null) {
            db.close();
            log.info("Database '{}' dropped successfully.", key);
        }
    }

    public List<String> listDatabases() {
        List<String> list = new ArrayList<>(databases.keySet());
        Collections.sort(list);
        return list;
    }

    // --- TABLE MANAGEMENT ---

    public synchronized void createTable(TableSchema schema) throws IOException {
        createTable(DEFAULT_DB, schema);
    }

    public synchronized void createTable(String dbName, TableSchema schema) throws IOException {
        Database db = getOrCreateDatabase(dbName);
        db.createTable(schema);
    }

    public void insert(String tableName, Tuple tuple) throws IOException {
        insert(DEFAULT_DB, tableName, tuple);
    }

    public void insert(String dbName, String tableName, Tuple tuple) throws IOException {
        Database db = getDatabase(dbName);
        if (db == null) {
            throw new IllegalArgumentException("Database '" + dbName + "' does not exist.");
        }

        tableName = tableName.toLowerCase();
        TableSchema schema = db.getSchema(tableName);
        if (schema == null) {
            throw new IllegalArgumentException("Table '" + tableName + "' does not exist in database '" + dbName + "'.");
        }

        String pkCol = schema.getPrimaryKeyColumn();
        if (pkCol == null) {
            throw new IllegalArgumentException("Table '" + tableName + "' must specify a primary key.");
        }

        Object pkVal = tuple.get(pkCol);
        if (pkVal == null) {
            throw new IllegalArgumentException("Primary key value for '" + pkCol + "' cannot be null.");
        }

        String keyStr = pkVal.toString();
        byte[] serializedBytes = jsonMapper.writeValueAsBytes(tuple.asMap());

        // 1. Write to LSM Tree (WAL + MemTable + SSTable)
        LSMTree lsm = db.getLsmTrees().get(tableName);
        if (lsm != null) {
            lsm.put(keyStr, serializedBytes);
        }

        // 2. Write to in-memory store
        db.getInMemoryTableStore().get(tableName).put(keyStr, tuple);

        // 3. Index vector columns in HNSW index
        String vectorCol = schema.getVectorColumn();
        if (vectorCol != null) {
            float[] vec = tuple.getVector(vectorCol);
            if (vec != null) {
                HNSWIndex hnsw = db.getVectorIndex(tableName, vectorCol);
                if (hnsw != null) {
                    hnsw.insert(keyStr, vec);
                }
            }
        }

        // 4. Index text columns in Inverted Index
        InvertedIndex invIdx = db.getInvertedIndexes().get(tableName);
        if (invIdx != null) {
            StringBuilder textAcc = new StringBuilder();
            for (ColumnDef col : schema.getColumnList()) {
                if (col.getType() == ColumnType.VARCHAR) {
                    String text = tuple.getString(col.getName());
                    if (text != null) textAcc.append(" ").append(text);
                }
            }
            if (textAcc.length() > 0) {
                invIdx.indexDocument(keyStr, textAcc.toString());
            }
        }

        // 5. Invalidate hot cache & record metric
        cacheEngine.invalidate(db.getName() + ":" + tableName + ":" + keyStr);
        writeOpsCount.incrementAndGet();

        // 6. Stream notification trigger
        Map<String, Object> streamEvent = new HashMap<>(tuple.asMap());
        streamEvent.put("_db", db.getName());
        streamEvent.put("_table", tableName);
        streamEvent.put("_op", "INSERT");
        streamEngine.publish("table_" + db.getName() + "_" + tableName, streamEvent);
    }

    public int update(String dbName, String tableName, Map<String, Object> setAssignments, List<com.syntricdb.sql.AST.Condition> conditions) throws IOException {
        Database db = getDatabase(dbName);
        if (db == null) return 0;
        tableName = tableName.toLowerCase();
        TableSchema schema = db.getSchema(tableName);
        if (schema == null) return 0;

        List<Tuple> matches = scanAll(dbName, tableName);
        int count = 0;
        for (Tuple existing : matches) {
            if (matchesConditions(existing, conditions)) {
                for (Map.Entry<String, Object> entry : setAssignments.entrySet()) {
                    existing.set(entry.getKey(), entry.getValue());
                }
                insert(dbName, tableName, existing);
                count++;
            }
        }
        return count;
    }

    public int delete(String dbName, String tableName, List<com.syntricdb.sql.AST.Condition> conditions) throws IOException {
        Database db = getDatabase(dbName);
        if (db == null) return 0;
        tableName = tableName.toLowerCase();
        TableSchema schema = db.getSchema(tableName);
        if (schema == null) return 0;

        String pkCol = schema.getPrimaryKeyColumn();
        List<Tuple> matches = scanAll(dbName, tableName);
        int count = 0;
        Map<String, Tuple> store = db.getInMemoryTableStore().get(tableName);
        InvertedIndex invIdx = db.getInvertedIndexes().get(tableName);
        String vectorCol = schema.getVectorColumn();

        for (Tuple existing : matches) {
            if (matchesConditions(existing, conditions)) {
                String pkVal = existing.get(pkCol) != null ? existing.get(pkCol).toString() : null;
                if (pkVal != null) {
                    if (store != null) store.remove(pkVal);
                    if (invIdx != null) invIdx.removeDocument(pkVal);
                    if (vectorCol != null) {
                        HNSWIndex hnsw = db.getVectorIndex(tableName, vectorCol);
                        if (hnsw != null) hnsw.remove(pkVal);
                    }
                    LSMTree lsm = db.getLsmTrees().get(tableName);
                    if (lsm != null) lsm.delete(pkVal);
                    cacheEngine.invalidate(db.getName() + ":" + tableName + ":" + pkVal);
                    writeOpsCount.incrementAndGet();
                    count++;
                }
            }
        }
        return count;
    }

    private boolean matchesConditions(Tuple tuple, List<com.syntricdb.sql.AST.Condition> conditions) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (com.syntricdb.sql.AST.Condition cond : conditions) {
            Object val = tuple.get(cond.getColumn());
            if (val == null) return false;
            String op = cond.getOperator();
            Object target = cond.getValue();
            if ("=".equals(op) && !val.toString().equals(target.toString())) return false;
            if ("!=".equals(op) && !val.toString().equals(target.toString())) return false;
            if (val instanceof Number && target instanceof Number) {
                double v = ((Number) val).doubleValue();
                double t = ((Number) target).doubleValue();
                if (">".equals(op) && v <= t) return false;
                if ("<".equals(op) && v >= t) return false;
                if (">=".equals(op) && v < t) return false;
                if ("<=".equals(op) && v > t) return false;
            }
        }
        return true;
    }


    public Tuple getByPrimaryKey(String tableName, String primaryKey) throws IOException {
        return getByPrimaryKey(DEFAULT_DB, tableName, primaryKey);
    }

    public Tuple getByPrimaryKey(String dbName, String tableName, String primaryKey) throws IOException {
        Database db = getDatabase(dbName);
        if (db == null) return null;

        tableName = tableName.toLowerCase();
        readOpsCount.incrementAndGet();

        String cacheKey = db.getName() + ":" + tableName + ":" + primaryKey;
        Object cached = cacheEngine.get(cacheKey);
        if (cached instanceof Tuple) {
            return (Tuple) cached;
        }

        Map<String, Tuple> store = db.getInMemoryTableStore().get(tableName);
        if (store != null && store.containsKey(primaryKey)) {
            Tuple tuple = store.get(primaryKey);
            cacheEngine.put(cacheKey, tuple);
            return tuple;
        }

        LSMTree lsm = db.getLsmTrees().get(tableName);
        if (lsm != null) {
            byte[] bytes = lsm.get(primaryKey);
            if (bytes != null) {
                Map<String, Object> map = jsonMapper.readValue(bytes, Map.class);
                Tuple tuple = new Tuple(map);
                cacheEngine.put(cacheKey, tuple);
                return tuple;
            }
        }

        return null;
    }

    public List<Tuple> scanAll(String tableName) {
        return scanAll(DEFAULT_DB, tableName);
    }

    public List<Tuple> scanAll(String dbName, String tableName) {
        Database db = getDatabase(dbName);
        if (db == null) return Collections.emptyList();

        tableName = tableName.toLowerCase();
        readOpsCount.incrementAndGet();
        Map<String, Tuple> store = db.getInMemoryTableStore().get(tableName);
        if (store == null) return Collections.emptyList();
        return new ArrayList<>(store.values());
    }

    public TableSchema getSchema(String tableName) {
        return getSchema(DEFAULT_DB, tableName);
    }

    public TableSchema getSchema(String dbName, String tableName) {
        Database db = getDatabase(dbName);
        if (db == null) return null;
        return db.getSchema(tableName);
    }

    public Map<String, TableSchema> getAllSchemas() {
        return getAllSchemas(DEFAULT_DB);
    }

    public Map<String, TableSchema> getAllSchemas(String dbName) {
        Database db = getDatabase(dbName);
        if (db == null) return Collections.emptyMap();
        return db.getSchemas();
    }

    public HNSWIndex getVectorIndex(String tableName, String columnName) {
        return getVectorIndex(DEFAULT_DB, tableName, columnName);
    }

    public HNSWIndex getVectorIndex(String dbName, String tableName, String columnName) {
        Database db = getDatabase(dbName);
        if (db == null) return null;
        return db.getVectorIndex(tableName, columnName);
    }

    public InvertedIndex getInvertedIndex(String tableName) {
        return getInvertedIndex(DEFAULT_DB, tableName);
    }

    public InvertedIndex getInvertedIndex(String dbName, String tableName) {
        Database db = getDatabase(dbName);
        if (db == null) return null;
        return db.getInvertedIndex(tableName);
    }

    public MemoryCacheEngine getCacheEngine() {
        return cacheEngine;
    }

    public StreamEngine getStreamEngine() {
        return streamEngine;
    }

    public long getWriteOpsCount() { return writeOpsCount.get(); }
    public long getReadOpsCount() { return readOpsCount.get(); }

    @Override
    public void close() throws Exception {
        for (Database db : databases.values()) {
            db.close();
        }
    }
}
