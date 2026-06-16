package com.app.infrastructure.redis;

import io.quarkus.redis.datasource.RedisDataSource;

public class RedisProvider {

    private final RedisDataSource redis;

    public RedisProvider(RedisDataSource redis) {
        this.redis = redis;
    }

    public boolean ping() {
        try {
            return "PONG".equalsIgnoreCase(redis.execute("PING").toString());
        } catch (Throwable t) {
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
