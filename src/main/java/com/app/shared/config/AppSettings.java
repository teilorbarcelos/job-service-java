package com.app.shared.config;

import java.time.Duration;

import com.app.shared.errors.AppError;

public record AppSettings(
    String environment,
    String logLevel,
    Duration shutdownTimeout,
    Duration jobExecutionTimeout,
    String databaseUrl,
    Duration databaseCommandTimeout,
    String redisUrl,
    String redisHost,
    int redisPort,
    String redisPassword,
    int redisDb,
    Duration redisCommandTimeout,
    boolean messagingEnabled,
    String rabbitUrl,
    String rabbitUser,
    String rabbitPassword,
    Duration rabbitPublishTimeout,
    String healthCheckCron,
    boolean healthCheckEnabled
) {
    public static AppSettings load() {
        return new AppSettings(
            getEnv("ENVIRONMENT", "local"),
            getEnv("LOG_LEVEL", "INFO"),
            Duration.ofSeconds(getEnvInt("SHUTDOWN_TIMEOUT_SECONDS", 30)),
            Duration.ofSeconds(getEnvInt("JOB_EXECUTION_TIMEOUT_SECONDS", 300)),
            getEnv("DATABASE_URL", ""),
            Duration.ofSeconds(getEnvInt("DATABASE_COMMAND_TIMEOUT_SECONDS", 10)),
            getEnv("REDIS_URL", ""),
            getEnv("REDIS_HOST", "localhost"),
            getEnvInt("REDIS_PORT", 6379),
            getEnv("REDIS_PASSWORD", ""),
            getEnvInt("REDIS_DB", 0),
            Duration.ofSeconds(getEnvInt("REDIS_COMMAND_TIMEOUT_SECONDS", 5)),
            getEnvBool("MESSAGING_ENABLED", false),
            getEnv("RABBIT_URL", "amqp://guest:guest@localhost:5672/"),
            getEnv("RABBIT_USER", "guest"),
            getEnv("RABBIT_PASSWORD", "guest"),
            Duration.ofSeconds(getEnvInt("RABBITMQ_PUBLISH_TIMEOUT", 5)),
            getEnv("HEALTH_CHECK_CRON", "*/1 * * * *"),
            getEnvBool("HEALTH_CHECK_ENABLED", true)
        );
    }

    private static String getEnv(String key, String fallback) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return fallback;
        return v;
    }

    private static int getEnvInt(String key, int fallback) {
        String raw = System.getenv(key);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw AppError.configuration("invalid integer for " + key + ": '" + raw + "'");
        }
    }

    private static boolean getEnvBool(String key, boolean fallback) {
        String raw = System.getenv(key);
        if (raw == null || raw.isBlank()) return fallback;
        String lower = raw.trim().toLowerCase();
        if (lower.equals("true") || lower.equals("1")) return true;
        if (lower.equals("false") || lower.equals("0")) return false;
        throw AppError.configuration("invalid boolean for " + key + ": '" + raw + "'");
    }
}
