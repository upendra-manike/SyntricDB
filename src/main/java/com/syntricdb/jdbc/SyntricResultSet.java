package com.syntricdb.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class SyntricResultSet implements ResultSet {

    private final List<Map<String, Object>> rows;
    private final List<String> columns;
    private int currentIndex = -1;
    private boolean closed = false;
    private boolean lastValueWasNull = false;

    public SyntricResultSet(List<Map<String, Object>> rows, List<String> columns) {
        this.rows = rows != null ? rows : List.of();
        this.columns = columns != null ? columns : List.of();
    }

    @Override
    public boolean next() throws SQLException {
        checkClosed();
        if (currentIndex + 1 < rows.size()) {
            currentIndex++;
            return true;
        } else {
            currentIndex = rows.size();
            return false;
        }
    }

    @Override
    public void close() throws SQLException {
        this.closed = true;
    }

    @Override
    public boolean wasNull() throws SQLException {
        return lastValueWasNull;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        return val != null ? val.toString() : null;
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        Object val = getObject(columnLabel);
        return val != null ? val.toString() : null;
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val instanceof Boolean) return (Boolean) val;
        return val != null && Boolean.parseBoolean(val.toString());
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        Object val = getObject(columnLabel);
        if (val instanceof Boolean) return (Boolean) val;
        return val != null && Boolean.parseBoolean(val.toString());
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return (byte) getInt(columnIndex);
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return (byte) getInt(columnLabel);
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        return (short) getInt(columnIndex);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return (short) getInt(columnLabel);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val == null) return 0;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        Object val = getObject(columnLabel);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val == null) return 0;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val == null) return 0L;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        Object val = getObject(columnLabel);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val == null) return 0L;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return (float) getDouble(columnIndex);
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return (float) getDouble(columnLabel);
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val == null) return 0.0;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        Object val = getObject(columnLabel);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val == null) return 0.0;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return getBigDecimal(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return getBigDecimal(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        Object val = getObject(columnIndex);
        if (val == null) return null;
        return new BigDecimal(val.toString());
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        Object val = getObject(columnLabel);
        if (val == null) return null;
        return new BigDecimal(val.toString());
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        String val = getString(columnIndex);
        return val != null ? val.getBytes() : null;
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        String val = getString(columnLabel);
        return val != null ? val.getBytes() : null;
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        String val = getString(columnIndex);
        return val != null ? Date.valueOf(val) : null;
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        String val = getString(columnLabel);
        return val != null ? Date.valueOf(val) : null;
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        String val = getString(columnIndex);
        return val != null ? Time.valueOf(val) : null;
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        String val = getString(columnLabel);
        return val != null ? Time.valueOf(val) : null;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        String val = getString(columnIndex);
        return val != null ? Timestamp.valueOf(val) : null;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        String val = getString(columnLabel);
        return val != null ? Timestamp.valueOf(val) : null;
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException { return null; }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException { return null; }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException { return null; }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException { return null; }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException { return null; }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException { return null; }

    @Override
    public SQLWarning getWarnings() throws SQLException { return null; }

    @Override
    public void clearWarnings() throws SQLException {}

    @Override
    public String getCursorName() throws SQLException { return "SyntricCursor"; }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new SyntricResultSetMetaData(columns);
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        checkClosed();
        checkRowPosition();
        if (columnIndex < 1 || columnIndex > columns.size()) {
            throw new SQLException("Invalid column index: " + columnIndex);
        }
        String columnName = columns.get(columnIndex - 1);
        return getObject(columnName);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        checkClosed();
        checkRowPosition();
        Map<String, Object> row = rows.get(currentIndex);
        Object val = row.get(columnLabel);
        if (val == null) {
            // Case-insensitive lookup fallback
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(columnLabel)) {
                    val = entry.getValue();
                    break;
                }
            }
        }
        this.lastValueWasNull = (val == null);
        return val;
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(columnLabel)) {
                return i + 1;
            }
        }
        throw new SQLException("Column '" + columnLabel + "' not found in ResultSet");
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException { return null; }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException { return null; }

    @Override
    public boolean isBeforeFirst() throws SQLException { return currentIndex < 0 && !rows.isEmpty(); }

    @Override
    public boolean isAfterLast() throws SQLException { return currentIndex >= rows.size() && !rows.isEmpty(); }

    @Override
    public boolean isFirst() throws SQLException { return currentIndex == 0 && !rows.isEmpty(); }

    @Override
    public boolean isLast() throws SQLException { return currentIndex == rows.size() - 1 && !rows.isEmpty(); }

    @Override
    public void beforeFirst() throws SQLException { currentIndex = -1; }

    @Override
    public void afterLast() throws SQLException { currentIndex = rows.size(); }

    @Override
    public boolean first() throws SQLException {
        if (!rows.isEmpty()) {
            currentIndex = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean last() throws SQLException {
        if (!rows.isEmpty()) {
            currentIndex = rows.size() - 1;
            return true;
        }
        return false;
    }

    @Override
    public int getRow() throws SQLException { return currentIndex >= 0 && currentIndex < rows.size() ? currentIndex + 1 : 0; }

    @Override
    public boolean absolute(int row) throws SQLException {
        if (row > 0 && row <= rows.size()) {
            currentIndex = row - 1;
            return true;
        }
        return false;
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        return absolute(currentIndex + 1 + rows);
    }

    @Override
    public boolean previous() throws SQLException {
        if (currentIndex > 0) {
            currentIndex--;
            return true;
        }
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {}

    @Override
    public int getFetchDirection() throws SQLException { return FETCH_FORWARD; }

    @Override
    public void setFetchSize(int rows) throws SQLException {}

    @Override
    public int getFetchSize() throws SQLException { return rows.size(); }

    @Override
    public int getType() throws SQLException { return TYPE_FORWARD_ONLY; }

    @Override
    public int getConcurrency() throws SQLException { return CONCUR_READ_ONLY; }

    @Override
    public boolean rowUpdated() throws SQLException { return false; }

    @Override
    public boolean rowInserted() throws SQLException { return false; }

    @Override
    public boolean rowDeleted() throws SQLException { return false; }

    @Override
    public void updateNull(int columnIndex) throws SQLException {}

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {}

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {}

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {}

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {}

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {}

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {}

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {}

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {}

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {}

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {}

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {}

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {}

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {}

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {}

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {}

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {}

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {}

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {}

    @Override
    public void updateNull(String columnLabel) throws SQLException {}

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {}

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {}

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {}

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {}

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {}

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {}

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {}

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {}

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {}

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {}

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {}

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {}

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {}

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {}

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {}

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {}

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {}

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {}

    @Override
    public void insertRow() throws SQLException {}

    @Override
    public void updateRow() throws SQLException {}

    @Override
    public void deleteRow() throws SQLException {}

    @Override
    public void refreshRow() throws SQLException {}

    @Override
    public void cancelRowUpdates() throws SQLException {}

    @Override
    public void moveToInsertRow() throws SQLException {}

    @Override
    public void moveToCurrentRow() throws SQLException {}

    @Override
    public Statement getStatement() throws SQLException { return null; }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException { return getObject(columnIndex); }

    @Override
    public Ref getRef(int columnIndex) throws SQLException { return null; }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException { return null; }

    @Override
    public Clob getClob(int columnIndex) throws SQLException { return null; }

    @Override
    public Array getArray(int columnIndex) throws SQLException { return null; }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException { return getObject(columnLabel); }

    @Override
    public Ref getRef(String columnLabel) throws SQLException { return null; }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException { return null; }

    @Override
    public Clob getClob(String columnLabel) throws SQLException { return null; }

    @Override
    public Array getArray(String columnLabel) throws SQLException { return null; }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException { return getDate(columnIndex); }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException { return getDate(columnLabel); }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException { return getTime(columnIndex); }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException { return getTime(columnLabel); }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException { return getTimestamp(columnIndex); }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException { return getTimestamp(columnLabel); }

    @Override
    public URL getURL(int columnIndex) throws SQLException { return null; }

    @Override
    public URL getURL(String columnLabel) throws SQLException { return null; }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {}

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {}

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {}

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {}

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {}

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {}

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {}

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {}

    @Override
    public RowId getRowId(int columnIndex) throws SQLException { return null; }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException { return null; }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {}

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {}

    @Override
    public int getHoldability() throws SQLException { return HOLD_CURSORS_OVER_COMMIT; }

    @Override
    public boolean isClosed() throws SQLException { return closed; }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {}

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {}

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {}

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {}

    @Override
    public NClob getNClob(int columnIndex) throws SQLException { return null; }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException { return null; }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException { return null; }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException { return null; }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {}

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {}

    @Override
    public String getNString(int columnIndex) throws SQLException { return getString(columnIndex); }

    @Override
    public String getNString(String columnLabel) throws SQLException { return getString(columnLabel); }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException { return null; }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException { return null; }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {}

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {}

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {}

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {}

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {}

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {}

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {}

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {}

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {}

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {}

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {}

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {}

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {}

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {}

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {}

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {}

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {}

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {}

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {}

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {}

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {}

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {}

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {}

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {}

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {}

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {}

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {}

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {}

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        Object val = getObject(columnIndex);
        if (val == null) return null;
        if (type.isInstance(val)) return type.cast(val);
        return null;
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        Object val = getObject(columnLabel);
        if (val == null) return null;
        if (type.isInstance(val)) return type.cast(val);
        return null;
    }

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
            throw new SQLException("ResultSet is closed");
        }
    }

    private void checkRowPosition() throws SQLException {
        if (currentIndex < 0 || currentIndex >= rows.size()) {
            throw new SQLException("Invalid row position. Call next() before retrieving column values.");
        }
    }
}
