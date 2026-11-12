package com.chaflow.client;

import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/** Native JavaFX sign-in view; the application retains responsibility for TCP authentication. */
final class LoginForm extends VBox {
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

    LoginForm(SubmitAction onSubmit, Runnable onRegister) {
        setId("login-form");
        setSpacing(14);
        getStylesheets().add(LoginForm.class.getResource("login.css").toExternalForm());

        Label eyebrow = new Label("VOTRE ESPACE DE DISCUSSION");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Heureux de vous retrouver");
        title.getStyleClass().add("title");
        title.setWrapText(true);
        Label intro = new Label("Connectez-vous pour retrouver vos proches.");
        intro.getStyleClass().add("muted");
        intro.setWrapText(true);

        TextField phone = new TextField();
        phone.setId("login-phone");
        phone.setPromptText("+212600000001");
        PasswordField password = new PasswordField();
        password.setId("login-password");
        password.setPromptText("Votre mot de passe");
        Label error = new Label();
        error.setId("login-error");
        error.getStyleClass().add("error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        for (TextField field : new TextField[]{phone, password}) {
            field.textProperty().addListener((observable, oldValue, newValue) -> {
                field.pseudoClassStateChanged(INVALID, false);
                error.setVisible(false);
                error.setManaged(false);
            });
        }

        Button submit = new Button("Se connecter");
        submit.setId("login-submit");
        submit.getStyleClass().add("primary");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(event -> {
            boolean missingPhone = phone.getText().isBlank();
            boolean missingPassword = password.getText().isBlank();
            phone.pseudoClassStateChanged(INVALID, missingPhone);
            password.pseudoClassStateChanged(INVALID, missingPassword);
            if (missingPhone || missingPassword) {
                error.setText(missingPhone ? "Renseignez votre numéro de téléphone." : "Renseignez votre mot de passe.");
                error.setVisible(true);
                error.setManaged(true);
                (missingPhone ? phone : password).requestFocus();
                return;
            }
            // Capture values on the FX thread before the application starts its TCP worker.
            onSubmit.run(submit, phone.getText().trim(), password.getText());
        });
        phone.setOnAction(event -> password.requestFocus());
        password.setOnAction(event -> submit.fire());
        Label hint = new Label("Utilisez le numéro associé à votre compte.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        Hyperlink register = new Hyperlink("Pas encore de compte ? Créer un compte");
        register.setId("login-register");
        register.setWrapText(true);
        register.setOnAction(event -> onRegister.run());
        getChildren().addAll(eyebrow, title, intro, labelled("Numéro de téléphone", phone),
                labelled("Mot de passe", password), hint, error, submit, register);
    }

    private static VBox labelled(String text, Control input) {
        Label label = new Label(text);
        label.setLabelFor(input);
        label.getStyleClass().add("caption");
        input.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, label, input);
    }

    @FunctionalInterface
    interface SubmitAction {
        void run(Button button, String phone, String password);
    }
}
