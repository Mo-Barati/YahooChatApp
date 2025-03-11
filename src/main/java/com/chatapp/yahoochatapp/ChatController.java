package com.chatapp.yahoochatapp;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class ChatController {

    @FXML
    private ListView<Node> chatMessagesList;

    @FXML
    private TextArea messageField;



    @FXML
    private void initialize() {
        // Set padding for the input area manually (Keep this if needed)
        HBox inputArea = (HBox) messageField.getParent();
        if (inputArea != null) {
            inputArea.setPadding(new Insets(10, 10, 10, 10));
        }

        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    // If Shift + Enter is pressed, go to the next line
                    messageField.appendText("\n");
                } else {
                    // If only Enter is pressed, do nothing (don't send)
                    event.consume();
                }
            }
        });


    }


    @FXML
    private void handleSendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // Create a label for the message
            Label messageLabel = new Label("You: " + message);
            messageLabel.setStyle("-fx-background-color: #0078FF; "
                    + "-fx-text-fill: white; "
                    + "-fx-padding: 10px; "
                    + "-fx-background-radius: 10;");

            // Wrap the label in an HBox to style and align
            HBox messageContainer = new HBox(messageLabel);
            messageContainer.setAlignment(Pos.CENTER_RIGHT);
            messageContainer.setPadding(new Insets(5, 10, 5, 50));

            // ✅ Add HBox instead of a string
            chatMessagesList.getItems().add(messageContainer);

            messageField.clear();

            // Simulate a received message after a short delay
            simulateReceivedMessage();
        }
    }


    /**
     * Simulates receiving a message from the chat.
     */
    private void simulateReceivedMessage() {
        PauseTransition delay = new PauseTransition(Duration.seconds(1)); // Delay of 1 second
        delay.setOnFinished(event -> {
            Label receivedMessageLabel = new Label("Bot: Hi there! 😊");
            receivedMessageLabel.setStyle("-fx-background-color: #E5E5E5; "
                    + "-fx-text-fill: black; "
                    + "-fx-padding: 10px; "
                    + "-fx-background-radius: 10;");

            // Wrap received message in HBox and align to the left
            HBox receivedMessageContainer = new HBox(receivedMessageLabel);
            receivedMessageContainer.setAlignment(Pos.CENTER_LEFT);
            receivedMessageContainer.setPadding(new Insets(5, 50, 5, 10));

            // Add received message to chat
            chatMessagesList.getItems().add(receivedMessageContainer);
        });
        delay.play();
    }

}
