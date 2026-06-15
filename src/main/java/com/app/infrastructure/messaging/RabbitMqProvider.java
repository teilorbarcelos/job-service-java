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
            if (connection != null && connection.isOpen() && channel != null && channel.isOpen()) {
                return;
            }
            ConnectionFactory factory = new ConnectionFactory();
            URI uri = new URI(url);
            String uriUserInfo = uri.getUserInfo();
            String u = (user == null || user.isBlank())
                ? (uriUserInfo == null ? "guest" : uriUserInfo.split(":")[0])
                : user;
            String p = (password == null || password.isBlank())
                ? (uriUserInfo != null && uriUserInfo.contains(":")
                    ? uriUserInfo.split(":")[1] : "guest")
                : password;
            int port = uri.getPort() == -1 ? 5672 : uri.getPort();
            try {
                factory.setUri("amqp://" + u + ":" + p + "@" + uri.getHost() + ":" + port + "/");
            } catch (Exception e) {
                throw new IOException("invalid rabbit uri: " + e.getMessage(), e);
            }
            connection = factory.newConnection("job-service-java");
            channel = connection.createChannel();
        }
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
