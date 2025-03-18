package com.chatapp.yahoochatapp;

public class ControllerManager {
    private static ChatController chatController;

    public static void setChatController(ChatController controller) {
        chatController = controller;
    }

    public static ChatController getChatController() {
        return chatController;
    }
}
