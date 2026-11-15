package com.chaflow.client.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public final class ChatView {
    private final VBox root=new VBox(0);
    private final Label title=new Label("Select a conversation");
    private final VBox messages=new VBox(10);
    private final TextField input=new TextField();
    public ChatView(Runnable send){
        root.setPadding(new Insets(18));
        title.setFont(Font.font(20));title.setStyle("-fx-font-weight:bold;");
        messages.setPadding(new Insets(16));
        ScrollPane history=new ScrollPane(messages);history.setFitToWidth(true);history.setStyle("-fx-background:transparent;-fx-background-color:transparent;");VBox.setVgrow(history,Priority.ALWAYS);
        input.setPromptText("Write a message...");
        HBox.setHgrow(input,Priority.ALWAYS);
        Button button=new Button("Send");
        button.setStyle("-fx-background-color:#16a085;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:9 15 9 15;");
        button.setOnAction(e->send.run());input.setOnAction(e->send.run());
        HBox composer=new HBox(8,input,button);composer.setAlignment(Pos.CENTER);
        root.getChildren().addAll(title,new Separator(),history,composer);
    }
    public VBox root(){return root;}
    public Label title(){return title;}
    public VBox messages(){return messages;}
    public TextField input(){return input;}
}
