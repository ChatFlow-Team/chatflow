package com.chaflow.client;

import com.chaflow.client.controller.AuthController;
import com.chaflow.client.controller.ChatController;
import com.chaflow.client.controller.MessengerController;
import com.chaflow.client.service.AuthService;
import com.chaflow.client.service.ChatService;
import com.chaflow.client.service.SocketService;
import com.chaflow.client.view.LocalWebView;
import com.chaflow.client.view.MessengerView;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChaFlowApp extends Application {
    private final SocketService socket = new SocketService();
    private final AuthController authController = new AuthController(new AuthService(socket));
    private final MessengerController messengerController = new MessengerController(new ChatService(socket));
    private final ChatController chatController = new ChatController(new ChatService(socket));
    private final ExecutorService io = Executors.newSingleThreadExecutor(action -> {
        Thread thread = new Thread(action, "chatflow-network");
        thread.setDaemon(true);
        return thread;
    });

    private Stage stage;
    private SocketClient.AuthResult account;
    private LocalWebView authPage;
    private MessengerView messengerView;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        showAuth();
        stage.show();
    }

    private void showAuth() {
        showAuthPage("Connexion", new LocalWebView("login.html", new LoginActions()));
    }

    private void showAuthPage(String title, LocalWebView page) {
        if (authPage != null) authPage.detach();
        authPage = page;
        stage.setTitle("ChatFlow — " + title);
        stage.setScene(new Scene(page.root(), 764, 604));
        stage.sizeToScene();
        page.root().requestFocus();
    }

    public final class LoginActions {
        public void login(String phone, String password) {
            LocalWebView page = authPage;
            io.submit(() -> {
                SocketClient.AuthResult result = authController.login(phone, password);
                Platform.runLater(() -> {
                    if (authPage != page) return;
                    if (!result.successful()) {
                        page.call("authFailed", loginError(result.message()));
                        return;
                    }

                    account = result;
                    page.call("authSucceeded", result.displayName());
                    page.call("updateConversationSummary", -1);
                    io.submit(() -> {
                        try {
                            int count = messengerController.conversations(result.token()).size();
                            Platform.runLater(() -> page.call("updateConversationSummary", count));
                        } catch (IOException ignored) {
                            // The welcome screen can still open the messenger after a preview failure.
                        }
                    });
                });
            });
        }

        public void openRegistration() {
            if (account == null) showRegistration();
        }

        public void openMessenger() {
            if (account != null) showMessenger();
        }
    }

    private static String loginError(String message) {
        if ("Incorrect phone number or password.".equals(message)) {
            return "Numéro de téléphone ou mot de passe incorrect.";
        }
        return message;
    }

    private void showRegistration() {
        showAuthPage("Inscription", new LocalWebView("register.html", new RegistrationActions()));
    }

    public final class RegistrationActions {
        public void register(String phone, String username, String displayName, String password) {
            LocalWebView page = authPage;
            io.submit(() -> {
                SocketClient.AuthResult result = authController.register(phone, username, displayName, password);
                Platform.runLater(() -> {
                    if (authPage != page) return;
                    if (result.successful()) {
                        account = result;
                        showMessenger();
                    } else {
                        page.call("registrationFailed", registrationError(result.message()));
                    }
                });
            });
        }

        public void openLogin() {
            showAuth();
        }
    }

    private static String registrationError(String message) {
        return switch (message) {
            case "Phone number or username is already registered." ->
                    "Ce numéro de téléphone ou ce pseudo est déjà utilisé.";
            default -> message;
        };
    }

    private void showMessenger() {
        authPage.detach();
        authPage = null;
        messengerView = new MessengerView(account, authController, messengerController, chatController, io, () -> {
            account = null;
            messengerView = null;
            showAuth();
        });
        stage.setTitle("ChatFlow — Messagerie");
        stage.setScene(new Scene(messengerView.root(), 1216, 680));
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        if (authPage != null) authPage.detach();
        SocketClient.AuthResult previous = account;
        if (previous != null) {
            Runnable closeEvents = messengerView != null ? messengerView.detach() : () -> {};
            io.execute(() -> {
                closeEvents.run();
                authController.logout(previous.token());
            });
        }
        io.shutdown();
    }

    public static void main(String[] args) { launch(args); }
}
