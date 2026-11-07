package com.example.disasteralertapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "fcm_disaster_channel";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        if (message.getNotification() != null) {
            showNotification(
                    message.getNotification().getTitle(),
                    message.getNotification().getBody());
        } else if (message.getData().containsKey("type")) {
            showNotification(
                    "Disaster Alert",
                    "New disaster reported: " + message.getData().get("type"));
        }
    }

    @Override
    public void onNewToken(String token) {
        FcmTokenManager.registerToken();
    }

    private void showNotification(String title, String body) {
        createChannel();
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title != null ? title : "Disaster Alert")
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "FCM Disaster Alerts", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
