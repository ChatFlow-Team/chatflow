package com.chaflow.server.socket;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Keeps authenticated, long-lived event connections indexed by user ID.
 * A caller must determine recipients from authorized conversation membership
 * before publishing an event through this registry.
 */
public final class ClientConnectionRegistry {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final ConcurrentMap<Long, Set<Subscriber>> subscribers = new ConcurrentHashMap<>();

    public Subscriber register(long userId, String token, Socket socket, BufferedWriter writer) {
        Subscriber subscriber = new Subscriber(userId, token, socket, writer);
        subscribers.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(subscriber);
        return subscriber;
    }

    public void publish(long userId, Object event) {
        Set<Subscriber> connections = subscribers.get(userId);
        if (connections == null || connections.isEmpty()) return;

        final String payload;
        try {
            payload = ENCODER.encodeToString(JSON.writeValueAsString(event).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("Could not serialize realtime event: " + e.getMessage());
            return;
        }

        for (Subscriber subscriber : connections) {
            try {
                subscriber.send("EVENT\t" + payload);
            } catch (IOException e) {
                unregister(subscriber);
            }
        }
    }

    public void unregister(Subscriber subscriber) {
        if (subscriber == null) return;
        Set<Subscriber> connections = subscribers.get(subscriber.userId);
        if (connections != null) {
            connections.remove(subscriber);
            if (connections.isEmpty()) subscribers.remove(subscriber.userId, connections);
        }
        subscriber.close();
    }

    public void closeToken(String token) {
        if (token == null) return;
        for (Set<Subscriber> connections : subscribers.values()) {
            for (Subscriber subscriber : connections) {
                if (token.equals(subscriber.token)) unregister(subscriber);
            }
        }
    }

    public static final class Subscriber {
        private final long userId;
        private final String token;
        private final Socket socket;
        private final BufferedWriter writer;

        private Subscriber(long userId, String token, Socket socket, BufferedWriter writer) {
            this.userId = userId;
            this.token = token;
            this.socket = socket;
            this.writer = writer;
        }

        private void send(String line) throws IOException {
            synchronized (writer) {
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
        }

        private void close() {
            try { socket.close(); } catch (IOException ignored) { }
        }
    }
}
