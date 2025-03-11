package com.chatapp.yahoochatapp;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatController {

    @FXML
    private ListView<Node> chatMessagesList;

    @FXML
    private TextArea messageField;

    @FXML
    private Button sendButton;



    @FXML
    private void initialize() {
        // Set padding for the input area manually (Keep this if needed)
        HBox inputArea = (HBox) messageField.getParent();
        if (inputArea != null) {
            inputArea.setPadding(new Insets(10, 10, 10, 10));
        }

        // Set send button icon
        ImageView sendIcon = new ImageView(new Image(getClass().getResource("/com/chatapp/yahoochatapp/icons/send_icon.png").toExternalForm()));
        sendIcon.setFitWidth(20);
        sendIcon.setFitHeight(20);
        sendButton.setGraphic(sendIcon);  // Set the icon on the button
        sendButton.setText(null); // Remove the default text "Send"

        // Handle Enter Key events in TextArea
        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    // If Shift + Enter is pressed, go to the next line
                    messageField.appendText("\n");
                } else {
                    // If only Enter is pressed, do nothing (don't send message)
                    event.consume();
                }
            }
        });
    }





    @FXML
    private void handleSendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // Get current timestamp
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String timestamp = now.format(formatter);

            // Create message text
            Label messageLabel = new Label(message);
            messageLabel.setStyle("-fx-background-color: #0078FF; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 10;");
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(250); // Limit width for readability

            // Create timestamp label (small, bottom-right)
            Label timeLabel = new Label(timestamp);
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
            timeLabel.setAlignment(Pos.BOTTOM_RIGHT);

            // Place message and time in a VBox
            VBox messageContainer = new VBox(messageLabel, timeLabel);
            messageContainer.setAlignment(Pos.BOTTOM_RIGHT);

            // Place VBox in HBox for alignment
            HBox messageBox = new HBox(messageContainer);
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.setPadding(new Insets(5, 10, 5, 50));

            // Add to ListView
            chatMessagesList.getItems().add(messageBox);

            // Simulate bot response
            simulateBotResponse();

            messageField.clear(); // Clear input field
        }
    }


    private void simulateBotResponse() {
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(event -> {
            // Get current timestamp
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String timestamp = now.format(formatter);

            // Create bot message
            Label botLabel = new Label("Hi there! 😊");
            botLabel.setStyle("-fx-background-color: #E5E5EA; -fx-text-fill: black; -fx-padding: 10px; -fx-background-radius: 10;");
            botLabel.setWrapText(true);
            botLabel.setMaxWidth(250); // Limit width

            // Create timestamp label
            Label timeLabel = new Label(timestamp);
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
            timeLabel.setAlignment(Pos.BOTTOM_RIGHT);

            // Place bot message and time in a VBox
            VBox botContainer = new VBox(botLabel, timeLabel);
            botContainer.setAlignment(Pos.BOTTOM_LEFT);

            // Place VBox in HBox for alignment
            HBox botMessageBox = new HBox(botContainer);
            botMessageBox.setAlignment(Pos.CENTER_LEFT);
            botMessageBox.setPadding(new Insets(5, 50, 5, 10));

            // Add bot response to ListView
            chatMessagesList.getItems().add(botMessageBox);
        });
        delay.play();
    }
}
