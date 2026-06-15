package com.app.infrastructure.messaging;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMqProvider {

    private static final String DEFAULT_USER = "guest";
    private static final int DEFAULT_PORT = 5672;

    private final Object lock = new Object();
    private Connection connection;
    private Channel channel;
    private String url;
    private String user;
    private String password;
    private long publishTimeoutMs;

    public void init(String url, String user, String password, long publishTimeoutMs) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.publishTimeoutMs = publishTimeoutMs;
    }

    public void connect() throws IOException, TimeoutException, URISyntaxException {
        synchronized (lock) {
            if (isAlreadyConnected()) return;
            ConnectionFactory factory = new ConnectionFactory();
            String amqpUri = buildAmqpUri(new URI(url));
            try {
                factory.setUri(amqpUri);
            } catch (Exception e) {
                throw new IOException("invalid rabbit uri: " + e.getMessage(), e);
            }
            connection = factory.newConnection("job-service-java");
            channel = connection.createChannel();
        }
    }

    private boolean isAlreadyConnected() {
        return connection != null && connection.isOpen()
            && channel != null && channel.isOpen();
    }

    private String buildAmqpUri(URI uri) {
        String uriUserInfo = uri.getUserInfo();
        String resolvedUser = resolveUser(uriUserInfo);
        String resolvedPass = resolvePass(uriUserInfo);
        int port = uri.getPort() == -1 ? DEFAULT_PORT : uri.getPort();
        return "amqp://" + resolvedUser + ":" + resolvedPass + "@" + uri.getHost() + ":" + port + "/";
    }

    private String resolveUser(String uriUserInfo) {
        if (user != null && !user.isBlank()) return user;
        if (uriUserInfo == null) return DEFAULT_USER;
        return uriUserInfo.split(":")[0];
    }

    private String resolvePass(String uriUserInfo) {
        if (password != null && !password.isBlank()) return password;
        if (uriUserInfo == null) return DEFAULT_USER;
        if (!uriUserInfo.contains(":")) return DEFAULT_USER;
        return uriUserInfo.split(":")[1];
    }

    public boolean isOpen() {
        synchronized (lock) {
            return connection != null && connection.isOpen()
                && channel != null && channel.isOpen();
        }
    }

    public void publish(String exchange, String routingKey, String json) throws IOException {
        synchronized (lock) {
            if (!isOpen()) {
                throw new IOException("rabbit is not connected");
            }
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            channel.basicPublish(exchange, routingKey, true, false,
                new com.rabbitmq.client.AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build(),
                body);
        }
    }

    public void close() {
        synchronized (lock) {
            try { if (channel != null && channel.isOpen()) channel.close(); } catch (Exception ignored) {}
            try { if (connection != null && connection.isOpen()) connection.close(); } catch (Exception ignored) {}
            channel = null;
            connection = null;
        }
    }

    long getPublishTimeoutMs() { return publishTimeoutMs; }
}
