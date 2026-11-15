package com.chaflow.client.view;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public final class AuthView {
    private final VBox root;
    private final Label status;
    public AuthView(LoginView login,RegisterView register){
        Label logo=new Label("ChaFlow");logo.setFont(Font.font("Arial",34));logo.setStyle("-fx-font-weight:bold;-fx-text-fill:#16a085;");
        Label subtitle=new Label("Connect with your people.");subtitle.setStyle("-fx-text-fill:#64748b;");
        Tab signIn=new Tab("Sign in",login.root()),signUp=new Tab("Create account",register.root());
        signIn.setClosable(false);signUp.setClosable(false);
        TabPane tabs=new TabPane(signIn,signUp);tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);tabs.setMaxWidth(390);
        status=new Label("Start Docker services, then sign in.");status.setWrapText(true);status.setMaxWidth(390);
        root=new VBox(14,logo,subtitle,tabs,status);root.setAlignment(Pos.CENTER);root.setPadding(new javafx.geometry.Insets(30));root.setStyle("-fx-background-color:#f7fafc;");
    }
    public VBox root(){return root;}
    public Label status(){return status;}
}
