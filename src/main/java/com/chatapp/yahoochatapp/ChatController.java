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
     * Shows a typing indicator in the chat.
     */
    private void showTypingIndicator() {
        Label typingLabel = new Label("Bot is typing...");
        typingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray; -fx-padding: 5px;");

        HBox typingBox = new HBox(typingLabel);
        typingBox.setAlignment(Pos.CENTER_LEFT);
        typingBox.setPadding(new Insets(5, 50, 5, 10));

        chatMessagesList.getItems().add(typingBox);

        // Auto-scroll to show typing indicator
        chatMessagesList.scrollTo(chatMessagesList.getItems().size() - 1);

        // Schedule the removal after 1 second (before bot response)
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(event -> {
            chatMessagesList.getItems().remove(typingBox);
            simulateBotResponse(); // Trigger the bot response after the delay
        });
        delay.play();
    }


    /**
     * Retrieves chat history from the database and displays it in the ListView.
     */
    private void loadChatHistory() {
        String query = "SELECT id, sender, message, timestamp, status FROM chat_messages ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int messageId = rs.getInt("id");
                String sender = rs.getString("sender");
                String message = rs.getString("message");
                String timestamp = rs.getString("timestamp");
                String status = rs.getString("status");

                Label messageLabel = new Label(message);
                messageLabel.setWrapText(true);
                messageLabel.setMaxWidth(250);

                Label timeLabel = new Label(timestamp);
                timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
                timeLabel.setAlignment(Pos.BOTTOM_RIGHT);

                // ✅ Add status label
                Label statusLabel = new Label(status);
                statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

                VBox messageContainer = new VBox(messageLabel, timeLabel, statusLabel);
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

                // ✅ If message is "Delivered", update it to "Seen"
                if (!"Seen".equals(status)) {
                    markMessageAsSeen(messageId);
                }
            }

            // Auto-scroll to the latest message
            chatMessagesList.scrollTo(chatMessagesList.getItems().size() - 1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ Marks a message as "Seen" in the database.
     */
    private void markMessageAsSeen(int messageId) {
        String updateQuery = "UPDATE chat_messages SET status = 'Seen' WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            pstmt.setInt(1, messageId);
            pstmt.executeUpdate();
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

            // Clear input field
            messageField.clear();

            // ✅ Show typing indicator before bot response
            showTypingIndicator();
        }
    }


    /**
     * Saves a chat message to the database.
     */
    private void saveMessageToDatabase(String sender, String message) {
        String query = "INSERT INTO chat_messages (sender, message, status) VALUES (?, ?, 'Sent')";

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
            String botMessage = "Hi there! 😊";
            Label botLabel = new Label(botMessage);
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

            // ✅ Save bot message to database
            saveMessageToDatabase("Bot", botMessage);

            // ✅ Auto-scroll to latest message
            chatMessagesList.scrollTo(chatMessagesList.getItems().size() - 1);
        });
        delay.play();
    }



}
