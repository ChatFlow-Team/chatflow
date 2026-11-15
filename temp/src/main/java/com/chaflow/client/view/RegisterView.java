package com.chaflow.client.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.BiConsumer;

public final class RegisterView {
    public record Registration(String phone,String username,String displayName,String password){}
    private final VBox root;
    public RegisterView(BiConsumer<Button,Registration> submit,java.util.function.Consumer<String> validationError){
        TextField name=new TextField();name.setPromptText("Your name");
        TextField username=new TextField();username.setPromptText("username");
        TextField phone=new TextField();phone.setPromptText("+212600000000");
        PasswordField password=new PasswordField();password.setPromptText("At least 8 characters");
        PasswordField confirm=new PasswordField();confirm.setPromptText("Repeat password");
        Button button=LoginView.primary("Create account");
        button.setOnAction(e->{if(!password.getText().equals(confirm.getText())){validationError.accept("Passwords do not match.");return;}submit.accept(button,new Registration(phone.getText(),username.getText(),name.getText(),password.getText()));});
        root=new VBox(9,LoginView.labelled("Display name",name),LoginView.labelled("Username",username),LoginView.labelled("Phone number",phone),LoginView.labelled("Password",password),LoginView.labelled("Confirm password",confirm),button);
        root.setPadding(new Insets(18,22,18,22));
    }
    public VBox root(){return root;}
}
