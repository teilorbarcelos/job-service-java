package com.app.infrastructure.redis;

import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RedisProvider {

    @Inject
    RedisDataSource redis;

    public boolean ping() {
        try {
            return "PONG".equalsIgnoreCase(
                redis.execute("PING").toString());
        } catch (Exception e) {
            return false;
        }
    }

    public void close() {
        if (redis instanceof AutoCloseable ac) {
            try {
                ac.close();
            } catch (Exception ignored) {
            }
        }
    }
}
