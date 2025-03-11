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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        // Set padding for the input area manually
        HBox inputArea = (HBox) messageField.getParent();
        if (inputArea != null) {
            inputArea.setPadding(new Insets(10, 10, 10, 10));
        }

        // Set send button icon
        ImageView sendIcon = new ImageView(new Image(getClass().getResource("/com/chatapp/yahoochatapp/icons/send_icon.png").toExternalForm()));
        sendIcon.setFitWidth(20);
        sendIcon.setFitHeight(20);
        sendButton.setGraphic(sendIcon);
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

        // ✅ Load chat history from the database
        loadChatHistory();
    }

    /**
     * Retrieves chat history from the database and displays it in the ListView.
     */
    private void loadChatHistory() {
        String query = "SELECT sender, message, timestamp FROM chat_messages ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String sender = rs.getString("sender");
                String message = rs.getString("message");
                String timestamp = rs.getString("timestamp");

                Label messageLabel = new Label(message);
                messageLabel.setWrapText(true);
                messageLabel.setMaxWidth(250);

                Label timeLabel = new Label(timestamp);
                timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
                timeLabel.setAlignment(Pos.BOTTOM_RIGHT);

                VBox messageContainer = new VBox(messageLabel, timeLabel);

                HBox messageBox = new HBox(messageContainer);

                if ("Bot".equals(sender)) {
                    // Bot messages (align left)
                    messageLabel.setStyle("-fx-background-color: #E5E5EA; -fx-text-fill: black; -fx-padding: 10px; -fx-background-radius: 10;");
                    messageContainer.setAlignment(Pos.BOTTOM_LEFT);
                    messageBox.setAlignment(Pos.CENTER_LEFT);
                    messageBox.setPadding(new Insets(5, 50, 5, 10));
                } else {
                    // User messages (align right)
                    messageLabel.setStyle("-fx-background-color: #0078FF; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 10;");
                    messageContainer.setAlignment(Pos.BOTTOM_RIGHT);
                    messageBox.setAlignment(Pos.CENTER_RIGHT);
                    messageBox.setPadding(new Insets(5, 10, 5, 50));
                }

                chatMessagesList.getItems().add(messageBox);
            }

            // Auto-scroll to the latest message
            chatMessagesList.scrollTo(chatMessagesList.getItems().size() - 1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }








    @FXML
    private void handleSendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // Get the current user (assume username is stored in SessionManager)
            String sender = SessionManager.getUser();

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

            // Auto-scroll to the latest message
            chatMessagesList.scrollTo(chatMessagesList.getItems().size() - 1);

            // Save message to the database ✅
            saveMessageToDatabase(sender, message);

            // Simulate bot response
            simulateBotResponse();

            // Clear input field
            messageField.clear();
        }
    }

    /**
     * Saves a chat message to the database.
     */
    private void saveMessageToDatabase(String sender, String message) {
        String query = "INSERT INTO chat_messages (sender, message) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, sender);
            pstmt.setString(2, message);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Message saved successfully.");
            } else {
                System.out.println("Failed to save message.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
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
            botLabel.setMaxWidth(250);

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

            // ✅ Save bot response to the database
            saveMessageToDatabase("Bot", "Hi there! 😊");
        });
        delay.play();
    }

}
