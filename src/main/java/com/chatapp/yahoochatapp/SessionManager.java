package com.chatapp.yahoochatapp;

public class SessionManager {

    private static String currentUser = null;
    private static String profilePicturePath = null;

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

    // Store profile picture
    public static void setProfilePicture(String path) {
        profilePicturePath = path;
    }

    // Retrieve profile picture
    public static String getProfilePicture() {
        return profilePicturePath;
    }
}
