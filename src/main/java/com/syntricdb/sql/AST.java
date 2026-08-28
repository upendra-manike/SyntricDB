package com.syntricdb.sql;

import com.syntricdb.engine.schema.ColumnDef;
import com.syntricdb.engine.schema.Tuple;
import java.util.*;

public class AST {

    public interface Statement {}

    public static class SetStatement implements Statement {
        private final String expression;
        public SetStatement(String expression) { this.expression = expression; }
        public String getExpression() { return expression; }
    }

    public static class NoOpStatement implements Statement {}

    public static class CreateTableStatement implements Statement {
        private final String tableName;
        private final List<ColumnDef> columns = new ArrayList<>();

        public CreateTableStatement(String tableName) {
            this.tableName = tableName.toLowerCase();
        }

        public CreateTableStatement addColumn(ColumnDef col) {
            columns.add(col);
            return this;
        }

        public String getTableName() { return tableName; }
        public List<ColumnDef> getColumns() { return columns; }
    }

    public static class InsertStatement implements Statement {
        private final String tableName;
        private final Tuple tuple;

        public InsertStatement(String tableName, Tuple tuple) {
            this.tableName = tableName.toLowerCase();
            this.tuple = tuple;
        }

        public String getTableName() { return tableName; }
        public Tuple getTuple() { return tuple; }
    }

    public static class SelectStatement implements Statement {
        private final String tableName;
        private final List<SelectItem> selectItems = new ArrayList<>();
        private final List<Condition> whereConditions = new ArrayList<>();
        private VectorSearchCondition vectorSearchCondition;
        private FullTextCondition fullTextCondition;
        private String orderByColumn;
        private boolean orderByDesc = false;
        private int limit = -1;

        public SelectStatement(String tableName) {
            this.tableName = tableName != null ? tableName.toLowerCase() : null;
        }

        public String getTableName() { return tableName; }
        public List<SelectItem> getSelectItems() { return selectItems; }
        public List<Condition> getWhereConditions() { return whereConditions; }
        public VectorSearchCondition getVectorSearchCondition() { return vectorSearchCondition; }
        public void setVectorSearchCondition(VectorSearchCondition v) { this.vectorSearchCondition = v; }
        public FullTextCondition getFullTextCondition() { return fullTextCondition; }
        public void setFullTextCondition(FullTextCondition f) { this.fullTextCondition = f; }
        public String getOrderByColumn() { return orderByColumn; }
        public void setOrderByColumn(String col) { this.orderByColumn = col; }
        public boolean isOrderByDesc() { return orderByDesc; }
        public void setOrderByDesc(boolean desc) { this.orderByDesc = desc; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }

    public static class SelectItem {
        private final String columnName;
        private final String alias;
        private final String aiFunction; // e.g., AI_SUMMARIZE, AI_CLASSIFY
        private final String[] aiArgs;

        public SelectItem(String columnName) {
            this(columnName, null, null, null);
        }

        public SelectItem(String columnName, String alias, String aiFunction, String[] aiArgs) {
            this.columnName = columnName;
            this.alias = alias != null ? alias : (aiFunction != null ? aiFunction.toLowerCase() + "_" + columnName : columnName);
            this.aiFunction = aiFunction;
            this.aiArgs = aiArgs;
        }

        public String getColumnName() { return columnName; }
        public String getAlias() { return alias; }
        public String getAiFunction() { return aiFunction; }
        public String[] getAiArgs() { return aiArgs; }
    }

    public static class Condition {
        private final String column;
        private final String operator; // =, !=, >, <, >=, <=
        private final Object value;

        public Condition(String column, String operator, Object value) {
            this.column = column.toLowerCase();
            this.operator = operator;
            this.value = value;
        }

        public String getColumn() { return column; }
        public String getOperator() { return operator; }
        public Object getValue() { return value; }
    }

    public static class VectorSearchCondition {
        private final String vectorColumn;
        private final String queryText;
        private final float[] targetVector;
        private final int k;
        private final double maxDistance;

        public VectorSearchCondition(String vectorColumn, String queryText, int k) {
            this(vectorColumn, queryText, null, k, 1.0);
        }

        public VectorSearchCondition(String vectorColumn, String queryText, float[] targetVector, int k, double maxDistance) {
            this.vectorColumn = vectorColumn.toLowerCase();
            this.queryText = queryText;
            this.targetVector = targetVector;
            this.k = k;
            this.maxDistance = maxDistance;
        }

        public String getVectorColumn() { return vectorColumn; }
        public String getQueryText() { return queryText; }
        public float[] getTargetVector() { return targetVector; }
        public int getK() { return k; }
        public double getMaxDistance() { return maxDistance; }
    }

    public static class FullTextCondition {
        private final String column;
        private final String queryText;

        public FullTextCondition(String column, String queryText) {
            this.column = column != null ? column.toLowerCase() : null;
            this.queryText = queryText;
        }

        public String getColumn() { return column; }
        public String getQueryText() { return queryText; }
    }

    public static class StreamPublishStatement implements Statement {
        private final String topic;
        private final Map<String, Object> payload;

        public StreamPublishStatement(String topic, Map<String, Object> payload) {
            this.topic = topic.toLowerCase();
            this.payload = payload;
        }

        public String getTopic() { return topic; }
        public Map<String, Object> getPayload() { return payload; }
    }

    public static class CreateDatabaseStatement implements Statement {
        private final String dbName;

        public CreateDatabaseStatement(String dbName) {
            this.dbName = dbName.toLowerCase();
        }

        public String getDbName() { return dbName; }
    }

    public static class DropDatabaseStatement implements Statement {
        private final String dbName;

        public DropDatabaseStatement(String dbName) {
            this.dbName = dbName.toLowerCase();
        }

        public String getDbName() { return dbName; }
    }

    public static class UseDatabaseStatement implements Statement {
        private final String dbName;

        public UseDatabaseStatement(String dbName) {
            this.dbName = dbName.toLowerCase();
        }

        public String getDbName() { return dbName; }
    }

    public static class ShowDatabasesStatement implements Statement {}

    public static class ShowTablesStatement implements Statement {
        private final String dbName;

        public ShowTablesStatement(String dbName) {
            this.dbName = dbName != null ? dbName.toLowerCase() : null;
        }

        public String getDbName() { return dbName; }
    }

    public static class UpdateStatement implements Statement {
        private final String tableName;
        private final Map<String, Object> setAssignments = new LinkedHashMap<>();
        private final List<Condition> whereConditions = new ArrayList<>();

        public UpdateStatement(String tableName) {
            this.tableName = tableName.toLowerCase();
        }

        public UpdateStatement addAssignment(String column, Object value) {
            setAssignments.put(column, value);
            return this;
        }

        public String getTableName() { return tableName; }
        public Map<String, Object> getSetAssignments() { return setAssignments; }
        public List<Condition> getWhereConditions() { return whereConditions; }
    }

    public static class DeleteStatement implements Statement {
        private final String tableName;
        private final List<Condition> whereConditions = new ArrayList<>();

        public DeleteStatement(String tableName) {
            this.tableName = tableName.toLowerCase();
        }

        public String getTableName() { return tableName; }
        public List<Condition> getWhereConditions() { return whereConditions; }
    }
}

