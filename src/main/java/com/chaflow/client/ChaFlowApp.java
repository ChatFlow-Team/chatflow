package com.chaflow.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ChaFlowApp extends Application {
    private final SocketClient socketClient = new SocketClient();

    @Override
    public void start(Stage stage) {
        Label logo = new Label("ChaFlow");
        // test ammend commit
        logo.setFont(Font.font("Arial", 34));
        logo.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a085;");

        Label welcome = new Label("Welcome to ChaFlow!");
        welcome.setFont(Font.font("Arial", 20));
        welcome.setStyle("-fx-text-fill: #263238;");

        Label connection = new Label("Connecting to ChaFlow server...");
        connection.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");

        VBox root = new VBox(16, logo, welcome, connection);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f7fafc; -fx-padding: 40;");

        stage.setTitle("ChaFlow");
        stage.setScene(new Scene(root, 520, 360));
        stage.setMinWidth(420);
        stage.setMinHeight(280);
        stage.show();

        Thread connectionThread = new Thread(() -> {
            String response = socketClient.connectAndReceive();
            Platform.runLater(() -> {
                if (response.startsWith("ERROR:")) {
                    connection.setText("Server offline — " + response.substring(6));
                    connection.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 12px;");
                } else {
                    connection.setText("Connected · " + response);
                    connection.setStyle("-fx-text-fill: #16a085; -fx-font-size: 12px;");
                }
            });
        });
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    @Override
    public void stop() {
        socketClient.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
