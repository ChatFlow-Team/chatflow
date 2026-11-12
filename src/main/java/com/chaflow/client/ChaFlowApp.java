package com.chaflow.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ChaFlowApp extends Application {
    private final SocketClient socketClient = new SocketClient();
    private Label status;
    private VBox root;
    private TabPane tabs;

    @Override
    public void start(Stage stage) {
        Label logo = new Label("ChaFlow");
        logo.setFont(Font.font("Arial", 34));
        logo.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a085;");
        Label subtitle = new Label("Connect with your people.");
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");

        Tab loginTab = new Tab("Sign in", createLoginForm());
        Tab registerTab = new Tab("Create account", createRegistrationForm());
        loginTab.setClosable(false);
        registerTab.setClosable(false);
        tabs = new TabPane(loginTab, registerTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setMaxWidth(430);

        status = new Label("Make sure Docker services are running.");
        status.setWrapText(true);
        status.setMaxWidth(380);
        status.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        root = new VBox(12, logo, subtitle, tabs, status);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f7fafc;");

        stage.setTitle("ChaFlow — Sign in");
        stage.setScene(new Scene(root, 560, 760));
        stage.setMinWidth(460);
        stage.setMinHeight(720);
        stage.show();
    }

    private VBox createLoginForm() {
        return new LoginForm(
                (button, phone, password) -> authenticate(button, () -> socketClient.login(phone, password)),
                () -> tabs.getSelectionModel().select(1));
    }

    private VBox createRegistrationForm() {
        TextField displayName = field("Your name");
        TextField username = field("username");
        TextField phone = field("+212600000000");
        PasswordField password = new PasswordField();
        password.setPromptText("At least 8 characters");
        PasswordField confirmation = new PasswordField();
        confirmation.setPromptText("Repeat password");
        Button submit = primaryButton("Create account");
        submit.setOnAction(event -> {
            if (!password.getText().equals(confirmation.getText())) {
                showStatus("Passwords do not match.", false);
                return;
            }
            authenticate(submit, () -> socketClient.register(phone.getText(), username.getText(), displayName.getText(), password.getText()));
        });
        VBox form = new VBox(9,
                labelled("Display name", displayName), labelled("Username", username),
                labelled("Phone number", phone), labelled("Password", password),
                labelled("Confirm password", confirmation), submit);
        form.setPadding(new Insets(18, 22, 18, 22));
        return form;
    }

    private void authenticate(Button button, AuthCall call) {
        button.setDisable(true);
        showStatus("Contacting ChaFlow server…", true);
        Thread thread = new Thread(() -> {
            SocketClient.AuthResult result = call.run();
            Platform.runLater(() -> {
                button.setDisable(false);
                if (result.successful()) showWelcome(result);
                else showStatus(result.message(), false);
            });
        }, "chaflow-auth-request");
        thread.setDaemon(true);
        thread.start();
    }

    private void showWelcome(SocketClient.AuthResult account) {
        Label heading = new Label("Welcome to ChaFlow, " + account.displayName() + "!");
        heading.setFont(Font.font("Arial", 24));
        heading.setStyle("-fx-text-fill: #16a085; -fx-font-weight: bold;");
        Label detail = new Label("Signed in as @" + account.username() + "\nAccount ID: " + account.userId());
        detail.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");
        Button back = new Button("Back to sign in");
        back.setOnAction(event -> start((Stage) root.getScene().getWindow()));
        root.getChildren().setAll(heading, detail, back);
    }

    private void showStatus(String message, boolean neutral) {
        status.setText(message);
        status.setStyle(neutral
                ? "-fx-text-fill: #64748b; -fx-font-size: 12px;"
                : "-fx-text-fill: #b42318; -fx-font-size: 12px;");
    }

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static VBox labelled(String label, Control control) {
        Label caption = new Label(label);
        caption.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        control.setMaxWidth(Double.MAX_VALUE);
        return new VBox(5, caption, control);
    }

    private static Button primaryButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 11 18 11 18;");
        return button;
    }

    @FunctionalInterface
    private interface AuthCall { SocketClient.AuthResult run(); }

    public static void main(String[] args) { launch(args); }
}
