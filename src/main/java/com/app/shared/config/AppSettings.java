package com.app.shared.config;

import java.time.Duration;
import java.util.function.UnaryOperator;

import com.app.shared.errors.AppError;

public record AppSettings(String environment, String logLevel, Duration shutdownTimeout, Duration jobExecutionTimeout,
        String databaseUrl, Duration databaseCommandTimeout, String redisUrl, String redisHost, int redisPort,
        String redisPassword, int redisDb, Duration redisCommandTimeout, boolean messagingEnabled, String rabbitUrl,
        String rabbitUser, String rabbitPassword, Duration rabbitPublishTimeout, String healthCheckCron,
        boolean healthCheckEnabled) {

    public static AppSettings load() {
        return load(System::getenv);
    }

    public static AppSettings load(UnaryOperator<String> env) {
        return new AppSettings(getEnv(env, "ENVIRONMENT", "local"), getEnv(env, "LOG_LEVEL", "INFO"),
                Duration.ofSeconds(getEnvInt(env, "SHUTDOWN_TIMEOUT_SECONDS", 30)),
                Duration.ofSeconds(getEnvInt(env, "JOB_EXECUTION_TIMEOUT_SECONDS", 300)),
                getEnv(env, "DATABASE_URL", ""),
                Duration.ofSeconds(getEnvInt(env, "DATABASE_COMMAND_TIMEOUT_SECONDS", 10)),
                getEnv(env, "REDIS_URL", ""), getEnv(env, "REDIS_HOST", "localhost"),
                getEnvInt(env, "REDIS_PORT", 6379), getEnv(env, "REDIS_PASSWORD", ""), getEnvInt(env, "REDIS_DB", 0),
                Duration.ofSeconds(getEnvInt(env, "REDIS_COMMAND_TIMEOUT_SECONDS", 5)),
                getEnvBool(env, "MESSAGING_ENABLED", false),
                getEnv(env, "RABBIT_URL", "amqp://guest:guest@localhost:5672/"), getEnv(env, "RABBIT_USER", "guest"),
                getEnv(env, "RABBIT_PASSWORD", "guest"),
                Duration.ofSeconds(getEnvInt(env, "RABBITMQ_PUBLISH_TIMEOUT", 5)),
                getEnv(env, "HEALTH_CHECK_CRON", "*/1 * * * *"), getEnvBool(env, "HEALTH_CHECK_ENABLED", true));
    }

    private static String getEnv(UnaryOperator<String> env, String key, String fallback) {
        String v = env.apply(key);
        if (v == null || v.isBlank())
            return fallback;
        return v;
    }

    private static int getEnvInt(UnaryOperator<String> env, String key, int fallback) {
        String raw = env.apply(key);
        if (raw == null || raw.isBlank())
            return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw AppError.configuration("invalid integer for " + key + ": '" + raw + "'");
        }
    }

    private static boolean getEnvBool(UnaryOperator<String> env, String key, boolean fallback) {
        String raw = env.apply(key);
        if (raw == null || raw.isBlank())
            return fallback;
        String lower = raw.trim().toLowerCase();
        if (lower.equals("true") || lower.equals("1"))
            return true;
        if (lower.equals("false") || lower.equals("0"))
            return false;
        throw AppError.configuration("invalid boolean for " + key + ": '" + raw + "'");
    }
}
