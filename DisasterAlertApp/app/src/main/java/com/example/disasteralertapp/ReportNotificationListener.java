package com.example.disasteralertapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashSet;
import java.util.Set;

public class ReportNotificationListener {

    private static final String CHANNEL_ID = "disaster_reports_channel";
    private static final int NOTIFICATION_ID_BASE = 1000;

    private final Context context;
    private final Set<String> knownReportIds = new HashSet<>();
    private boolean initialLoadDone = false;
    private ListenerRegistration registration;

    public ReportNotificationListener(Context context) {
        this.context = context.getApplicationContext();
        createNotificationChannel();
    }

    public void start() {
        registration = FirebaseFirestore.getInstance()
                .collection("disaster_reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((QuerySnapshot snapshots, com.google.firebase.firestore.FirebaseFirestoreException error) -> {
                    if (snapshots == null) return;

                    if (!initialLoadDone) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            knownReportIds.add(doc.getId());
                        }
                        initialLoadDone = true;
                        return;
                    }

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED
                                && !knownReportIds.contains(change.getDocument().getId())) {
                            knownReportIds.add(change.getDocument().getId());
                            String type = change.getDocument().getString("type");
                            showNotification(type != null ? type : "Unknown");
                        }
                    }
                });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void showNotification(String type) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Disaster Alert")
                .setContentText("New disaster reported: " + type)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        int notificationId = NOTIFICATION_ID_BASE + (int) (System.currentTimeMillis() % 10000);
        manager.notify(notificationId, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Disaster Reports",
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }
}
