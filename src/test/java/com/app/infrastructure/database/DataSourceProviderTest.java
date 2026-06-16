package com.app.infrastructure.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.withSettings;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

class DataSourceProviderTest {

    @Test
    void getConnection_delegates() throws Exception {
        var ds = mock(DataSource.class);
        var conn = mock(Connection.class);
        when(ds.getConnection()).thenReturn(conn);
        var provider = new DataSourceProvider(ds);
        assertSame(conn, provider.getConnection());
    }

    @Test
    void getConnection_propagates_sql_exception() throws Exception {
        var ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("boom"));
        var provider = new DataSourceProvider(ds);
        assertThrows(SQLException.class, provider::getConnection);
    }

    @Test
    void ping_returns_true_on_valid() throws Exception {
        var ds = mock(DataSource.class);
        var conn = mock(Connection.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.isValid(1)).thenReturn(true);
        var provider = new DataSourceProvider(ds);
        assertTrue(provider.ping());
        verify(conn).close();
    }

    @Test
    void ping_returns_false_on_invalid() throws Exception {
        var ds = mock(DataSource.class);
        var conn = mock(Connection.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.isValid(1)).thenReturn(false);
        var provider = new DataSourceProvider(ds);
        assertFalse(provider.ping());
    }

    @Test
    void ping_returns_false_on_exception() throws Exception {
        var ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new java.sql.SQLException("ds down"));
        var provider = new DataSourceProvider(ds);
        assertFalse(provider.ping());
    }

    @Test
    void close_skips_non_autocloseable() {
        var ds = mock(DataSource.class);
        var provider = new DataSourceProvider(ds);
        provider.close();
    }

    @Test
    void close_invokes_autocloseable() throws Exception {
        var ds = mock(DataSource.class, withSettings().extraInterfaces(AutoCloseable.class));
        var provider = new DataSourceProvider(ds);
        provider.close();
        verify((AutoCloseable) ds).close();
    }

    @Test
    void close_swallows_exception() throws Exception {
        var ds = mock(DataSource.class, withSettings().extraInterfaces(AutoCloseable.class));
        doThrow(new RuntimeException("oops")).when((AutoCloseable) ds).close();
        var provider = new DataSourceProvider(ds);
        provider.close();
    }
}
