package com.chatapp.yahoochatapp;

public class SessionManager {

    private static String currentUser = null;

    public static void setUser(String username) {
        currentUser = username;
    }

    public static String getUser() {
        return currentUser;
    }

    public static void clearSession() {
        currentUser = null;
    }

    // Check if session is active
    public static boolean isUserLoggedIn() {
        return currentUser != null;
    }
}
