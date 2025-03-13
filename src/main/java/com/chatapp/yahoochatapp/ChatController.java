package com.chatapp.yahoochatapp;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
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
    private ListView<String> friendsList; // ✅ Added Friend List UI

    @FXML
    private TextArea messageField;

    @FXML
    private Button sendButton;

    @FXML
    private Button addFriendButton; // ✅ Button to add friends



    @FXML
    private void initialize() {
        // ✅ Ensure all UI components are properly initialized
        if (messageField == null || sendButton == null || friendsList == null || addFriendButton == null) {
            System.err.println("UI components are not properly initialized!");
            return;
        }

        // ✅ Set padding for input area
        HBox inputArea = (HBox) messageField.getParent();
        if (inputArea != null) {
            inputArea.setPadding(new Insets(10));
        }

        // ✅ Set send button icon with error handling
        try {
            Image sendImage = new Image(getClass().getResource("/com/chatapp/yahoochatapp/icons/send_icon.png").toExternalForm());
            ImageView sendIcon = new ImageView(sendImage);
            sendIcon.setFitWidth(20);
            sendIcon.setFitHeight(20);
            sendButton.setGraphic(sendIcon);
            sendButton.setText(null);
        } catch (Exception e) {
            System.err.println("Error loading send icon: " + e.getMessage());
        }

        // ✅ Handle Enter Key events in TextArea
        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    messageField.appendText("\n"); // Move to the next line
                } else {
                    event.consume(); // Prevent default behavior (message sending)
                }
            }
        });

        // ✅ Load chat history
        loadChatHistory();

        // ✅ Load friends list
        loadFriendList();

        // ✅ Set action for adding friends
        addFriendButton.setOnAction(event -> promptAddFriend());

        // ✅ Print success message (for debugging)
        System.out.println("Chat UI initialized successfully!");
    }



    /**
     * ✅ Loads the friend list from the database
     */
    private void loadFriendList() {
        String query = "SELECT friend FROM friends WHERE user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, SessionManager.getUser());
            ResultSet rs = pstmt.executeQuery();

            friendsList.getItems().clear(); // Clear before adding new ones

            boolean hasFriends = false;
            while (rs.next()) {
                String friend = rs.getString("friend");
                friendsList.getItems().add(friend);
                hasFriends = true;
                System.out.println("Loaded Friend: " + friend);
            }

            if (!hasFriends) {
                System.out.println("No friends found in the database.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ Opens a dialog to add a new friend
     */
    private void promptAddFriend() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Friend");
        dialog.setHeaderText("Enter your friend's username:");
        dialog.setContentText("Username:");

        dialog.showAndWait().ifPresent(friendUsername -> {
            if (!friendUsername.trim().isEmpty()) {
                addFriend(friendUsername.trim());
            }
        });
    }

    /**
     * ✅ Adds a friend request to the database
     */
    private void addFriend(String friendUsername) {
        String query = "INSERT INTO friends (user, friend, status) VALUES (?, ?, 'Pending')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, SessionManager.getUser());
            pstmt.setString(2, friendUsername);

            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("Friend request sent!");
            } else {
                System.out.println("Failed to send friend request.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ Removes a friend from the database
     */
    private void removeFriend(String friendUsername) {
        String query = "DELETE FROM friends WHERE user = ? AND friend = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, SessionManager.getUser());
            pstmt.setString(2, friendUsername);

            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println(friendUsername + " removed from friend list.");
                loadFriendList(); // Refresh friend list
            } else {
                System.out.println("Failed to remove friend.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddFriend() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Friend");
        dialog.setHeaderText("Enter friend's username:");
        dialog.setContentText("Username:");

        dialog.showAndWait().ifPresent(friend -> {
            if (!friend.trim().isEmpty()) {
                addFriendToDatabase(friend);
                loadFriendList(); // Refresh the list
            }
        });
    }

    private void addFriendToDatabase(String friendUsername) {
        String checkQuery = "SELECT * FROM friends WHERE user = ? AND friend = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

            checkStmt.setString(1, SessionManager.getUser());
            checkStmt.setString(2, friendUsername);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                System.out.println("Friend already exists: " + friendUsername);
                return; // Stop execution if friend already exists
            }

            // If friend doesn't exist, insert them
            String insertQuery = "INSERT INTO friends (user, friend, status) VALUES (?, ?, 'Accepted')";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, SessionManager.getUser());
                insertStmt.setString(2, friendUsername);
                int rowsInserted = insertStmt.executeUpdate();

                if (rowsInserted > 0) {
                    System.out.println("Friend added successfully: " + friendUsername);
                    loadFriendList(); // Refresh the list
                } else {
                    System.out.println("Failed to add friend.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeFriendFromDatabase(String friend) {
        String currentUser = SessionManager.getUser();
        String query = "DELETE FROM friends WHERE user = ? AND friend = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, currentUser);
            pstmt.setString(2, friend);
            pstmt.executeUpdate();
            System.out.println(friend + " removed from friend list.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ Accepts a pending friend request
     */
    private void acceptFriendRequest(String friendUsername) {
        String query = "UPDATE friends SET status = 'Accepted' WHERE user = ? AND friend = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, SessionManager.getUser());
            pstmt.setString(2, friendUsername);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Friend request accepted.");
                loadFriendList(); // Refresh friend list
            } else {
                System.out.println("Failed to accept friend request.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ Loads pending friend requests
     */
    private void loadPendingRequests() {
        String query = "SELECT user FROM friends WHERE friend = ? AND status = 'Pending'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, SessionManager.getUser());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String requester = rs.getString("user");
                System.out.println("Pending request from: " + requester);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ Displays a context menu for removing a friend
     */
    private void showFriendContextMenu(String friendUsername) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem removeItem = new MenuItem("Remove Friend");
        removeItem.setOnAction(event -> removeFriend(friendUsername));

        contextMenu.getItems().add(removeItem);

        friendsList.setOnContextMenuRequested(event -> {
            contextMenu.show(friendsList, event.getScreenX(), event.getScreenY());
        });
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
     * ✅ Loads chat history from the database and displays it in the ListView.
     */
    private void loadChatHistory() {
        String query = "SELECT id, sender, message, timestamp, status FROM chat_messages ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            boolean hasMessages = false; // To track if there are messages

            while (rs.next()) {
                hasMessages = true; // At least one message exists

                int messageId = rs.getInt("id"); // ✅ Get message ID
                String sender = rs.getString("sender");
                String message = rs.getString("message");
                String timestamp = rs.getString("timestamp");
                String status = rs.getString("status");

                // ✅ FIX: Correct method call (ensure `allowEdit` is a boolean)
                HBox messageBox = createMessageUI(messageId, sender, message, timestamp, status, true);

                // ✅ Attach context menu with message ID
                addContextMenu(messageBox, messageId, sender, message,
                        (Label) ((VBox) messageBox.getChildren().get(0)).getChildren().get(0)
                );

                chatMessagesList.getItems().add(messageBox);

                // ✅ If message is "Delivered", update it to "Seen"
                if (!"Seen".equals(status)) {
                    markMessageAsSeen(messageId);
                }
            }

            // ✅ If no messages exist, show a placeholder
            if (!hasMessages) {
                Label placeholderLabel = new Label("No messages yet...");
                placeholderLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20px;");
                chatMessagesList.getItems().add(new HBox(placeholderLabel));
            }

            // ✅ Auto-scroll to the latest message
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
            String sender = SessionManager.getUser();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String timestamp = now.format(formatter);

            // Create message UI (Pass -1 as a placeholder for the message ID)
            HBox messageBox = createMessageUI(-1, sender, message, timestamp, "Sent", true);

            chatMessagesList.getItems().add(messageBox);

            // Auto-scroll to the latest message
            chatMessagesList.scrollTo(chatMessagesList.getItems().size() - 1);

            // Save message to the database
            saveMessageToDatabase(sender, message);

            // Clear input field
            messageField.clear();

            // Show typing indicator before bot response
            showTypingIndicator();
        }
    }


    /**
     * ✅ Adds right-click menu for editing and deleting messages
     */
    private void addContextMenu(HBox messageBox, int messageId, String sender, String message, Label messageLabel) {
        ContextMenu contextMenu = new ContextMenu();

        // Edit Option
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(event -> editMessage(sender, message, messageLabel));

        // Delete Option
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(event -> deleteMessage(messageId, messageBox));

        contextMenu.getItems().addAll(editItem, deleteItem);

        // ✅ Attach to message box
        messageBox.setOnContextMenuRequested(event -> {
            System.out.println("Right-click detected on message.");
            contextMenu.show(messageBox, event.getScreenX(), event.getScreenY());
        });
    }

    /**
     * ✅ Creates the UI for each message and attaches the context menu
     */
    private HBox createMessageUI(int messageId, String sender, String message, String timestamp, String status, boolean allowEdit) {
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(250);

        Label timeLabel = new Label(timestamp);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        timeLabel.setAlignment(Pos.BOTTOM_RIGHT);

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

            if (allowEdit) {
                // ✅ Attach context menu with messageId
                addContextMenu(messageBox, messageId, sender, message, messageLabel);
            }
        }

        return messageBox;
    }


    /**
     * ✅ Allows users to edit their message
     */
    private void editMessage(String sender, String oldMessage, Label messageLabel) {
        TextInputDialog dialog = new TextInputDialog(oldMessage);
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Edit your message:");
        dialog.setContentText("Message:");

        dialog.showAndWait().ifPresent(newMessage -> {
            if (!newMessage.trim().isEmpty()) {
                updateMessageInDatabase(sender, oldMessage, newMessage);
                messageLabel.setText(newMessage);
            }
        });
    }

    /**
     * ✅ Deletes a message from the UI and database
     */
    private void deleteMessage(int messageId, HBox messageBox) {
        deleteMessageFromDatabase(messageId); // Remove from DB first
        chatMessagesList.getItems().remove(messageBox); // Remove from UI
    }

    /**
     * ✅ Updates a message in the database
     */
    private void updateMessageInDatabase(String sender, String oldMessage, String newMessage) {
        String query = "UPDATE chat_messages SET message = ? WHERE sender = ? AND message = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newMessage);
            pstmt.setString(2, sender);
            pstmt.setString(3, oldMessage);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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

    /**
     * ✅ Deletes a specific message from the database.
     */
    private void deleteMessageFromDatabase(int messageId) {
        String query = "DELETE FROM chat_messages WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, messageId);
            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("Message deleted from database.");
            } else {
                System.out.println("Failed to delete message from database.");
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
