package com.example.disasteralertapp;

import android.location.Location;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SosService {

    public interface SosCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    private final FirebaseFirestore db;

    public SosService() {
        db = FirebaseFirestore.getInstance();
    }

    public void sendSosAlert(String userId, Location location, SosCallback callback) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("userId", userId);
        alert.put("latitude", location.getLatitude());
        alert.put("longitude", location.getLongitude());
        alert.put("timestamp", FieldValue.serverTimestamp());

        db.collection("sos_alerts")
                .add(alert)
                .addOnSuccessListener(ref -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
