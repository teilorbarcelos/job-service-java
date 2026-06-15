package com.app.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

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
    void get_publish_timeout() {
        var p = new RabbitMqProvider();
        p.init("amqp://localhost:5672/", "", "", 5000);
        assertEquals(5000L, p.getPublishTimeoutMs());
    }

    @Test
    void buildAmqpUri_with_explicit_user_and_password() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://localhost:5672/", "usr1", "pw1", 1000);
        assertEquals("amqp://usr1:pw1@localhost:5672/", p.buildAmqpUri(new URI("amqp://localhost:5672/")));
    }

    @Test
    void buildAmqpUri_with_user_in_url() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://u2:pp2@host:5672/", "", "", 1000);
        assertEquals("amqp://u2:pp2@host:5672/", p.buildAmqpUri(new URI("amqp://u2:pp2@host:5672/")));
    }

    @Test
    void buildAmqpUri_with_user_only_in_url_no_password() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://u3@host:5672/", "", "", 1000);
        assertEquals("amqp://u3:guest@host:5672/", p.buildAmqpUri(new URI("amqp://u3@host:5672/")));
    }

    @Test
    void buildAmqpUri_with_default_port() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://localhost", "u", "p", 1000);
        assertEquals("amqp://u:p@localhost:5672/", p.buildAmqpUri(new URI("amqp://localhost")));
    }

    @Test
    void buildAmqpUri_with_explicit_password_override() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://localhost:5672/", "u", "overridepass", 1000);
        assertEquals("amqp://u:overridepass@localhost:5672/", p.buildAmqpUri(new URI("amqp://localhost:5672/")));
    }

    @Test
    void isOpen_returns_true_when_connected() throws Exception {
        var p = new RabbitMqProvider();
        var connection = mock(Connection.class);
        var channel = mock(Channel.class);
        when(connection.isOpen()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        injectConnection(p, connection, channel);
        assertTrue(p.isOpen());
    }

    @Test
    void isOpen_returns_false_when_disconnected() throws Exception {
        var p = new RabbitMqProvider();
        var connection = mock(Connection.class);
        when(connection.isOpen()).thenReturn(false);
        injectConnection(p, connection, null);
        assertFalse(p.isOpen());
    }

    @Test
    void publish_succeeds_when_connected() throws Exception {
        var p = new RabbitMqProvider();
        var connection = mock(Connection.class);
        var channel = mock(Channel.class);
        when(connection.isOpen()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        injectConnection(p, connection, channel);
        p.publish("ex", "rk", "{\"a\":1}");
        verify(channel).basicPublish(eq("ex"), eq("rk"), eq(true), eq(false), any(), eq("{\"a\":1}".getBytes()));
    }

    @Test
    void close_closes_connected_channel_and_connection() throws Exception {
        var p = new RabbitMqProvider();
        var connection = mock(Connection.class);
        var channel = mock(Channel.class);
        when(connection.isOpen()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        injectConnection(p, connection, channel);
        p.close();
        verify(channel).close();
        verify(connection).close();
    }

    @Test
    void close_swallows_exceptions() throws Exception {
        var p = new RabbitMqProvider();
        var connection = mock(Connection.class);
        var channel = mock(Channel.class);
        when(connection.isOpen()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        doThrow(new IOException("oops")).when(channel).close();
        doThrow(new IOException("oops")).when(connection).close();
        injectConnection(p, connection, channel);
        p.close();
    }

    @Test
    void init_sets_all_fields() {
        var p = new RabbitMqProvider();
        p.init("amqp://host", "u", "p", 9999);
        assertEquals(9999L, p.getPublishTimeoutMs());
    }

    private static void injectConnection(RabbitMqProvider p, Connection connection, Channel channel)
            throws Exception {
        var f = RabbitMqProvider.class.getDeclaredField("connection");
        f.setAccessible(true);
        f.set(p, connection);
        var ch = RabbitMqProvider.class.getDeclaredField("channel");
        ch.setAccessible(true);
        ch.set(p, channel);
    }
}
