package com.chaflow.client.view;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/** Main messenger layout: navigation/sidebar, active chat and application status. */
public final class MessengerView {
    private final BorderPane root=new BorderPane();
    public MessengerView(VBox sidebar,ChatView chat,Label status){
        root.setStyle("-fx-background-color:#f4f7f9;");
        root.setLeft(sidebar);root.setCenter(chat.root());
        status.setPadding(new Insets(5,12,5,12));status.setStyle("-fx-text-fill:#64748b;");
        root.setBottom(status);
    }
    public BorderPane root(){return root;}
}
