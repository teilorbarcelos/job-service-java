package com.app.infrastructure.health;

public interface HealthChecker {
    HealthCheckResult checkPostgres();

    HealthCheckResult checkRedis();

    HealthCheckResult checkRabbit();
}
