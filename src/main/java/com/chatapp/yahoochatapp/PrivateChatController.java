package com.chatapp.yahoochatapp;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PrivateChatController {

    @FXML private ListView<String> privateChatMessages;
    @FXML private TextArea privateMessageField;
    @FXML private Button sendPrivateMessageButton;

    private String currentUser;
    private String friendUsername;

    public void setFriend(String friend) {
        this.friendUsername = friend;
        this.currentUser = SessionManager.getUser();
        loadPrivateChat();
    }

    private void loadPrivateChat() {
        privateChatMessages.getItems().clear();

        String query = "SELECT sender, message FROM chat_messages WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, currentUser);
            pstmt.setString(2, friendUsername);
            pstmt.setString(3, friendUsername);
            pstmt.setString(4, currentUser);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String sender = rs.getString("sender");
                String message = rs.getString("message");
                privateChatMessages.getItems().add(sender + ": " + message);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSendPrivateMessage() {
        String message = privateMessageField.getText().trim();
        if (!message.isEmpty()) {
            savePrivateMessage(currentUser, friendUsername, message);
            privateChatMessages.getItems().add("You: " + message);
            privateMessageField.clear();
        }
    }

    private void savePrivateMessage(String sender, String receiver, String message) {
        String query = "INSERT INTO chat_messages (sender, receiver, message, status) VALUES (?, ?, ?, 'Sent')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, message);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Private message sent successfully to " + receiver);
            } else {
                System.out.println("Failed to send private message.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
