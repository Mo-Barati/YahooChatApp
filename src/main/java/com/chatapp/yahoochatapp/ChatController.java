package com.chatapp.yahoochatapp;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.control.ListView;

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

        ControllerManager.setChatController(this);

        loadChatHistory();
        loadFriendList();
        loadPendingRequests(); // ✅ Check for pending friend requests

        addFriendButton.setOnAction(event -> promptAddFriend());

        System.out.println("Chat UI initialized successfully!");

        // ✅ Handle double-click event on friend list to open private chat
        friendsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double-click detected
                String selectedFriend = friendsList.getSelectionModel().getSelectedItem();
                if (selectedFriend != null) {
                    selectedFriend = selectedFriend.replace(" (New)", ""); // ✅ Remove notification tag if present
                    if (!selectedFriend.contains("(Pending)") && !selectedFriend.contains("(Requested)")) {
                        openPrivateChat(selectedFriend);
                    }
                }
            }
        });

        // ✅ Automatically check for new messages and update notifications
        startNotificationChecker();
    }

    /**
     * ✅ Starts a background task to check for new messages and update the friend list.
     */
    private void startNotificationChecker() {
        Thread notificationThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // ✅ Check every 5 seconds
                    Platform.runLater(this::updateFriendListNotifications); // ✅ Now supports 🔵 count
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    /**
     * ✅ Updates the friend list to show an unread message notification for each friend
     */
    private void updateFriendListNotifications() {
        for (int i = 0; i < friendsList.getItems().size(); i++) {
            String friend = friendsList.getItems().get(i);
            String friendName = friend.replaceAll(" \\(New\\)| 🔵 \\(\\d+\\)", "").trim(); // ✅ Remove old notifications

            int unreadCount = getUnreadMessageCount(friendName); // ✅ Fetch unread messages count

            if (unreadCount > 0) {
                friendsList.getItems().set(i, friendName + " 🔵 (" + unreadCount + ")");
            } else {
                friendsList.getItems().set(i, friendName); // ✅ Remove notification if no unread messages
            }
        }
    }

    /**
     * ✅ Updates a specific friend's name in the list to show unread messages
     */
    public void updateFriendNotification(String receiver) {
        Platform.runLater(() -> {
            if (friendsList == null) {
                System.err.println("Error: friendsList is not initialized!");
                return;
            }

            for (int i = 0; i < friendsList.getItems().size(); i++) {
                String friend = friendsList.getItems().get(i).trim();

                if (friend.equals(receiver) || friend.startsWith(receiver)) {
                    int unreadCount = getUnreadMessageCount(receiver); // ✅ Fetch updated count
                    friendsList.getItems().set(i, receiver + " 🔵 (" + unreadCount + ")");
                    System.out.println("Updated friend list: " + receiver + " has " + unreadCount + " unread messages.");
                    return;
                }
            }
        });
    }

    /**
     * ✅ Retrieves the number of unread messages for a specific friend
     */
    private int getUnreadMessageCount(String friendUsername) {
        String currentUser = SessionManager.getUser();
        String query = "SELECT COUNT(*) FROM chat_messages WHERE sender = ? AND receiver = ? AND status = 'Delivered'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, friendUsername); // Sender
            pstmt.setString(2, currentUser); // Receiver

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1); // ✅ Return count of unread messages
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0; // Default to no unread messages
    }

    /**
     * ✅ Checks if there are unread messages from a friend.
     */
    private boolean hasUnreadMessages(String friendUsername) {
        String query = "SELECT COUNT(*) FROM chat_messages WHERE sender = ? AND receiver = ? AND status != 'Seen'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, friendUsername);
            pstmt.setString(2, SessionManager.getUser());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // ✅ Returns true if there are unread messages
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ✅ Opens a private chat window with the selected friend and removes the notification
     */
    private void openPrivateChat(String friend) {
        try {
            friend = friend.replaceAll(" 🔵 \\(\\d+\\)", ""); // ✅ Remove notification count

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/yahoochatapp/private-chat-view.fxml"));
            Parent root = loader.load();

            PrivateChatController privateChatController = loader.getController();
            privateChatController.setFriend(friend);

            System.out.println("Calling loadPrivateChat() for: " + friend);
            privateChatController.loadPrivateChat(friend); // ✅ Load chat messages

            privateChatController.startChatAutoRefresh();  // ✅ Start real-time updates

            Stage chatStage = new Stage();
            chatStage.setTitle("Chat with " + friend);
            chatStage.setScene(new Scene(root));
            chatStage.show();

            privateChatController.setChatStage(chatStage); // ✅ Pass stage reference

            // ✅ Remove notification when chat opens
            removeFriendNotification(friend);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ Removes the unread message notification after opening chat
     */
    private void removeFriendNotification(String friend) {
        Platform.runLater(() -> {
            for (int i = 0; i < friendsList.getItems().size(); i++) {
                String listItem = friendsList.getItems().get(i);

                if (listItem.startsWith(friend)) {
                    friendsList.getItems().set(i, friend); // ✅ Reset to default name (remove 🔵)
                    markMessagesAsSeen(friend); // ✅ Mark all messages as "Seen" in DB
                    return;
                }
            }
        });
    }

    /**
     * ✅ Marks messages as "Seen" when chat is opened
     */
    private void markMessagesAsSeen(String sender) {
        String currentUser = SessionManager.getUser();
        String query = "UPDATE chat_messages SET status = 'Seen' WHERE sender = ? AND receiver = ? AND status = 'Delivered'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, currentUser);
            pstmt.executeUpdate();
            System.out.println("Messages from " + sender + " marked as Seen.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
     * ✅ Loads only general (public) chat history from the database and displays it.
     */
    private void loadChatHistory() {
        String query = "SELECT id, sender, content, timestamp FROM public_messages ORDER BY timestamp ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            boolean hasMessages = false;
            chatMessagesList.getItems().clear();

            while (rs.next()) {
                hasMessages = true;

                int messageId = rs.getInt("id");
                String sender = rs.getString("sender");
                String content = rs.getString("content");
                String timestamp = rs.getTimestamp("timestamp").toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm"));

                HBox messageBox = createMessageUI(messageId, sender, content, timestamp, "", false);
                chatMessagesList.getItems().add(messageBox);
            }

            if (!hasMessages) {
                Label placeholderLabel = new Label("No messages yet...");
                placeholderLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-padding: 20px;");
                chatMessagesList.getItems().add(new HBox(placeholderLabel));
            }

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
        // 🖼 Load profile image
        ImageView profileView = new ImageView();
        profileView.setFitWidth(30);
        profileView.setFitHeight(30);
        profileView.setPreserveRatio(true);

        String imagePath = getUserProfileImagePath(sender);
        if (imagePath != null && !imagePath.isEmpty()) {
            profileView.setImage(new Image(imagePath));
            // Make it circular
            Circle clip = new Circle(15, 15, 15);
            profileView.setClip(clip);
        } else {
            System.out.println("⚠️ No profile image found for user: " + sender);
            profileView.setVisible(false);
            profileView.setManaged(false);
        }

        // 👤 Sender label
        Label senderLabel = new Label(sender + ":");
        senderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

        // 🧩 Combine profile and sender name
        HBox headerBox = new HBox(profileView, senderLabel);
        headerBox.setSpacing(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // 💬 Message content
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(250);

        // ⏰ Time label
        Label timeLabel = new Label(timestamp);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        // 👍 Like + 💬 Comment Buttons
        Button likeBtn = new Button("👍");
        likeBtn.setOnAction(e -> handleLike(messageId));

        int likeCount = getLikeCount(messageId);
        Label likeCountLabel = new Label(String.valueOf(likeCount));
        VBox likeBox = new VBox(likeBtn, likeCountLabel);
        likeBox.setAlignment(Pos.CENTER);

        Button commentBtn = new Button("💬");
        commentBtn.setOnAction(e -> promptComment(messageId));

        HBox interactionBar = new HBox(likeBox, commentBtn);
        interactionBar.setSpacing(10);

        // 🗨️ Comments
        List<Label> comments = getComments(messageId);
        VBox commentBox = new VBox();
        commentBox.setSpacing(3);
        commentBox.setPadding(new Insets(5, 0, 0, 10));
        if (!comments.isEmpty()) commentBox.getChildren().addAll(comments);

        // 📦 Message container with everything
        VBox messageContainer = new VBox(headerBox, messageLabel, timeLabel, interactionBar, commentBox);
        messageContainer.setSpacing(5);

        // 👥 Final message bubble
        HBox messageBox = new HBox(messageContainer);
        messageBox.setPadding(new Insets(5, 10, 5, 10));
        messageBox.setAlignment(sender.equals(SessionManager.getUser()) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        return messageBox;
    }

    private void handleLike(int messageId) {
        String sender = SessionManager.getUser();

        String query = "INSERT INTO message_interactions (message_id, user, type) VALUES (?, ?, 'like')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, messageId);
            pstmt.setString(2, sender);
            pstmt.executeUpdate();

            System.out.println("👍 Liked message " + messageId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getLikeCount(int messageId) {
        String query = "SELECT COUNT(*) FROM message_interactions WHERE message_id = ? AND type = 'like'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, messageId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void promptComment(int messageId) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Comment");
        dialog.setHeaderText("Write your comment:");
        dialog.setContentText("Comment:");

        dialog.showAndWait().ifPresent(comment -> {
            if (!comment.trim().isEmpty()) {
                saveComment(messageId, comment);
            }
        });
    }

    private void saveComment(int messageId, String commentText) {
        String sender = SessionManager.getUser();

        String query = "INSERT INTO message_interactions (message_id, user, type, comment_text) VALUES (?, ?, 'comment', ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, messageId);
            pstmt.setString(2, sender);
            pstmt.setString(3, commentText);
            pstmt.executeUpdate();

            System.out.println("💬 Comment added to message " + messageId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<Label> getComments(int messageId) {
        List<Label> commentLabels = new ArrayList<>();
        String query = "SELECT user, comment_text FROM message_interactions WHERE message_id = ? AND type = 'comment' ORDER BY timestamp ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, messageId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String user = rs.getString("user");
                String text = rs.getString("comment_text");

                Label comment = new Label(user + ": " + text);
                comment.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
                comment.setWrapText(true);
                comment.setMaxWidth(220);
                commentLabels.add(comment);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return commentLabels;
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
        String query = "INSERT INTO public_messages (sender, content) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, sender);
            pstmt.setString(2, message);
            pstmt.executeUpdate();

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

    private String getUserProfileImagePath(String username) {
        String query = "SELECT profile_picture FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("profile_picture"); // It’s a full URI like "file:/..."
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
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
