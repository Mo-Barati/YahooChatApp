package com.chatapp.yahoochatapp;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;

import java.awt.*;

public class ChatController {

    @FXML
    private ListView<String> chatMessagesList;

    @FXML
    private TextField messageField;

    @FXML
    private void initialize() {
        // Set padding for the input area manually (Keep this if needed)
        HBox inputArea = (HBox) messageField.getParent();
        if (inputArea != null) {
            inputArea.setPadding(new Insets(10, 10, 10, 10));
        }

        // ✅ Listen for the ENTER key press
        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSendMessage(); // Call send message function
            }
        });
    }


    @FXML
    private void handleSendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            chatMessagesList.getItems().add("You: " + message);
            messageField.clear();
        }
    }
}
