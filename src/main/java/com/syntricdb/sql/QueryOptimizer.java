package com.syntricdb.sql;

import com.syntricdb.engine.StorageEngine;
import com.syntricdb.engine.schema.TableSchema;

public class QueryOptimizer {
    private final StorageEngine storageEngine;

    public QueryOptimizer(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
    }

    public ExecutionPlan optimize(AST.SelectStatement stmt) {
        return optimize(StorageEngine.DEFAULT_DB, stmt);
    }

    public ExecutionPlan optimize(String dbName, AST.SelectStatement stmt) {
        String tableName = stmt.getTableName();
        String targetDb = dbName;
        if (tableName == null) {
            return new ExecutionPlan(ExecutionPlan.ExecutionStrategy.FULL_TABLE_SCAN, "Function Execution", 0.0, stmt);
        }
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            targetDb = parts[0];
            tableName = parts[1];
        }

        TableSchema schema = storageEngine.getSchema(targetDb, tableName);
        if (schema == null) {
            throw new IllegalArgumentException("Table '" + tableName + "' does not exist in database '" + targetDb + "'.");
        }

        // 1. Check if query is vector similarity search
        if (stmt.getVectorSearchCondition() != null) {
            AST.VectorSearchCondition vecCond = stmt.getVectorSearchCondition();
            return new ExecutionPlan(
                ExecutionPlan.ExecutionStrategy.INDEX_VECTOR_HNSW,
                "HNSW Vector Index Scan on column '" + vecCond.getVectorColumn() + "' (Dimension=" + schema.getColumn(vecCond.getVectorColumn()).getVectorDimension() + ", BeamWidth=64)",
                1.5,
                stmt
            );
        }

        // 2. Check if full-text inverted index search
        if (stmt.getFullTextCondition() != null) {
            return new ExecutionPlan(
                ExecutionPlan.ExecutionStrategy.INDEX_INVERTED_FULLTEXT,
                "Inverted BM25 Index Search for query '" + stmt.getFullTextCondition().getQueryText() + "'",
                2.0,
                stmt
            );
        }

        // 3. Check for primary key point lookup
        String pkCol = schema.getPrimaryKeyColumn();
        if (pkCol != null) {
            for (AST.Condition cond : stmt.getWhereConditions()) {
                if (pkCol.equalsIgnoreCase(cond.getColumn()) && "=".equals(cond.getOperator())) {
                    return new ExecutionPlan(
                        ExecutionPlan.ExecutionStrategy.INDEX_PRIMARY_KEY,
                        "LSM Primary Key Index Point Lookup on key='" + cond.getValue() + "'",
                        0.1,
                        stmt
                    );
                }
            }
        }

        // 4. Fallback: Full Table Scan with Predicate Pushdown
        return new ExecutionPlan(
            ExecutionPlan.ExecutionStrategy.FULL_TABLE_SCAN,
            "Full Table Scan with Predicate Pushdown filter",
            10.0,
            stmt
        );
    }
}
