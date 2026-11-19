package com.chaflow.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public final class SocketClient {
    private static final String HOST = System.getProperty("chaflow.host", "127.0.0.1");
    private static final int PORT = Integer.getInteger("chaflow.port", 5050);
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private static final ObjectMapper JSON = new ObjectMapper();

    public AuthResult login(String phone, String password) {
        return auth("LOGIN", phone, password);
    }

    public AuthResult register(String phone, String username, String displayName, String password) {
        return auth("REGISTER", phone, username, displayName, password);
    }

    private AuthResult auth(String command, String... values) {
        try {
            Map<String, Object> account = JSON.readValue(raw(command, values), new TypeReference<>() {});
            String displayName = stringValue(account, "displayName");
            return new AuthResult(true, numberValue(account, "id"), stringValue(account, "phone"),
                    stringValue(account, "username"), displayName, stringValue(account, "token"),
                    "Welcome, " + displayName + "!");
        } catch (Exception exception) {
            return AuthResult.failure(message(exception));
        }
    }

    public List<Map<String, Object>> search(String token, String query) throws IOException {
        return list("SEARCH_USERS", token, query);
    }

    public Map<String, Object> friendRequest(String token, long target) throws IOException {
        return object("FRIEND_REQUEST", token, Long.toString(target));
    }

    public Map<String, Object> respond(String token, long requestId, boolean accept) throws IOException {
        return object("FRIEND_RESPOND", token, Long.toString(requestId), Boolean.toString(accept));
    }

    public List<Map<String, Object>> friendRequests(String token) throws IOException {
        return list("FRIEND_REQUESTS", token);
    }

    public List<Map<String, Object>> friends(String token) throws IOException {
        return list("FRIENDS", token);
    }

    public Map<String, Object> removeFriend(String token, long userId) throws IOException {
        return object("FRIEND_REMOVE", token, Long.toString(userId));
    }

    public long openDirect(String token, long target) throws IOException {
        return numberValue(object("OPEN_DIRECT", token, Long.toString(target)), "conversationId");
    }

    public List<Map<String, Object>> conversations(String token) throws IOException {
        return list("CONVERSATIONS", token);
    }

    public List<Map<String, Object>> messages(String token, long conversation) throws IOException {
        return list("MESSAGES", token, Long.toString(conversation));
    }

    public Map<String, Object> sendMessage(String token, long conversation, String content) throws IOException {
        return object("SEND_MESSAGE", token, Long.toString(conversation), content);
    }

    public Map<String, Object> uploadAttachment(String token, long conversation, String filename,
                                               String contentType, String base64Data) throws IOException {
        return object("UPLOAD_ATTACHMENT", token, Long.toString(conversation), filename, contentType, base64Data);
    }

    public Map<String, Object> downloadAttachment(String token, long blobId) throws IOException {
        return object("DOWNLOAD_ATTACHMENT", token, Long.toString(blobId));
    }

    public void logout(String token) {
        try {
            raw("LOGOUT", token);
        } catch (IOException ignored) {
        }
    }

    public EventConnection listenEvents(String token, Consumer<Map<String, Object>> onEvent,
                                        Consumer<Exception> onError) {
        EventConnection connection = new EventConnection(token, onEvent, onError);
        connection.start();
        return connection;
    }

    public static final class EventConnection implements AutoCloseable {
        private final String token;
        private final Consumer<Map<String, Object>> onEvent;
        private final Consumer<Exception> onError;
        private volatile Socket socket;
        private volatile boolean closed;
        private Thread thread;

        private EventConnection(String token, Consumer<Map<String, Object>> onEvent,
                                Consumer<Exception> onError) {
            this.token = Objects.requireNonNull(token);
            this.onEvent = Objects.requireNonNull(onEvent);
            this.onError = Objects.requireNonNull(onError);
        }

        private void start() {
            thread = new Thread(this::listen, "chaflow-realtime-events");
            thread.setDaemon(true);
            thread.start();
        }

        private void listen() {
            if (closed) return;
            try (Socket connection = new Socket()) {
                socket = connection;
                if (closed) return;
                connection.connect(new InetSocketAddress(HOST, PORT), 4000);
                connection.setSoTimeout(0);
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        connection.getOutputStream(), StandardCharsets.UTF_8));
                     BufferedReader reader = new BufferedReader(new InputStreamReader(
                             connection.getInputStream(), StandardCharsets.UTF_8))) {
                    writer.write("EVENTS\t" + ENC.encodeToString(token.getBytes(StandardCharsets.UTF_8)));
                    writer.newLine();
                    writer.flush();

                    String handshake = reader.readLine();
                    if (handshake == null) throw new IOException("Server closed the realtime connection.");
                    String[] hello = handshake.split("\\t", -1);
                    if (hello.length != 2) throw new IOException("Invalid realtime handshake.");
                    String payload = new String(DEC.decode(hello[1]), StandardCharsets.UTF_8);
                    if ("ERROR".equals(hello[0])) throw new IOException(payload);
                    if (!"OK".equals(hello[0])) throw new IOException("Unexpected realtime handshake.");

                    String line;
                    while (!closed && (line = reader.readLine()) != null) {
                        String[] parts = line.split("\\t", -1);
                        if (parts.length != 2 || !"EVENT".equals(parts[0])) {
                            throw new IOException("Invalid realtime event frame.");
                        }
                        String json = new String(DEC.decode(parts[1]), StandardCharsets.UTF_8);
                        onEvent.accept(JSON.readValue(json, new TypeReference<>() {}));
                    }
                    if (!closed) throw new IOException("Realtime connection was closed by the server.");
                }
            } catch (Exception exception) {
                if (!closed) onError.accept(exception);
            } finally {
                socket = null;
            }
        }

        @Override
        public void close() {
            closed = true;
            Socket current = socket;
            if (current != null) {
                try {
                    current.close();
                } catch (IOException ignored) {
                }
            }
            if (thread != null) thread.interrupt();
        }
    }

    private List<Map<String, Object>> list(String command, String token, String... values) throws IOException {
        return JSON.convertValue(JSON.readTree(raw(command, prepend(token, values))), new TypeReference<>() {});
    }

    private Map<String, Object> object(String command, String token, String... values) throws IOException {
        return JSON.readValue(raw(command, prepend(token, values)), new TypeReference<>() {});
    }

    private static String[] prepend(String token, String[] values) {
        String[] all = new String[values.length + 1];
        all[0] = token;
        System.arraycopy(values, 0, all, 1, values.length);
        return all;
    }

    private String raw(String command, String... values) throws IOException {
        StringBuilder request = new StringBuilder(command);
        for (String value : values) {
            request.append('\t').append(ENC.encodeToString((value == null ? "" : value)
                    .getBytes(StandardCharsets.UTF_8)));
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 4000);
            socket.setSoTimeout(120000);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(
                         socket.getInputStream(), StandardCharsets.UTF_8))) {
                writer.write(request.toString());
                writer.newLine();
                writer.flush();
                String response = reader.readLine();
                if (response == null) throw new IOException("Server closed the connection.");
                String[] parts = response.split("\\t", -1);
                if (parts.length != 2) throw new IOException("Unexpected server response.");
                String payload = new String(DEC.decode(parts[1]), StandardCharsets.UTF_8);
                if ("ERROR".equals(parts[0])) throw new IOException(payload);
                if (!"OK".equals(parts[0])) throw new IOException("Unexpected server response.");
                return payload;
            }
        }
    }

    private static String message(Exception exception) {
        String message = exception.getMessage();
        return message == null ? "Cannot reach ChaFlow server." : message;
    }

    private static String stringValue(Map<String, Object> values, String key) {
        return Objects.toString(values.get(key), "");
    }

    private static long numberValue(Map<String, Object> values, String key) {
        return ((Number) values.get(key)).longValue();
    }

    public record AuthResult(boolean successful, long userId, String phone, String username,
                             String displayName, String token, String message) {
        static AuthResult failure(String message) {
            return new AuthResult(false, -1, "", "", "", "", message);
        }
    }
}
