package com.app.infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DataSourceProvider {

    @Inject
    DataSource dataSource;

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean ping() {
        try (Connection c = dataSource.getConnection()) {
            return c.isValid(1);
        } catch (SQLException e) {
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
