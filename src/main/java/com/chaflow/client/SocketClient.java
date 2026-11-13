package com.chaflow.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SocketClient {
    private static final String SERVER_HOST = System.getProperty("chaflow.host", "127.0.0.1");
    private static final int SERVER_PORT = Integer.getInteger("chaflow.port", 5050);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public AuthResult login(String phone, String password) {
        return send("LOGIN", phone, password);
    }

    public AuthResult register(String phone, String username, String displayName, String password) {
        return send("REGISTER", phone, username, displayName, password);
    }

    public OpenConversationResult openConversation(long requesterId, String targetIdOrPhone) {
        if (targetIdOrPhone == null || targetIdOrPhone.isBlank()) {
            return OpenConversationResult.failure("The other user's id or phone is required.");
        }
        StringBuilder request = new StringBuilder("OPEN_CONVERSATION");
        request.append('\t').append(encode(Long.toString(requesterId)));
        request.append('\t').append(encode(targetIdOrPhone));

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), 4000);
            socket.setSoTimeout(8000);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                writer.write(request.toString());
                writer.newLine();
                writer.flush();
                String response = reader.readLine();
                if (response == null) return OpenConversationResult.failure("The server closed the connection.");
                String[] fields = response.split("\\t", -1);
                if ("OK".equals(fields[0]) && fields.length == 3) {
                    return OpenConversationResult.success(Long.parseLong(fields[1]), "1".equals(fields[2]));
                }
                if ("ERROR".equals(fields[0]) && fields.length == 2) {
                    return OpenConversationResult.failure(decode(fields[1]));
                }
                return OpenConversationResult.failure("Unexpected server response.");
            }
        } catch (IOException | NumberFormatException exception) {
            return OpenConversationResult.failure("Cannot reach ChaFlow server: " + exception.getMessage());
        }
    }

    private AuthResult send(String command, String... values) {
        StringBuilder request = new StringBuilder(command);
        for (String value : values) {
            request.append('\t').append(ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8)));
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), 4000);
            socket.setSoTimeout(8000);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                writer.write(request.toString());
                writer.newLine();
                writer.flush();
                String response = reader.readLine();
                if (response == null) return AuthResult.failure("The server closed the connection.");
                String[] fields = response.split("\\t", -1);
                if ("OK".equals(fields[0]) && fields.length == 4) {
                    return AuthResult.success(Long.parseLong(fields[1]), decode(fields[2]), decode(fields[3]));
                }
                if ("ERROR".equals(fields[0]) && fields.length == 2) return AuthResult.failure(decode(fields[1]));
                return AuthResult.failure("Unexpected server response.");
            }
        } catch (IOException | NumberFormatException exception) {
            return AuthResult.failure("Cannot reach ChaFlow server: " + exception.getMessage());
        }
    }

    private static String decode(String value) {
        return new String(DECODER.decode(value), StandardCharsets.UTF_8);
    }

    private static String encode(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public record AuthResult(boolean successful, long userId, String username, String displayName, String message) {
        static AuthResult success(long id, String username, String name) {
            return new AuthResult(true, id, username, name, "Welcome, " + name + "!");
        }
        static AuthResult failure(String message) {
            return new AuthResult(false, -1, null, null, message);
        }
    }

    public record OpenConversationResult(boolean successful, long conversationId, boolean created, String message) {
        static OpenConversationResult success(long conversationId, boolean created) {
            return new OpenConversationResult(true, conversationId, created,
                    created ? "New conversation started." : "Existing conversation reused.");
        }

        static OpenConversationResult failure(String message) {
            return new OpenConversationResult(false, -1, false, message);
        }
    }
}
