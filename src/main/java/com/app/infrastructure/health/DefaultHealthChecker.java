package com.app.infrastructure.health;

import com.app.infrastructure.database.DataSourceProvider;
import com.app.infrastructure.messaging.RabbitMqProvider;
import com.app.infrastructure.redis.RedisProvider;
import com.app.shared.config.AppSettings;

public class DefaultHealthChecker implements HealthChecker {

    private final DataSourceProvider dataSource;
    private final RedisProvider redis;
    private final RabbitMqProvider rabbit;
    private final AppSettings settings;

    public DefaultHealthChecker(DataSourceProvider dataSource, RedisProvider redis,
                                RabbitMqProvider rabbit, AppSettings settings) {
        this.dataSource = dataSource;
        this.redis = redis;
        this.rabbit = rabbit;
        this.settings = settings;
    }

    @Override
    public HealthCheckResult checkPostgres() {
        long start = System.nanoTime();
        boolean ok = dataSource.ping();
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        if (ok) return HealthCheckResult.up(latencyMs);
        return HealthCheckResult.down(latencyMs, "ping returned false");
    }

    @Override
    public HealthCheckResult checkRedis() {
        long start = System.nanoTime();
        boolean ok = redis.ping();
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        if (ok) return HealthCheckResult.up(latencyMs);
        return HealthCheckResult.down(latencyMs, "ping returned false");
    }

    @Override
    public HealthCheckResult checkRabbit() {
        if (!settings.messagingEnabled()) {
            return HealthCheckResult.disabled();
        }
        if (rabbit.isOpen()) return HealthCheckResult.up(0L);
        return HealthCheckResult.down(0L, "connection closed");
    }
}
