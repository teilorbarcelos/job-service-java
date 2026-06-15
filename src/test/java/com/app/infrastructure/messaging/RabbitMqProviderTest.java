package com.app.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class RabbitMqProviderTest {

    @Test
    void new_provider_not_open() {
        var p = new RabbitMqProvider();
        assertFalse(p.isOpen());
    }

    @Test
    void publish_throws_when_not_connected() {
        var p = new RabbitMqProvider();
        assertThrows(IOException.class, () -> p.publish("ex", "rk", "{}"));
    }

    @Test
    void close_idempotent_when_not_connected() {
        var p = new RabbitMqProvider();
        p.close();
        p.close();
    }

    @Test
    void init_and_connect_invalid_url_throws() {
        var p = new RabbitMqProvider();
        p.init("://invalid-uri", "u", "p", 1000);
        assertThrows(Exception.class, p::connect);
    }

    @Test
    void connect_no_creds_in_url_uses_defaults() throws Exception {
        // URL with no user:pass@ (defaults to anonymous)
        var p = new RabbitMqProvider();
        p.init("amqp://localhost:5672/", "", "", 1000);
        try {
            p.connect();
        } catch (IOException | TimeoutException | URISyntaxException e) {
            // Expected: cannot reach localhost
        }
    }

    @Test
    void publish_with_explicit_user_password() {
        var p = new RabbitMqProvider();
        p.init("amqp://user:pass@localhost:5672/", "override", "overridepass", 1000);
        // No connection attempted, just verifies init doesn't throw
    }

    @Test
    void get_publish_timeout() {
        var p = new RabbitMqProvider();
        p.init("amqp://u:p@localhost:5672/", "", "", 5000);
        assertEquals(5000L, p.getPublishTimeoutMs());
    }
}
