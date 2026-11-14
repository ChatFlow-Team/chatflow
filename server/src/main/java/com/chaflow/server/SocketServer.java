package com.chaflow.server;

import com.chaflow.server.auth.AuthService;
import com.chaflow.server.db.Database;
import com.chaflow.server.service.ConversationService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SocketServer {
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("CHAFLOW_PORT", "5050"));
    private static final AuthService AUTH = new AuthService();
    private static final ConversationService CONVERSATIONS = new ConversationService();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public static void main(String[] args) throws Exception {
        Database.migrate();
        System.out.println("Flyway migrations completed. ChaFlow database is ready.");
        ExecutorService clients = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("ChaFlow authentication server listening on port " + PORT);
            while (true) {
                Socket socket = server.accept();
                clients.submit(() -> handle(socket));
            }
        } finally {
            clients.shutdown();
        }
    }

    private static void handle(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String request = reader.readLine();
            writer.write(process(request));
            writer.newLine();
            writer.flush();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static String process(String request) {
        try {
            if (request == null || request.isBlank()) throw new IllegalArgumentException("Empty request.");
            String[] fields = request.split("\\t", -1);
            String command = fields[0];

            return switch (command) {
                case "LOGIN" -> {
                    if (fields.length != 3) throw new IllegalArgumentException("Unknown request.");
                    AuthService.Account account = AUTH.login(decode(fields[1]), decode(fields[2]));
                    yield encodeAccount(account);
                }
                case "REGISTER" -> {
                    if (fields.length != 5) throw new IllegalArgumentException("Unknown request.");
                    AuthService.Account account = AUTH.register(decode(fields[1]), decode(fields[2]), decode(fields[3]), decode(fields[4]));
                    yield encodeAccount(account);
                }
                // New command for this task: open (or reuse) a private conversation.
                // Request shape:  OPEN_CONVERSATION \t <requesterId, base64> \t <targetIdOrPhone, base64>
                // Response shape: OK \t <conversationId> \t <created: "1" or "0">
                //
                // TODO FOR YOU: fields[1] (the requester id) is sent by the client itself,
                // because this protocol has no session/login-token that would let the server
                // know "who is this socket" on its own — see the longer note in
                // ConversationService.openPrivateConversation's javadoc. Until that's added,
                // whatever the client sends here is trusted as-is.
                case "OPEN_CONVERSATION" -> {
                    if (fields.length != 3) throw new IllegalArgumentException("Unknown request.");
                    long requesterId;
                    try {
                        requesterId = Long.parseLong(decode(fields[1]));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid requester id.");
                    }
                    String targetIdOrPhone = decode(fields[2]);
                    ConversationService.ConversationResult result =
                            CONVERSATIONS.openPrivateConversation(requesterId, targetIdOrPhone);
                    yield "OK\t" + result.conversationId() + "\t" + (result.created() ? "1" : "0");
                }
                default -> throw new IllegalArgumentException("Unknown request.");
            };
        } catch (IllegalArgumentException exception) {
            return "ERROR\t" + encode(exception.getMessage() == null ? "Invalid request." : exception.getMessage());
        } catch (Exception exception) {
            exception.printStackTrace();
            return "ERROR\t" + encode("Server error. Check the server logs.");
        }
    }


    private static String encodeAccount(AuthService.Account account) {
        return "OK\t" + account.id() + "\t" + encode(account.username()) + "\t" + encode(account.displayName());
    }

    private static String decode(String value) {
        return new String(DECODER.decode(value), StandardCharsets.UTF_8);
    }
    private static String encode(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

}
