package com.syntricdb.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class SyntricStatement implements Statement {

    private final SyntricConnection connection;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private boolean closed = false;
    private ResultSet currentResultSet = null;
    private int updateCount = -1;

    public SyntricStatement(SyntricConnection connection) {
        this.connection = connection;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        execute(sql);
        if (currentResultSet == null) {
            throw new SQLException("Query did not return a ResultSet");
        }
        return currentResultSet;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        execute(sql);
        return updateCount >= 0 ? updateCount : 0;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        checkClosed();
        this.currentResultSet = null;
        this.updateCount = -1;

        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("sql", sql);
        reqBody.put("database", connection.getDatabase());

        Map<String, Object> responseMap = connection.executeApiCall("/api/sql", reqBody);

        boolean success = Boolean.TRUE.equals(responseMap.get("success"));
        if (!success) {
            String err = responseMap.containsKey("error") ? responseMap.get("error").toString() : "Database error";
            throw new SQLException("SyntricDB Error: " + err);
        }

        if (responseMap.containsKey("data")) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
            if (data == null) {
                data = Collections.emptyList();
            }
            List<String> columns = data.isEmpty() ? Collections.emptyList() : new ArrayList<>(data.get(0).keySet());
            this.currentResultSet = new SyntricResultSet(data, columns);
            return true;
        } else {
            Number affectedRows = (Number) responseMap.getOrDefault("affectedRows", responseMap.get("rowCount"));
            this.updateCount = affectedRows != null ? affectedRows.intValue() : 1;
            return false;
        }
    }

    @Override
    public void close() throws SQLException {
        this.closed = true;
        if (currentResultSet != null) {
            currentResultSet.close();
        }
    }

    @Override
    public int getMaxFieldSize() throws SQLException { return 0; }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {}

    @Override
    public int getMaxRows() throws SQLException { return 0; }

    @Override
    public void setMaxRows(int max) throws SQLException {}

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {}

    @Override
    public int getQueryTimeout() throws SQLException { return 0; }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {}

    @Override
    public void cancel() throws SQLException {}

    @Override
    public SQLWarning getWarnings() throws SQLException { return null; }

    @Override
    public void clearWarnings() throws SQLException {}

    @Override
    public void setCursorName(String name) throws SQLException {}

    @Override
    public ResultSet getResultSet() throws SQLException { return currentResultSet; }

    @Override
    public int getUpdateCount() throws SQLException { return updateCount; }

    @Override
    public boolean getMoreResults() throws SQLException { return false; }

    @Override
    public void setFetchDirection(int direction) throws SQLException {}

    @Override
    public int getFetchDirection() throws SQLException { return ResultSet.FETCH_FORWARD; }

    @Override
    public void setFetchSize(int rows) throws SQLException {}

    @Override
    public int getFetchSize() throws SQLException { return 0; }

    @Override
    public int getResultSetConcurrency() throws SQLException { return ResultSet.CONCUR_READ_ONLY; }

    @Override
    public int getResultSetType() throws SQLException { return ResultSet.TYPE_FORWARD_ONLY; }

    @Override
    public void addBatch(String sql) throws SQLException {}

    @Override
    public void clearBatch() throws SQLException {}

    @Override
    public int[] executeBatch() throws SQLException { return new int[0]; }

    @Override
    public Connection getConnection() throws SQLException { return connection; }

    @Override
    public boolean getMoreResults(int current) throws SQLException { return false; }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return new SyntricResultSet(List.of(), List.of());
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { return executeUpdate(sql); }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { return executeUpdate(sql); }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException { return executeUpdate(sql); }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { return execute(sql); }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException { return execute(sql); }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException { return execute(sql); }

    @Override
    public int getResultSetHoldability() throws SQLException { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }

    @Override
    public boolean isClosed() throws SQLException { return closed; }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {}

    @Override
    public boolean isPoolable() throws SQLException { return false; }

    @Override
    public void closeOnCompletion() throws SQLException {}

    @Override
    public boolean isCloseOnCompletion() throws SQLException { return false; }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    private void checkClosed() throws SQLException {
        if (closed) {
            throw new SQLException("Statement is closed");
        }
    }
}
