package com.app.infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class DataSourceProvider {

    private final DataSource dataSource;

    public DataSourceProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean ping() {
        try (Connection c = dataSource.getConnection()) {
            return c.isValid(1);
        } catch (Exception e) {
            return false;
        }
    }

    public void close() {
        if (dataSource instanceof AutoCloseable ac) {
            try {
                ac.close();
            } catch (Exception ignored) {
            }
        }
    }
}
