package com.example.disasteralertapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class FcmTokenManager {

    public static void registerToken() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    String uid = auth.getCurrentUser().getUid();
                    Map<String, Object> data = new HashMap<>();
                    data.put("fcmToken", token);
                    data.put("updatedAt", FieldValue.serverTimestamp());

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .set(data, com.google.firebase.firestore.SetOptions.merge());
                });

        FirebaseMessaging.getInstance().subscribeToTopic("disaster_alerts");
    }
}
