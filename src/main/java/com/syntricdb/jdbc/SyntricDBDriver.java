package com.syntricdb.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Official Native SyntricDB JDBC Driver.
 * Accepts connection URLs starting with "jdbc:syntricdb:" or "syntricdb:".
 * Connects natively to SyntricDB engine endpoints.
 */
public class SyntricDBDriver implements Driver {

    private static final String URL_PREFIX = "jdbc:syntricdb:";
    private static final String SHORT_PREFIX = "syntricdb:";

    // Pattern for jdbc:syntricdb://[user:pass@]host[:port]/[database]
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(?:jdbc:)?syntricdb://(?:([^:@]+):([^@]+)@)?([^:/]+)(?::(\\d+))?(?:/(.+))?$"
    );

    static {
        try {
            DriverManager.registerDriver(new SyntricDBDriver());
        } catch (SQLException e) {
            System.err.println("Failed to register SyntricDBDriver: " + e.getMessage());
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }

        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new SQLException("Invalid SyntricDB connection URL format: " + url +
                ". Expected format: jdbc:syntricdb://[user:pass@]host:port/database");
        }

        String urlUser = matcher.group(1);
        String urlPass = matcher.group(2);
        String host = matcher.group(3);
        String portStr = matcher.group(4);
        String database = matcher.group(5);

        String user = urlUser != null ? urlUser : (info != null ? info.getProperty("user", "admin") : "admin");
        String pass = urlPass != null ? urlPass : (info != null ? info.getProperty("password", "syntricdb_secret_pass") : "syntricdb_secret_pass");
        int port = portStr != null ? Integer.parseInt(portStr) : 8080;
        if (database == null || database.isBlank()) {
            database = "default";
        }

        return new SyntricConnection(host, port, database, user, pass);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && (url.startsWith(URL_PREFIX) || url.startsWith(SHORT_PREFIX));
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        DriverPropertyInfo userProp = new DriverPropertyInfo("user", info != null ? info.getProperty("user", "admin") : "admin");
        userProp.description = "SyntricDB Admin/User Name";
        userProp.required = false;

        DriverPropertyInfo passProp = new DriverPropertyInfo("password", info != null ? info.getProperty("password", "") : "");
        passProp.description = "SyntricDB Password";
        passProp.required = false;

        return new DriverPropertyInfo[]{ userProp, passProp };
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger("com.syntricdb.jdbc");
    }
}
