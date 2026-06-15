package com.app.infrastructure.health;

import com.app.infrastructure.database.DataSourceProvider;
import com.app.infrastructure.messaging.RabbitMqProvider;
import com.app.infrastructure.redis.RedisProvider;
import com.app.shared.config.AppSettings;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultHealthChecker implements HealthChecker {

    @Inject DataSourceProvider dataSource;
    @Inject RedisProvider redis;
    @Inject RabbitMqProvider rabbit;
    @Inject AppSettings settings;

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
