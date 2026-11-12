package com.chaflow.client;

import java.util.Locale;
import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Registration presentation and local validation; TCP and server errors remain in ChaFlowApp. */
final class RegistrationForm extends VBox {
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

    RegistrationForm(SubmitAction onSubmit, Runnable onLogin) {
        setId("registration-form");
        getStylesheets().add(RegistrationForm.class.getResource("registration.css").toExternalForm());
        Label eyebrow = new Label("BIENVENUE CHEZ CHAFLOW");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Créons votre compte");
        title.getStyleClass().add("title");
        title.setWrapText(true);
        Label intro = new Label("Quelques informations pour faire connaissance.\nTous les champs sont obligatoires.");
        intro.getStyleClass().add("muted");
        intro.setWrapText(true);

        Input phone = new Input("phone", "Numéro de téléphone", "+212600000000", "8 à 15 chiffres, avec un + facultatif.", false);
        Input username = new Input("username", "Pseudo", "votre_pseudo", "3 à 32 lettres, chiffres ou tirets bas (_).", false);
        Input displayName = new Input("display-name", "Nom affiché", "Votre nom", "80 caractères maximum.", false);
        Input password = new Input("password", "Mot de passe", "Au moins 8 caractères", null, true);
        Input confirmation = new Input("confirmation", "Confirmer le mot de passe", "Répétez votre mot de passe", null, true);
        Input[] inputs = {phone, username, displayName, password, confirmation};

        Button submit = new Button("Créer un compte");
        submit.setId("registration-submit");
        submit.getStyleClass().add("primary");
        submit.setMaxWidth(Double.MAX_VALUE);
        VBox content = new VBox(10, eyebrow, title, intro);
        content.getStyleClass().add("card");
        for (Input input : inputs) content.getChildren().add(input.box);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(490);
        scroll.setMinHeight(0);
        setMinHeight(0);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        submit.setOnAction(event -> {
            String phoneValue = phone.field.getText().trim();
            String usernameValue = username.field.getText().trim().toLowerCase(Locale.ROOT);
            String nameValue = displayName.field.getText().trim();
            String passwordValue = password.field.getText();
            phone.validate(phoneValue.isEmpty() ? "Renseignez votre numéro de téléphone."
                    : !phoneValue.matches("\\+?[0-9]{8,15}") ? "Utilisez 8 à 15 chiffres, avec un + facultatif." : null);
            username.validate(usernameValue.isEmpty() ? "Choisissez un pseudo."
                    : !usernameValue.matches("[a-z0-9_]{3,32}") ? "Utilisez 3 à 32 lettres, chiffres ou tirets bas (_)." : null);
            displayName.validate(nameValue.isBlank() ? "Renseignez votre nom affiché."
                    : nameValue.length() > 80 ? "Le nom affiché est limité à 80 caractères." : null);
            password.validate(passwordValue.isBlank() ? "Choisissez un mot de passe."
                    : passwordValue.length() < 8 ? "Le mot de passe doit contenir au moins 8 caractères." : null);
            confirmation.validate(confirmation.field.getText().isEmpty() ? "Confirmez votre mot de passe."
                    : !passwordValue.equals(confirmation.field.getText()) ? "Les mots de passe ne correspondent pas." : null);
            for (Input input : inputs) {
                if (input.error.isVisible()) {
                    // Make the first error reachable even when the form needs scrolling.
                    scroll.setVvalue(0);
                    input.field.requestFocus();
                    return;
                }
            }
            // Read controls on the FX thread, preserving the password exactly.
            onSubmit.run(submit, phoneValue, usernameValue, nameValue, passwordValue);
        });
        for (int i = 0; i < inputs.length - 1; i++) {
            TextField next = inputs[i + 1].field;
            inputs[i].field.setOnAction(event -> next.requestFocus());
        }
        confirmation.field.setOnAction(event -> submit.fire());
        Hyperlink login = new Hyperlink("Déjà un compte ? Se connecter");
        login.setId("registration-login");
        login.setWrapText(true);
        login.setOnAction(event -> onLogin.run());
        content.getChildren().addAll(submit, login);
        getChildren().add(scroll);
    }

    private static final class Input {
        final TextField field;
        final Label error = new Label();
        final VBox box;
        final String help;

        Input(String id, String text, String prompt, String hint, boolean secret) {
            field = secret ? new PasswordField() : new TextField();
            field.setId("registration-" + id);
            field.setPromptText(prompt);
            field.setMaxWidth(Double.MAX_VALUE);
            help = hint;
            field.setAccessibleHelp(hint);
            Label caption = new Label(text);
            caption.setLabelFor(field);
            caption.getStyleClass().add("caption");
            error.setId("registration-" + id + "-error");
            error.getStyleClass().add("error");
            error.setWrapText(true);
            error.setLabelFor(field);
            error.setVisible(false);
            error.setManaged(false);
            box = new VBox(4, caption, field);
            if (hint != null) {
                Label detail = new Label(hint);
                detail.getStyleClass().add("muted");
                detail.setWrapText(true);
                box.getChildren().add(detail);
            }
            box.getChildren().add(error);
            field.textProperty().addListener((observable, before, after) -> validate(null));
        }

        void validate(String message) {
            boolean invalid = message != null;
            error.setText(message == null ? "" : message);
            error.setVisible(invalid);
            error.setManaged(invalid);
            field.pseudoClassStateChanged(INVALID, invalid);
            field.setAccessibleHelp(invalid ? message : help);
        }
    }

    @FunctionalInterface
    interface SubmitAction {
        void run(Button button, String phone, String username, String displayName, String password);
    }
}
