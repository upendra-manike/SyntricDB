package com.syntricdb.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;

public class SyntricConnection implements Connection {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String authHeader;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private boolean closed = false;
    private boolean autoCommit = true;

    public SyntricConnection(String host, int port, String database, String username, String password) throws SQLException {
        this.host = host != null && !host.isEmpty() ? host : "localhost";
        this.port = port > 0 ? port : 8080;
        this.database = database != null && !database.isEmpty() ? database : "default";
        this.username = username != null ? username : "admin";
        this.password = password != null ? password : "syntricdb_secret_pass";

        String rawAuth = this.username + ":" + this.password;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(rawAuth.getBytes(StandardCharsets.UTF_8));

        verifyConnection();
    }

    private void verifyConnection() throws SQLException {
        try {
            Map<String, Object> res = executeApiCall("/api/auth/verify", null, "GET");
            Boolean success = (Boolean) res.get("success");
            if (success == null || !success) {
                throw new SQLException("SyntricDB Authentication Failed: Invalid credentials for user '" + username + "'");
            }
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            try {
                Map<String, Object> res = executeApiCall("/api/health", null, "GET");
                if (res == null || !"UP".equals(res.get("status"))) {
                    throw new SQLException("Could not connect to SyntricDB server at http://" + host + ":" + port);
                }
            } catch (Exception ex) {
                throw new SQLException("Could not connect to SyntricDB server at http://" + host + ":" + port + " - " + ex.getMessage(), ex);
            }
        }
    }

    public Map<String, Object> executeApiCall(String path, Map<String, Object> reqBody) throws SQLException {
        return executeApiCall(path, reqBody, "POST");
    }

    public Map<String, Object> executeApiCall(String path, Map<String, Object> reqBody, String httpMethod) throws SQLException {
        checkClosed();
        try {
            URL url = new URL("http://" + host + ":" + port + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(httpMethod);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Authorization", authHeader);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            if (reqBody != null && ("POST".equals(httpMethod) || "PUT".equals(httpMethod))) {
                conn.setDoOutput(true);
                byte[] jsonBytes = jsonMapper.writeValueAsBytes(reqBody);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBytes);
                    os.flush();
                }
            }

            int statusCode = conn.getResponseCode();
            InputStream is = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();

            if (is == null) {
                if (statusCode == 401 || statusCode == 403) {
                    throw new SQLException("SyntricDB Authentication Failed (HTTP " + statusCode + ") for user '" + username + "'");
                }
                throw new SQLException("SyntricDB HTTP API Error (" + statusCode + ")");
            }

            byte[] responseBytes = is.readAllBytes();
            if (responseBytes.length == 0) {
                return Map.of("success", statusCode < 300);
            }

            return jsonMapper.readValue(responseBytes, Map.class);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Error communicating with SyntricDB API: " + e.getMessage(), e);
        }
    }

    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getHost() { return host; }
    public int getPort() { return port; }

    @Override
    public Statement createStatement() throws SQLException {
        checkClosed();
        return new SyntricStatement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkClosed();
        return new SyntricPreparedStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException("CallableStatement not supported");
    }

    @Override
    public String nativeSQL(String sql) throws SQLException { return sql; }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.autoCommit = autoCommit;
    }

    @Override
    public boolean getAutoCommit() throws SQLException { return autoCommit; }

    @Override
    public void commit() throws SQLException { checkClosed(); }

    @Override
    public void rollback() throws SQLException { checkClosed(); }

    @Override
    public void close() throws SQLException {
        this.closed = true;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        checkClosed();
        return new DatabaseMetaData() {
            @Override public boolean allProceduresAreCallable() throws SQLException { return false; }
            @Override public boolean allTablesAreSelectable() throws SQLException { return true; }
            @Override public String getURL() throws SQLException { return "jdbc:syntricdb://" + host + ":" + port + "/" + database; }
            @Override public String getUserName() throws SQLException { return username; }
            @Override public boolean isReadOnly() throws SQLException { return false; }
            @Override public boolean nullsAreSortedHigh() throws SQLException { return false; }
            @Override public boolean nullsAreSortedLow() throws SQLException { return false; }
            @Override public boolean nullsAreSortedAtStart() throws SQLException { return false; }
            @Override public boolean nullsAreSortedAtEnd() throws SQLException { return false; }
            @Override public String getDatabaseProductName() throws SQLException { return "SyntricDB"; }
            @Override public String getDatabaseProductVersion() throws SQLException { return "1.0.0-PROD"; }
            @Override public String getDriverName() throws SQLException { return "SyntricDB Native Driver"; }
            @Override public String getDriverVersion() throws SQLException { return "1.0.0"; }
            @Override public int getDriverMajorVersion() { return 1; }
            @Override public int getDriverMinorVersion() { return 0; }
            @Override public boolean usesLocalFiles() throws SQLException { return false; }
            @Override public boolean usesLocalFilePerTable() throws SQLException { return false; }
            @Override public boolean supportsMixedCaseIdentifiers() throws SQLException { return true; }
            @Override public boolean storesUpperCaseIdentifiers() throws SQLException { return false; }
            @Override public boolean storesLowerCaseIdentifiers() throws SQLException { return true; }
            @Override public boolean storesMixedCaseIdentifiers() throws SQLException { return true; }
            @Override public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException { return true; }
            @Override public boolean storesUpperCaseQuotedIdentifiers() throws SQLException { return false; }
            @Override public boolean storesLowerCaseQuotedIdentifiers() throws SQLException { return true; }
            @Override public boolean storesMixedCaseQuotedIdentifiers() throws SQLException { return true; }
            @Override public String getIdentifierQuoteString() throws SQLException { return "\""; }
            @Override public String getSQLKeywords() throws SQLException { return "VECTOR,SIMILAR,TOP,MATCH,EMBED,RAG"; }
            @Override public String getNumericFunctions() throws SQLException { return ""; }
            @Override public String getStringFunctions() throws SQLException { return ""; }
            @Override public String getSystemFunctions() throws SQLException { return ""; }
            @Override public String getTimeDateFunctions() throws SQLException { return ""; }
            @Override public String getSearchStringEscape() throws SQLException { return "\\"; }
            @Override public String getExtraNameCharacters() throws SQLException { return ""; }
            @Override public boolean supportsAlterTableWithAddColumn() throws SQLException { return true; }
            @Override public boolean supportsAlterTableWithDropColumn() throws SQLException { return true; }
            @Override public boolean supportsColumnAliasing() throws SQLException { return true; }
            @Override public boolean nullPlusNonNullIsNull() throws SQLException { return true; }
            @Override public boolean supportsConvert() throws SQLException { return false; }
            @Override public boolean supportsConvert(int fromType, int toType) throws SQLException { return false; }
            @Override public boolean supportsTableCorrelationNames() throws SQLException { return true; }
            @Override public boolean supportsDifferentTableCorrelationNames() throws SQLException { return false; }
            @Override public boolean supportsExpressionsInOrderBy() throws SQLException { return true; }
            @Override public boolean supportsOrderByUnrelated() throws SQLException { return true; }
            @Override public boolean supportsGroupBy() throws SQLException { return true; }
            @Override public boolean supportsGroupByUnrelated() throws SQLException { return true; }
            @Override public boolean supportsGroupByBeyondSelect() throws SQLException { return true; }
            @Override public boolean supportsLikeEscapeClause() throws SQLException { return true; }
            @Override public boolean supportsMultipleResultSets() throws SQLException { return false; }
            @Override public boolean supportsMultipleTransactions() throws SQLException { return false; }
            @Override public boolean supportsNonNullableColumns() throws SQLException { return true; }
            @Override public boolean supportsMinimumSQLGrammar() throws SQLException { return true; }
            @Override public boolean supportsCoreSQLGrammar() throws SQLException { return true; }
            @Override public boolean supportsExtendedSQLGrammar() throws SQLException { return false; }
            @Override public boolean supportsANSI92EntryLevelSQL() throws SQLException { return true; }
            @Override public boolean supportsANSI92IntermediateSQL() throws SQLException { return false; }
            @Override public boolean supportsANSI92FullSQL() throws SQLException { return false; }
            @Override public boolean supportsIntegrityEnhancementFacility() throws SQLException { return false; }
            @Override public boolean supportsOuterJoins() throws SQLException { return true; }
            @Override public boolean supportsFullOuterJoins() throws SQLException { return false; }
            @Override public boolean supportsLimitedOuterJoins() throws SQLException { return true; }
            @Override public String getSchemaTerm() throws SQLException { return "schema"; }
            @Override public String getProcedureTerm() throws SQLException { return "procedure"; }
            @Override public String getCatalogTerm() throws SQLException { return "database"; }
            @Override public boolean isCatalogAtStart() throws SQLException { return true; }
            @Override public String getCatalogSeparator() throws SQLException { return "."; }
            @Override public boolean supportsSchemasInDataManipulation() throws SQLException { return true; }
            @Override public boolean supportsSchemasInProcedureCalls() throws SQLException { return false; }
            @Override public boolean supportsSchemasInTableDefinitions() throws SQLException { return true; }
            @Override public boolean supportsSchemasInIndexDefinitions() throws SQLException { return true; }
            @Override public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException { return false; }
            @Override public boolean supportsCatalogsInDataManipulation() throws SQLException { return true; }
            @Override public boolean supportsCatalogsInProcedureCalls() throws SQLException { return false; }
            @Override public boolean supportsCatalogsInTableDefinitions() throws SQLException { return true; }
            @Override public boolean supportsCatalogsInIndexDefinitions() throws SQLException { return true; }
            @Override public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException { return false; }
            @Override public boolean supportsPositionedDelete() throws SQLException { return false; }
            @Override public boolean supportsPositionedUpdate() throws SQLException { return false; }
            @Override public boolean supportsSelectForUpdate() throws SQLException { return false; }
            @Override public boolean supportsStoredProcedures() throws SQLException { return false; }
            @Override public boolean supportsSubqueriesInComparisons() throws SQLException { return true; }
            @Override public boolean supportsSubqueriesInExists() throws SQLException { return true; }
            @Override public boolean supportsSubqueriesInIns() throws SQLException { return true; }
            @Override public boolean supportsSubqueriesInQuantifieds() throws SQLException { return false; }
            @Override public boolean supportsCorrelatedSubqueries() throws SQLException { return false; }
            @Override public boolean supportsUnion() throws SQLException { return true; }
            @Override public boolean supportsUnionAll() throws SQLException { return true; }
            @Override public boolean supportsOpenCursorsAcrossCommit() throws SQLException { return false; }
            @Override public boolean supportsOpenCursorsAcrossRollback() throws SQLException { return false; }
            @Override public boolean supportsOpenStatementsAcrossCommit() throws SQLException { return true; }
            @Override public boolean supportsOpenStatementsAcrossRollback() throws SQLException { return true; }
            @Override public int getMaxBinaryLiteralLength() throws SQLException { return 0; }
            @Override public int getMaxCharLiteralLength() throws SQLException { return 0; }
            @Override public int getMaxColumnNameLength() throws SQLException { return 128; }
            @Override public int getMaxColumnsInGroupBy() throws SQLException { return 32; }
            @Override public int getMaxColumnsInIndex() throws SQLException { return 16; }
            @Override public int getMaxColumnsInOrderBy() throws SQLException { return 32; }
            @Override public int getMaxColumnsInSelect() throws SQLException { return 256; }
            @Override public int getMaxColumnsInTable() throws SQLException { return 256; }
            @Override public int getMaxConnections() throws SQLException { return 10000; }
            @Override public int getMaxCursorNameLength() throws SQLException { return 64; }
            @Override public int getMaxIndexLength() throws SQLException { return 256; }
            @Override public int getMaxSchemaNameLength() throws SQLException { return 64; }
            @Override public int getMaxProcedureNameLength() throws SQLException { return 64; }
            @Override public int getMaxCatalogNameLength() throws SQLException { return 64; }
            @Override public int getMaxRowSize() throws SQLException { return 10485760; }
            @Override public boolean doesMaxRowSizeIncludeBlobs() throws SQLException { return true; }
            @Override public int getMaxStatementLength() throws SQLException { return 1048576; }
            @Override public int getMaxStatements() throws SQLException { return 0; }
            @Override public int getMaxTableNameLength() throws SQLException { return 128; }
            @Override public int getMaxTablesInSelect() throws SQLException { return 16; }
            @Override public int getMaxUserNameLength() throws SQLException { return 64; }
            @Override public int getDefaultTransactionIsolation() throws SQLException { return TRANSACTION_READ_COMMITTED; }
            @Override public boolean supportsTransactions() throws SQLException { return true; }
            @Override public boolean supportsTransactionIsolationLevel(int level) throws SQLException { return level == TRANSACTION_READ_COMMITTED; }
            @Override public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException { return false; }
            @Override public boolean supportsDataManipulationTransactionsOnly() throws SQLException { return true; }
            @Override public boolean dataDefinitionCausesTransactionCommit() throws SQLException { return false; }
            @Override public boolean dataDefinitionIgnoredInTransactions() throws SQLException { return false; }
            @Override public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getSchemas() throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getCatalogs() throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getTableTypes() throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getTypeInfo() throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public boolean supportsResultSetType(int type) throws SQLException { return type == ResultSet.TYPE_FORWARD_ONLY; }
            @Override public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException { return type == ResultSet.TYPE_FORWARD_ONLY && concurrency == ResultSet.CONCUR_READ_ONLY; }
            @Override public boolean ownUpdatesAreVisible(int type) throws SQLException { return false; }
            @Override public boolean ownDeletesAreVisible(int type) throws SQLException { return false; }
            @Override public boolean ownInsertsAreVisible(int type) throws SQLException { return false; }
            @Override public boolean othersUpdatesAreVisible(int type) throws SQLException { return false; }
            @Override public boolean othersDeletesAreVisible(int type) throws SQLException { return false; }
            @Override public boolean othersInsertsAreVisible(int type) throws SQLException { return false; }
            @Override public boolean updatesAreDetected(int type) throws SQLException { return false; }
            @Override public boolean deletesAreDetected(int type) throws SQLException { return false; }
            @Override public boolean insertsAreDetected(int type) throws SQLException { return false; }
            @Override public boolean supportsBatchUpdates() throws SQLException { return false; }
            @Override public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public Connection getConnection() throws SQLException { return SyntricConnection.this; }
            @Override public boolean supportsSavepoints() throws SQLException { return false; }
            @Override public boolean supportsNamedParameters() throws SQLException { return true; }
            @Override public boolean supportsMultipleOpenResults() throws SQLException { return false; }
            @Override public boolean supportsGetGeneratedKeys() throws SQLException { return true; }
            @Override public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public boolean supportsResultSetHoldability(int holdability) throws SQLException { return holdability == ResultSet.HOLD_CURSORS_OVER_COMMIT; }
            @Override public int getResultSetHoldability() throws SQLException { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }
            @Override public int getDatabaseMajorVersion() throws SQLException { return 1; }
            @Override public int getDatabaseMinorVersion() throws SQLException { return 0; }
            @Override public int getJDBCMajorVersion() throws SQLException { return 4; }
            @Override public int getJDBCMinorVersion() throws SQLException { return 2; }
            @Override public int getSQLStateType() throws SQLException { return sqlStateSQL; }
            @Override public boolean locatorsUpdateCopy() throws SQLException { return false; }
            @Override public boolean supportsStatementPooling() throws SQLException { return false; }
            @Override public RowIdLifetime getRowIdLifetime() throws SQLException { return RowIdLifetime.ROWID_UNSUPPORTED; }
            @Override public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException { return false; }
            @Override public boolean autoCommitFailureClosesAllResultSets() throws SQLException { return false; }
            @Override public ResultSet getClientInfoProperties() throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException { return new SyntricResultSet(List.of(), List.of()); }
            @Override public boolean generatedKeyAlwaysReturned() throws SQLException { return false; }
            @Override public <T> T unwrap(Class<T> iface) throws SQLException {
                if (iface.isInstance(this)) return iface.cast(this);
                throw new SQLException("Cannot unwrap to " + iface.getName());
            }
            @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return iface.isInstance(this); }
        };
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {}

    @Override
    public boolean isReadOnly() throws SQLException { return false; }

    @Override
    public void setCatalog(String catalog) throws SQLException {}

    @Override
    public String getCatalog() throws SQLException { return database; }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {}

    @Override
    public int getTransactionIsolation() throws SQLException { return TRANSACTION_READ_COMMITTED; }

    @Override
    public SQLWarning getWarnings() throws SQLException { return null; }

    @Override
    public void clearWarnings() throws SQLException {}

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException { return createStatement(); }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return prepareStatement(sql); }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException { return Map.of(); }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {}

    @Override
    public void setHoldability(int holdability) throws SQLException {}

    @Override
    public int getHoldability() throws SQLException { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }

    @Override
    public Savepoint setSavepoint() throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return createStatement(); }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return prepareStatement(sql); }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return prepareStatement(sql); }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return prepareStatement(sql); }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return prepareStatement(sql); }

    @Override
    public Clob createClob() throws SQLException { return null; }

    @Override
    public Blob createBlob() throws SQLException { return null; }

    @Override
    public NClob createNClob() throws SQLException { return null; }

    @Override
    public SQLXML createSQLXML() throws SQLException { return null; }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (closed) return false;
        try {
            verifyConnection();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {}

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {}

    @Override
    public String getClientInfo(String name) throws SQLException { return null; }

    @Override
    public Properties getClientInfo() throws SQLException { return new Properties(); }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException { return null; }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException { return null; }

    @Override
    public void setSchema(String schema) throws SQLException {}

    @Override
    public String getSchema() throws SQLException { return database; }

    @Override
    public void abort(Executor executor) throws SQLException { close(); }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {}

    @Override
    public int getNetworkTimeout() throws SQLException { return 0; }

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
            throw new SQLException("Connection is closed");
        }
    }
}
