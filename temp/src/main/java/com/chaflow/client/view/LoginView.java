package com.chaflow.client.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.BiConsumer;

public final class LoginView {
    public record Credentials(String phone,String password){}
    private final VBox root;
    public LoginView(BiConsumer<Button,Credentials> submit){
        TextField phone=new TextField();phone.setPromptText("Phone number (+212...)");
        PasswordField password=new PasswordField();password.setPromptText("Password");
        Button button=primary("Sign in");
        button.setOnAction(e->submit.accept(button,new Credentials(phone.getText(),password.getText())));
        root=new VBox(12,labelled("Phone number",phone),labelled("Password",password),button);
        root.setPadding(new Insets(22));
    }
    public VBox root(){return root;}
    static VBox labelled(String name,Control c){Label l=new Label(name);l.setStyle("-fx-font-size:12px;-fx-font-weight:bold;");return new VBox(5,l,c);}
    static Button primary(String text){Button b=new Button(text);b.setStyle("-fx-background-color:#16a085;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:9 15 9 15;");return b;}
}
