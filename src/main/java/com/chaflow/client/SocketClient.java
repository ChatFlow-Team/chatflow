package com.chaflow.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketClient {
    private static final String SERVER_HOST = System.getProperty("chaflow.host", "127.0.0.1");
    private static final int SERVER_PORT = Integer.getInteger("chaflow.port", 5050);
    private Socket socket;

    public String connectAndReceive() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), 3000);
            socket.setSoTimeout(3000);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            String message = reader.readLine();
            return message == null ? "Connected" : message;
        } catch (IOException exception) {
            return "ERROR:" + exception.getMessage();
        } finally {
            close();
        }
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
