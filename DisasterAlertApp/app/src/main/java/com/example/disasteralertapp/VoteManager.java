package com.example.disasteralertapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class VoteManager {

    private static final String PREFS_NAME = "report_votes";

    private final SharedPreferences prefs;
    private final FirebaseFirestore db;

    public VoteManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
    }

    public boolean hasVoted(String docId, String userId) {
        return prefs.contains(voteKey(docId, userId));
    }

    public String getVoteStatus(long upvotes, long downvotes) {
        if (upvotes > downvotes) return "Verified";
        if (downvotes > upvotes) return "Fake";
        return "Pending";
    }

    public void upvote(String docId, String userId, Runnable onSuccess, Runnable onAlreadyVoted) {
        if (hasVoted(docId, userId)) {
            onAlreadyVoted.run();
            return;
        }
        db.collection("disaster_reports").document(docId)
                .update("upvotes", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    prefs.edit().putString(voteKey(docId, userId), "up").apply();
                    onSuccess.run();
                });
    }

    public void downvote(String docId, String userId, Runnable onSuccess, Runnable onAlreadyVoted) {
        if (hasVoted(docId, userId)) {
            onAlreadyVoted.run();
            return;
        }
        db.collection("disaster_reports").document(docId)
                .update("downvotes", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    prefs.edit().putString(voteKey(docId, userId), "down").apply();
                    onSuccess.run();
                });
    }

    private String voteKey(String docId, String userId) {
        return docId + "_" + userId;
    }
}
