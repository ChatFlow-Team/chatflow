package com.chaflow.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class SocketServer {
    private static final int PORT = Integer.parseInt(
            System.getenv().getOrDefault("CHAFLOW_PORT", "5050"));
    private static final Path DATA_DIR = Path.of(
            System.getenv().getOrDefault("CHAFLOW_DATA_DIR", "/app/data"));

    public static void main(String[] args) throws IOException {
        Files.createDirectories(DATA_DIR);
        Path logFile = DATA_DIR.resolve("server.log");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("ChaFlow Socket Server listening on port " + PORT);
            log(logFile, "Server started on port " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                String remote = String.valueOf(client.getRemoteSocketAddress());
                System.out.println("Client connected: " + remote);

                try (client;
                     BufferedWriter writer = new BufferedWriter(
                             new OutputStreamWriter(client.getOutputStream()))) {
                    writer.write("ChaFlow server is ready!");
                    writer.newLine();
                    writer.flush();
                    log(logFile, "Client connected: " + remote);
                } catch (IOException exception) {
                    System.err.println("Client communication error: " + exception.getMessage());
                    log(logFile, "Client communication error: " + exception.getMessage());
                }
            }
        }
    }

    private static void log(Path file, String message) throws IOException {
        Files.writeString(file, Instant.now() + " " + message + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
