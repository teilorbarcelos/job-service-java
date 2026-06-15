package com.app.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.RedisDataSource;
import io.vertx.mutiny.redis.client.Response;

class RedisProviderTest {

    @Test
    void ping_returns_true_on_PONG() {
        var rds = mock(RedisDataSource.class);
        @SuppressWarnings("unchecked")
        Response response = mock(Response.class);
        when(response.toString()).thenReturn("PONG");
        when(rds.execute("PING")).thenReturn(response);
        var provider = new RedisProvider(rds);
        assertTrue(provider.ping());
    }

    @Test
    void ping_returns_true_case_insensitive() {
        var rds = mock(RedisDataSource.class);
        @SuppressWarnings("unchecked")
        Response response = mock(Response.class);
        when(response.toString()).thenReturn("pong");
        when(rds.execute("PING")).thenReturn(response);
        var provider = new RedisProvider(rds);
        assertTrue(provider.ping());
    }

    @Test
    void ping_returns_false_on_other_reply() {
        var rds = mock(RedisDataSource.class);
        @SuppressWarnings("unchecked")
        Response response = mock(Response.class);
        when(response.toString()).thenReturn("ERROR");
        when(rds.execute("PING")).thenReturn(response);
        var provider = new RedisProvider(rds);
        assertFalse(provider.ping());
    }

    @Test
    void ping_returns_false_on_exception() {
        var rds = mock(RedisDataSource.class);
        when(rds.execute("PING")).thenThrow(new RuntimeException("redis down"));
        var provider = new RedisProvider(rds);
        assertFalse(provider.ping());
    }

    @Test
    void close_closes_autocloseable() throws Exception {
        var rds = mock(RedisDataSource.class);
        var provider = new RedisProvider(rds);
        provider.close();
    }
}
