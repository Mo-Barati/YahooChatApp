package com.chatapp.yahoochatapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrivateChatController {

    @FXML private ListView<String> privateChatMessages;
    @FXML private TextArea privateMessageField;
    @FXML private Button sendPrivateMessageButton;
    @FXML
    private ListView<String> friendsList; // ✅ Ensure this exists in your class


    private String currentUser;
    private String friendUsername;
    private Stage chatStage; // ✅ Store reference to chat window stage

    public void setChatStage(Stage stage) {
        this.chatStage = stage;
    }


    public void setFriend(String friend) {
        this.friendUsername = friend;
        loadPrivateChat(friendUsername);
    }

    public void loadPrivateChat(String friendUsername) {
        String currentUser = SessionManager.getUser();
        friendUsername = friendUsername.replaceAll(" 🔵 \\(\\d+\\)", ""); // ✅ Remove badge before fetching

        System.out.println("Loading chat between " + currentUser + " and " + friendUsername);

        String query = "SELECT sender, message FROM chat_messages " +
                "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY id ASC";

        List<String> messages = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, currentUser);
            pstmt.setString(2, friendUsername);
            pstmt.setString(3, friendUsername);
            pstmt.setString(4, currentUser);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String sender = rs.getString("sender");
                    String message = rs.getString("message");

                    System.out.println("Fetched message (before UI update): " + sender + " -> " + message);
                    messages.add(sender + ": " + message);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // ✅ Ensure UI updates correctly
        Platform.runLater(() -> {
            System.out.println("Updating UI with messages..."); // Debugging UI update

            privateChatMessages.getItems().setAll(messages); // ✅ Force ListView update
            privateChatMessages.refresh(); // ✅ Ensure UI refresh
        });
    }

    @FXML
    private void handleSendPrivateMessage() {
        String message = privateMessageField.getText().trim();
        if (message.isEmpty()) {
            System.err.println("Cannot send an empty message.");
            return;
        }

        // ✅ Ensure sender and receiver are correctly assigned
        String sender = SessionManager.getUser(); // Ensure this method returns the correct username
        if (sender == null) {
            System.err.println("Error: Current user (sender) is null!");
            return;
        }

        if (friendUsername == null) {
            System.err.println("Error: Friend username (receiver) is null!");
            return;
        }

        // ✅ Save message in database
        savePrivateMessage(sender, friendUsername, message);

        // ✅ Display message in chat
        privateChatMessages.getItems().add(sender + ": " + message);

        // ✅ Clear message field after sending
        privateMessageField.clear();
    }

    /**
     * ✅ Saves a private message to the database and notifies the receiver
     */
    private void savePrivateMessage(String sender, String receiver, String message) {
        if (sender == null || receiver == null || message == null || message.trim().isEmpty()) {
            System.err.println("Error: Missing sender, receiver, or message!");
            return;
        }

        String query = "INSERT INTO chat_messages (sender, receiver, message, status) VALUES (?, ?, ?, 'Delivered')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, message);

            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("Message saved successfully: " + sender + " -> " + receiver);

                // ✅ Get the ChatController instance from ControllerManager
                ChatController chatController = ControllerManager.getChatController();
                if (chatController != null) {
                    Platform.runLater(() -> chatController.updateFriendNotification(receiver)); // ✅ Notify receiver
                } else {
                    System.err.println("Error: ChatController instance is null!");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void updateFriendNotification(String receiver) {
        Platform.runLater(() -> {
            if (friendsList == null) {
                System.err.println("Error: friendsList is not initialized!");
                return;
            }

            for (int i = 0; i < friendsList.getItems().size(); i++) {
                String friend = friendsList.getItems().get(i);
                if (friend.contains(receiver)) {
                    // ✅ Extract existing notification count
                    int unreadCount = 1;
                    if (friend.matches(".*\\(\\d+\\)$")) {
                        unreadCount = Integer.parseInt(friend.replaceAll("[^0-9]", "")) + 1;
                    }

                    // ✅ Update with new count in a blue badge
                    friendsList.getItems().set(i, receiver + " 🔵 (" + unreadCount + ")");
                    return; // Stop after updating
                }
            }
        });
    }

    /**
     * ✅ Starts auto-refresh for real-time chat updates
     */
    private Thread chatRefreshThread;

    public void startChatAutoRefresh() {
        if (chatRefreshThread != null && chatRefreshThread.isAlive()) {
            return; // ✅ Prevent duplicate threads
        }

        chatRefreshThread = new Thread(() -> {
            while (chatStage != null && chatStage.isShowing()) { // ✅ Stops when chat window is closed
                try {
                    Thread.sleep(3000); // ✅ Refresh every 3 seconds
                    Platform.runLater(() -> loadPrivateChat(friendUsername)); // ✅ Update chat in UI thread
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break; // ✅ Exit loop if interrupted
                }
            }
        });

        chatRefreshThread.setDaemon(true); // ✅ Stops thread when app closes
        chatRefreshThread.start();
    }

    /**
     * ✅ Stops the auto-refresh thread when the chat window is closed.
     */
    public void stopChatAutoRefresh() {
        if (chatRefreshThread != null && chatRefreshThread.isAlive()) {
            chatRefreshThread.interrupt(); // ✅ Stop the thread
            System.out.println("Chat auto-refresh stopped.");
        }
    }



}
