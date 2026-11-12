package com.chaflow.server;

import com.chaflow.server.auth.AuthService;
import com.chaflow.server.db.Database;

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
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String request = reader.readLine();
            String response = process(request);
            writer.write(response);
            writer.newLine();
            writer.flush();
        } catch (Exception exception) {
            System.err.println("Client request failed: " + exception.getMessage());
        }
    }

    private static String process(String request) {
        try {
            if (request == null || request.isBlank()) throw new IllegalArgumentException("Empty request.");
            String[] fields = request.split("\\t", -1);
            String command = fields[0];
            AuthService.Account account;
            if ("LOGIN".equals(command) && fields.length == 3) {
                account = AUTH.login(decode(fields[1]), decode(fields[2]));
            } else if ("REGISTER".equals(command) && fields.length == 5) {
                account = AUTH.register(decode(fields[1]), decode(fields[2]), decode(fields[3]), decode(fields[4]));
            } else {
                throw new IllegalArgumentException("Unknown request.");
            }
            return "OK\t" + account.id() + "\t" + encode(account.username()) + "\t" + encode(account.displayName());
        } catch (IllegalArgumentException exception) {
            return "ERROR\t" + encode(exception.getMessage() == null ? "Invalid request." : exception.getMessage());
        } catch (Exception exception) {
            exception.printStackTrace();
            return "ERROR\t" + encode("Server error. Check the server logs.");
        }
    }

    private static String decode(String value) {
        return new String(DECODER.decode(value), StandardCharsets.UTF_8);
    }
    private static String encode(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
