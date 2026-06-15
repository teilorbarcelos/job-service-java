package com.app.infrastructure.health;

public record HealthCheckResult(HealthStatus status, Long latencyMs, String error) {
    public static HealthCheckResult up(long latencyMs) {
        return new HealthCheckResult(HealthStatus.UP, latencyMs, null);
    }
    public static HealthCheckResult down(long latencyMs, String error) {
        return new HealthCheckResult(HealthStatus.DOWN, latencyMs, error);
    }
    public static HealthCheckResult disabled() {
        return new HealthCheckResult(HealthStatus.DISABLED, null, null);
    }
}
