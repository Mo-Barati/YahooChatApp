
package com.chatapp.yahoochatapp;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrivateChatController {

    @FXML private ListView<Node> privateChatListView;
    @FXML private TextArea privateMessageField;
    @FXML private Button sendPrivateMessageButton;
    @FXML private Button emojiButton;

    private String currentUser;
    private String friendUsername;
    private Stage chatStage;
    private Thread chatRefreshThread;

    public void setChatStage(Stage stage) {
        this.chatStage = stage;
    }

    public void setFriend(String friend) {
        this.friendUsername = friend;
        loadPrivateChat(friendUsername);
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            try {
                privateChatListView.setFocusTraversable(false); // optional: avoid auto-focus
                privateChatListView.setStyle("-fx-font-size: 16px;"); // readable font size
                System.out.println("✅ ListView initialized for private chat");
            } catch (Exception e) {
                System.err.println("❌ Error initializing ListView: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void loadPrivateChat(String friendUsername) {
        String currentUser = SessionManager.getUser();
        friendUsername = friendUsername.replaceAll("\\(\\d+\\)", "");

        System.out.println("Loading chat between " + currentUser + " and " + friendUsername);

        String query = "SELECT sender, message FROM chat_messages " +
                "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY id ASC";

        List<HBox> messageBubbles = new ArrayList<>();

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

                    Label label = new Label(sender + ": " + message);
                    label.setWrapText(true);
                    label.setMaxWidth(300);
                    label.setPadding(new Insets(8));
                    label.setStyle(
                            "-fx-background-color: " + (sender.equals(currentUser) ? "#DCF8C6" : "#FFFFFF") + ";" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-border-radius: 10;" +
                                    "-fx-font-size: 14px;" +
                                    "-fx-text-fill: black;"
                    );

                    HBox hBox = new HBox(label);
                    hBox.setAlignment(sender.equals(currentUser) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                    hBox.setPadding(new Insets(4, 10, 4, 10));

                    messageBubbles.add(hBox);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        Platform.runLater(() -> privateChatListView.getItems().setAll(messageBubbles));
    }

    @FXML
    private void handleSendPrivateMessage() {
        String message = privateMessageField.getText().trim();
        if (message.isEmpty()) return;

        String sender = SessionManager.getUser();
        savePrivateMessage(sender, friendUsername, message);

        // Update ListView directly
        Platform.runLater(() -> {
            Label messageLabel = new Label(sender + ": " + message);
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(300);
            messageLabel.setPadding(new Insets(8));
            messageLabel.setStyle("-fx-background-color: #DCF8C6; -fx-background-radius: 10; -fx-border-radius: 10; -fx-font-size: 14px; -fx-text-fill: black;");

            HBox bubble = new HBox(messageLabel);
            bubble.setAlignment(Pos.CENTER_RIGHT);
            bubble.setPadding(new Insets(4, 10, 4, 10));

            privateChatListView.getItems().add(bubble);
            privateMessageField.clear();
        });
    }

    private void savePrivateMessage(String sender, String receiver, String message) {
        String query = "INSERT INTO chat_messages (sender, receiver, message, status) VALUES (?, ?, ?, 'Delivered')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void startChatAutoRefresh() {
        if (chatRefreshThread != null && chatRefreshThread.isAlive()) {
            return; // ✅ Prevent duplicate threads
        }

        chatRefreshThread = new Thread(() -> {
            while (chatStage != null && chatStage.isShowing()) {
                try {
                    Thread.sleep(3000); // ✅ Refresh every 3 seconds
                    Platform.runLater(() -> loadPrivateChat(friendUsername)); // ✅ Refresh chat in UI
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });

        chatRefreshThread.setDaemon(true);
        chatRefreshThread.start();
    }

    @FXML
    private void showEmojiPicker(ActionEvent event) {
        ContextMenu emojiMenu = new ContextMenu();
        String[] emojis = {"😀", "😂", "😍", "👍", "🔥", "❤️", "😊", "😎", "🤔", "🎉"};
        for (String emoji : emojis) {
            MenuItem emojiItem = new MenuItem(emoji);
            emojiItem.setStyle("-fx-font-size: 16px;");
            emojiItem.setOnAction(e -> privateMessageField.appendText(emoji));
            emojiMenu.getItems().add(emojiItem);
        }
        emojiMenu.show(emojiButton, emojiButton.getScene().getWindow().getX() + emojiButton.getLayoutX(),
                emojiButton.getScene().getWindow().getY() + emojiButton.getLayoutY() + emojiButton.getHeight());
    }
}
