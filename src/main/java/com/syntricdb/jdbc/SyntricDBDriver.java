package com.syntricdb.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Official SyntricDB Native JDBC Driver.
 * Accepts connection URLs starting with "jdbc:syntricdb:" or "jdbc:postgresql:"
 * and manages wire protocol connections to SyntricDB ports (5432 / 8080).
 */
public class SyntricDBDriver implements Driver {

    private static final String URL_PREFIX = "jdbc:syntricdb:";
    private static final Driver POSTGRES_DRIVER;

    static {
        Driver pgDriver = null;
        try {
            Class<?> pgDriverClass = Class.forName("org.postgresql.Driver");
            pgDriver = (Driver) pgDriverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(pgDriver);
        } catch (Throwable t) {
            // PostgreSQL driver optional on classpath
        }
        POSTGRES_DRIVER = pgDriver;

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

        String targetUrl = convertUrl(url);

        if (POSTGRES_DRIVER != null) {
            return POSTGRES_DRIVER.connect(targetUrl, info);
        }

        throw new SQLException("No suitable underlying wire driver found for " + url + ". Ensure org.postgresql:postgresql is on classpath.");
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && (url.startsWith(URL_PREFIX) || url.startsWith("jdbc:postgresql:"));
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        if (POSTGRES_DRIVER != null) {
            return POSTGRES_DRIVER.getPropertyInfo(convertUrl(url), info);
        }
        return new DriverPropertyInfo[0];
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
        if (POSTGRES_DRIVER != null) {
            return POSTGRES_DRIVER.getParentLogger();
        }
        throw new SQLFeatureNotSupportedException("Logger not supported");
    }

    private String convertUrl(String url) {
        if (url == null) return null;
        if (url.startsWith(URL_PREFIX)) {
            String stripped = url.substring(URL_PREFIX.length());
            return "jdbc:postgresql:" + stripped;
        }
        return url;
    }
}
