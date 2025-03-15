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
import java.util.HashSet;
import java.util.Set;

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
        if (messageField == null || sendButton == null || friendsList == null || addFriendButton == null) {
            System.err.println("UI components are not properly initialized!");
            return;
        }

        HBox inputArea = (HBox) messageField.getParent();
        if (inputArea != null) {
            inputArea.setPadding(new Insets(10));
        }

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

        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    messageField.appendText("\n");
                } else {
                    event.consume();
                }
            }
        });

        loadChatHistory();
        loadFriendList();
        loadPendingRequests(); // ✅ Check for pending friend requests

        addFriendButton.setOnAction(event -> promptAddFriend());

        System.out.println("Chat UI initialized successfully!");
    }

    /**
     * ✅ Loads the user's friend list from the database and marks outgoing friend requests.
     */
    private void loadFriendList() {
        String currentUser = SessionManager.getUser();
        String query = "SELECT user, friend, status FROM friends WHERE user = ? OR friend = ? ORDER BY friend";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, currentUser);
            pstmt.setString(2, currentUser);
            ResultSet rs = pstmt.executeQuery();

            Set<String> uniqueFriends = new HashSet<>();
            friendsList.getItems().clear(); // ✅ Clear old list

            while (rs.next()) {
                String user = rs.getString("user");
                String friend = rs.getString("friend");
                String status = rs.getString("status");

                String displayName;
                if (user.equals(currentUser)) {
                    // ✅ If the logged-in user sent the request, show "Requested"
                    displayName = isPendingRequest(currentUser, friend) ? friend + " (Requested)" : friend;
                } else {
                    // ✅ If the logged-in user received the request, show "Pending Request"
                    displayName = isPendingRequest(user, currentUser) ? user + " (Pending Request)" : user;
                }

                if (!uniqueFriends.contains(displayName)) {
                    uniqueFriends.add(displayName);
                    friendsList.getItems().add(displayName);
                }
            }

            // ✅ Attach the context menu for right-click options
            friendsList.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(String friend, boolean empty) {
                    super.updateItem(friend, empty);
                    if (empty || friend == null) {
                        setText(null);
                        setContextMenu(null);
                    } else {
                        setText(friend);
                        showFriendContextMenu(this, friend); // ✅ Attach context menu
                    }
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    /**
     * ✅ Prompts the user to enter a friend's username and sends a friend request.
     */
    private void promptAddFriend() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Friend");
        dialog.setHeaderText("Enter the username of the friend you want to add:");
        dialog.setContentText("Username:");

        dialog.showAndWait().ifPresent(friendUsername -> {
            if (!friendUsername.trim().isEmpty() && !friendUsername.equals(SessionManager.getUser())) {
                sendFriendRequest(friendUsername);
                loadFriendList(); // ✅ Refresh list immediately after adding
            } else {
                System.out.println("Invalid username.");
            }
        });
    }

    /**
     * ✅ Sends a friend request to another user and updates UI immediately.
     */
    private void sendFriendRequest(String friendUsername) {
        String query = "INSERT INTO friends (user, friend, status) VALUES (?, ?, 'Pending')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, SessionManager.getUser());
            pstmt.setString(2, friendUsername);
            pstmt.executeUpdate();

            System.out.println("Friend request sent to " + friendUsername);
            loadFriendList(); // ✅ Refresh UI immediately

        } catch (SQLException e) {
            e.printStackTrace();
        }
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
    private void handleRemoveFriend() {
        String selectedFriend = friendsList.getSelectionModel().getSelectedItem();

        if (selectedFriend == null) {
            showAlert("No Friend Selected", "Please select a friend to remove.");
            return;
        }

        // Confirm before deleting
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Are you sure you want to remove " + selectedFriend + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                removeFriend(selectedFriend);
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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



    /**
     * ✅ Accepts a pending friend request.
     */
    private void acceptFriendRequest(String requester) {
        String currentUser = SessionManager.getUser();
        String query = "UPDATE friends SET status = 'Accepted' WHERE user = ? AND friend = ? AND status = 'Pending'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, requester);
            pstmt.setString(2, currentUser);
            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Friend request from " + requester + " accepted.");
                loadFriendList(); // ✅ Refresh the friend list after accepting the request
            } else {
                System.out.println("Failed to accept friend request.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ Loads pending friend requests for the logged-in user.
     */
    private void loadPendingRequests() {
        String currentUser = SessionManager.getUser();
        String query = "SELECT user FROM friends WHERE friend = ? AND status = 'Pending'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, currentUser);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String requester = rs.getString("user");

                // Show notification or highlight the pending request
                System.out.println("You have a pending friend request from " + requester);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ Shows a right-click context menu for managing friends.
     */
    private void showFriendContextMenu(ListCell<String> cell, String friend) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem removeItem = new MenuItem("Remove Friend");
        removeItem.setOnAction(event -> removeFriend(friend));

        contextMenu.getItems().add(removeItem);

        // ✅ If the friend request is pending, show an "Accept" option
        if (friend.contains("(Pending Request)")) {
            String requester = friend.replace(" (Pending Request)", ""); // Extract username
            MenuItem acceptItem = new MenuItem("Accept Friend Request");
            acceptItem.setOnAction(event -> acceptFriendRequest(requester));
            contextMenu.getItems().add(acceptItem);
        }

        cell.setContextMenu(contextMenu);
    }

    /**
     * ✅ Checks if a friend request is still pending.
     */
    private boolean isPendingRequest(String user, String friend) {
        String query = "SELECT status FROM friends WHERE user = ? AND friend = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, user);
            pstmt.setString(2, friend);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return "Pending".equals(rs.getString("status")); // ✅ Return true if request is still pending
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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
