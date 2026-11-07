package com.example.disasteralertapp;

/**
 * Admin access is restricted by Firebase Auth UID.
 * Replace the placeholder with your own admin account UID(s) from Firebase Console.
 */
public final class AdminConfig {

    private static final String[] ADMIN_UIDS = {
            "YOUR_ADMIN_FIREBASE_UID"
    };

    private AdminConfig() {}

    public static boolean isAdmin(String uid) {
        if (uid == null || uid.isEmpty()) return false;
        for (String adminUid : ADMIN_UIDS) {
            if (uid.equals(adminUid)) return true;
        }
        return false;
    }
}
