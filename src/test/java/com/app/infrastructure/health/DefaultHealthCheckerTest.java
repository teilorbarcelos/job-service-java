package com.app.infrastructure.health;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.app.infrastructure.database.DataSourceProvider;
import com.app.infrastructure.messaging.RabbitMqProvider;
import com.app.infrastructure.redis.RedisProvider;
import com.app.shared.config.AppSettings;

class DefaultHealthCheckerTest {

    @Test
    void check_postgres_up() {
        var ds = mock(DataSourceProvider.class);
        when(ds.ping()).thenReturn(true);
        var checker = new DefaultHealthChecker(ds, mock(RedisProvider.class), mock(RabbitMqProvider.class), defaultSettings());
        var result = checker.checkPostgres();
        assertEquals(HealthStatus.UP, result.status());
        assertNull(result.error());
    }

    @Test
    void check_postgres_down() {
        var ds = mock(DataSourceProvider.class);
        when(ds.ping()).thenReturn(false);
        var checker = new DefaultHealthChecker(ds, mock(RedisProvider.class), mock(RabbitMqProvider.class), defaultSettings());
        var result = checker.checkPostgres();
        assertEquals(HealthStatus.DOWN, result.status());
        assertEquals("ping returned false", result.error());
    }

    @Test
    void check_redis_up() {
        var r = mock(RedisProvider.class);
        when(r.ping()).thenReturn(true);
        var checker = new DefaultHealthChecker(mock(DataSourceProvider.class), r, mock(RabbitMqProvider.class), defaultSettings());
        var result = checker.checkRedis();
        assertEquals(HealthStatus.UP, result.status());
    }

    @Test
    void check_redis_down() {
        var r = mock(RedisProvider.class);
        when(r.ping()).thenReturn(false);
        var checker = new DefaultHealthChecker(mock(DataSourceProvider.class), r, mock(RabbitMqProvider.class), defaultSettings());
        var result = checker.checkRedis();
        assertEquals(HealthStatus.DOWN, result.status());
        assertEquals("ping returned false", result.error());
    }

    @Test
    void check_rabbit_disabled_when_messaging_off() {
        var rb = mock(RabbitMqProvider.class);
        when(rb.isOpen()).thenReturn(true);
        var s = defaultSettings();
        s = new AppSettings(s.environment(), s.logLevel(), s.shutdownTimeout(), s.jobExecutionTimeout(),
            s.databaseUrl(), s.databaseCommandTimeout(), s.redisUrl(), s.redisHost(), s.redisPort(),
            s.redisPassword(), s.redisDb(), s.redisCommandTimeout(), false,
            s.rabbitUrl(), s.rabbitUser(), s.rabbitPassword(), s.rabbitPublishTimeout(),
            s.healthCheckCron(), s.healthCheckEnabled());
        var checker = new DefaultHealthChecker(mock(DataSourceProvider.class), mock(RedisProvider.class), rb, s);
        assertEquals(HealthStatus.DISABLED, checker.checkRabbit().status());
    }

    @Test
    void check_rabbit_up_when_open() {
        var rb = mock(RabbitMqProvider.class);
        when(rb.isOpen()).thenReturn(true);
        var s = defaultSettings();
        s = new AppSettings(s.environment(), s.logLevel(), s.shutdownTimeout(), s.jobExecutionTimeout(),
            s.databaseUrl(), s.databaseCommandTimeout(), s.redisUrl(), s.redisHost(), s.redisPort(),
            s.redisPassword(), s.redisDb(), s.redisCommandTimeout(), true,
            s.rabbitUrl(), s.rabbitUser(), s.rabbitPassword(), s.rabbitPublishTimeout(),
            s.healthCheckCron(), s.healthCheckEnabled());
        var checker = new DefaultHealthChecker(mock(DataSourceProvider.class), mock(RedisProvider.class), rb, s);
        assertEquals(HealthStatus.UP, checker.checkRabbit().status());
    }

    @Test
    void check_rabbit_down_when_closed() {
        var rb = mock(RabbitMqProvider.class);
        when(rb.isOpen()).thenReturn(false);
        var s = defaultSettings();
        s = new AppSettings(s.environment(), s.logLevel(), s.shutdownTimeout(), s.jobExecutionTimeout(),
            s.databaseUrl(), s.databaseCommandTimeout(), s.redisUrl(), s.redisHost(), s.redisPort(),
            s.redisPassword(), s.redisDb(), s.redisCommandTimeout(), true,
            s.rabbitUrl(), s.rabbitUser(), s.rabbitPassword(), s.rabbitPublishTimeout(),
            s.healthCheckCron(), s.healthCheckEnabled());
        var checker = new DefaultHealthChecker(mock(DataSourceProvider.class), mock(RedisProvider.class), rb, s);
        assertEquals(HealthStatus.DOWN, checker.checkRabbit().status());
        assertEquals("connection closed", checker.checkRabbit().error());
    }

    private static AppSettings defaultSettings() {
        return new AppSettings("test", "INFO", Duration.ofSeconds(5), Duration.ofSeconds(5),
            "jdbc:postgresql://localhost/test", Duration.ofSeconds(5),
            "", "h", 6379, "", 0, Duration.ofSeconds(5),
            false, "amqp://localhost:5672/", "u", "u", Duration.ofSeconds(5),
            "*/1 * * * *", true);
    }
}
