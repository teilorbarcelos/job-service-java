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
import com.rabbitmq.client.ConnectionFactory;

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
    void init_sets_all_fields() {
        var p = new RabbitMqProvider();
        p.init("amqp://host", "u", "p", 9999);
        assertEquals(9999L, p.getPublishTimeoutMs());
    }

    @Test
    void connect_early_return_when_already_connected() throws Exception {
        var p = new RabbitMqProvider();
        var connection = mock(Connection.class);
        var channel = mock(Channel.class);
        when(connection.isOpen()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        injectConnection(p, connection, channel);
        p.connect();
        verify(connection, never()).close();
        verify(channel, never()).close();
    }

    @Test
    void connect_success_path_with_mocked_factory() throws Exception {
        var connection = mock(Connection.class);
        var channel = mock(Channel.class);
        when(connection.isOpen()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(connection.createChannel()).thenReturn(channel);
        var factory = mock(ConnectionFactory.class);
        when(factory.newConnection("job-service-java")).thenReturn(connection);

        var p = new TestableProvider(factory);
        p.init("amqp://myhost:5672/", "", "", 1000);
        p.connect();
        assertTrue(p.isOpen());
        verify(factory).setUri("amqp://guest:guest@myhost:5672/");
    }

    @Test
    void connect_catches_exception_from_setUri() throws Exception {
        var factory = mock(ConnectionFactory.class);
        doThrow(new java.net.URISyntaxException("bad", "nope")).when(factory).setUri(anyString());

        var p = new TestableProvider(factory);
        p.init("amqp://localhost:5672/", "", "", 1000);
        assertThrows(IOException.class, p::connect);
    }

    @Test
    void isAlreadyConnected_all_false_when_null_connection() {
        var p = new RabbitMqProvider();
        assertFalse(p.isAlreadyConnected());
    }

    @Test
    void isAlreadyConnected_false_when_connection_closed() throws Exception {
        var p = new RabbitMqProvider();
        var connection = mock(Connection.class);
        when(connection.isOpen()).thenReturn(false);
        injectConnection(p, connection, null);
        assertFalse(p.isAlreadyConnected());
    }

    @Test
    void isAlreadyConnected_false_when_channel_null() throws Exception {
        var p = new RabbitMqProvider();
        var connection = mock(Connection.class);
        when(connection.isOpen()).thenReturn(true);
        injectConnection(p, connection, null);
        assertFalse(p.isAlreadyConnected());
    }

    @Test
    void isAlreadyConnected_false_when_channel_closed() throws Exception {
        var p = new RabbitMqProvider();
        var connection = mock(Connection.class);
        var channel = mock(Channel.class);
        when(connection.isOpen()).thenReturn(true);
        when(channel.isOpen()).thenReturn(false);
        injectConnection(p, connection, channel);
        assertFalse(p.isAlreadyConnected());
    }

    @Test
    void isAlreadyConnected_true_when_both_open() throws Exception {
        var p = new RabbitMqProvider();
        var connection = mock(Connection.class);
        var channel = mock(Channel.class);
        when(connection.isOpen()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        injectConnection(p, connection, channel);
        assertTrue(p.isAlreadyConnected());
    }

    @Test
    void resolveUser_returns_explicit_user() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://host", "explicit", "", 1000);
        assertEquals("explicit", p.resolveUser("ignored"));
        assertEquals("explicit", p.resolveUser("fromurl"));
        assertEquals("explicit", p.resolveUser(null));
    }

    @Test
    void resolveUser_from_uri_info() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://host", "", "", 1000);
        assertEquals("fromurl", p.resolveUser("fromurl:mypass"));
        assertEquals("onlyuser", p.resolveUser("onlyuser"));
    }

    @Test
    void resolveUser_default_when_no_user_info_null() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://host", "", "", 1000);
        assertEquals("guest", p.resolveUser(null));
    }

    @Test
    void resolvePass_explicit_password() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://host", "", "mypass", 1000);
        assertEquals("mypass", p.resolvePass("fromurl:mypassfromurl"));
        assertEquals("mypass", p.resolvePass("onlyuser"));
        assertEquals("mypass", p.resolvePass(null));
    }

    @Test
    void resolvePass_from_uri_info() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://host", "", "", 1000);
        assertEquals("mypass", p.resolvePass("user:mypass"));
    }

    @Test
    void resolvePass_default_when_no_password_in_uri() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://host", "", "", 1000);
        assertEquals("guest", p.resolvePass("u@host"));
    }

    @Test
    void resolvePass_default_when_null_user_info() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://host", "", "", 1000);
        assertEquals("guest", p.resolvePass(null));
    }

    @Test
    void buildAmqpUri_with_default_port() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://localhost", "u", "p", 1000);
        assertEquals("amqp://u:p@localhost:5672/", p.buildAmqpUri(new URI("amqp://localhost")));
    }

    @Test
    void buildAmqpUri_with_explicit_port() throws Exception {
        var p = new RabbitMqProvider();
        p.init("amqp://host:1234/", "", "", 1000);
        assertEquals("amqp://guest:guest@host:1234/", p.buildAmqpUri(new URI("amqp://host:1234/")));
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

    private static void injectConnection(RabbitMqProvider p, Connection connection, Channel channel)
            throws Exception {
        var f = RabbitMqProvider.class.getDeclaredField("connection");
        f.setAccessible(true);
        f.set(p, connection);
        var ch = RabbitMqProvider.class.getDeclaredField("channel");
        ch.setAccessible(true);
        ch.set(p, channel);
    }

    private static class TestableProvider extends RabbitMqProvider {
        private final ConnectionFactory factory;

        TestableProvider(ConnectionFactory factory) {
            this.factory = factory;
        }

        @Override
        ConnectionFactory newConnectionFactory() {
            return factory;
        }
    }
}
