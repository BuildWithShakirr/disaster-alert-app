package com.example.disasteralertapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DisasterDetailActivity extends AppCompatActivity {

    TextView tvType, tvDescription, tvLocationName, tvLatLng, tvTimestamp, tvAddedBy, tvStatus;
    TextView tvVoteStatus, tvVoteCounts, tvMediaLink;
    ImageView imgMedia;
    Button btnBack, btnOpenMap, btnConfirm, btnDismiss;

    private String docId;
    private VoteManager voteManager;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disaster_detail);

        voteManager = new VoteManager(this);
        db = FirebaseFirestore.getInstance();

        tvType = findViewById(R.id.tvType);
        tvDescription = findViewById(R.id.tvDescription);
        tvLocationName = findViewById(R.id.tvLocationName);
        tvLatLng = findViewById(R.id.tvLatLng);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvAddedBy = findViewById(R.id.tvAddedBy);
        tvStatus = findViewById(R.id.tvStatus);
        tvVoteStatus = findViewById(R.id.tvVoteStatus);
        tvVoteCounts = findViewById(R.id.tvVoteCounts);
        tvMediaLink = findViewById(R.id.tvMediaLink);
        imgMedia = findViewById(R.id.imgMedia);

        btnBack = findViewById(R.id.btnBack);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnDismiss = findViewById(R.id.btnDismiss);

        Intent intent = getIntent();
        if (intent != null) {
            docId = intent.getStringExtra("docId");

            tvType.setText("Type: " + safe(intent.getStringExtra("type")));
            tvDescription.setText("Description: " + safe(intent.getStringExtra("description")));
            tvLocationName.setText("Location: " + safe(intent.getStringExtra("locationName")));
            double lat = intent.getDoubleExtra("latitude", 0.0);
            double lng = intent.getDoubleExtra("longitude", 0.0);
            tvLatLng.setText("Coordinates: " + lat + ", " + lng);
            tvTimestamp.setText("Reported: " + safe(intent.getStringExtra("timestamp")));
            tvAddedBy.setText("Added by: " + safe(intent.getStringExtra("addedBy")));
            tvStatus.setText("Status: " + safe(intent.getStringExtra("status")));

            displayMedia(intent.getStringExtra("mediaUrl"), intent.getStringExtra("mediaType"));

            final double fLat = lat;
            final double fLng = lng;
            btnOpenMap.setOnClickListener(v -> openLocationInMap(fLat, fLng));
        }

        btnBack.setOnClickListener(v -> finish());
        setupVoting();
    }

    private void setupVoting() {
        if (docId == null || docId.isEmpty()) {
            btnConfirm.setEnabled(false);
            btnDismiss.setEnabled(false);
            return;
        }

        loadVoteData();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        btnConfirm.setOnClickListener(v -> {
            if (userId == null) return;
            voteManager.upvote(docId, userId,
                    () -> {
                        Toast.makeText(this, "Report confirmed", Toast.LENGTH_SHORT).show();
                        loadVoteData();
                    },
                    () -> Toast.makeText(this, "You already voted", Toast.LENGTH_SHORT).show());
        });

        btnDismiss.setOnClickListener(v -> {
            if (userId == null) return;
            voteManager.downvote(docId, userId,
                    () -> {
                        Toast.makeText(this, "Report dismissed", Toast.LENGTH_SHORT).show();
                        loadVoteData();
                    },
                    () -> Toast.makeText(this, "You already voted", Toast.LENGTH_SHORT).show());
        });
    }

    private void loadVoteData() {
        db.collection("disaster_reports").document(docId)
                .get()
                .addOnSuccessListener(this::updateVoteUi);
    }

    private void updateVoteUi(DocumentSnapshot doc) {
        long upvotes = doc.getLong("upvotes") != null ? doc.getLong("upvotes") : 0;
        long downvotes = doc.getLong("downvotes") != null ? doc.getLong("downvotes") : 0;
        tvVoteCounts.setText("Confirm: " + upvotes + " | Dismiss: " + downvotes);
        tvVoteStatus.setText("Community Status: " + voteManager.getVoteStatus(upvotes, downvotes));
    }

    private void displayMedia(String mediaUrl, String mediaType) {
        if (mediaUrl == null || mediaUrl.isEmpty()) return;

        tvMediaLink.setVisibility(View.VISIBLE);
        if ("video".equals(mediaType)) {
            tvMediaLink.setText("View attached video");
        } else {
            tvMediaLink.setText("View attached photo");
        }
        tvMediaLink.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mediaUrl))));
    }

    private String safe(String s) {
        return s == null ? "-" : s;
    }

    private void openLocationInMap(double latitude, double longitude) {
        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Invalid location coordinates", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String url = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude + "&title=Disaster%20Location";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            intent.setPackage("com.android.chrome");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }

            intent.setPackage(null);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }

            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

        } catch (Exception e) {
            Toast.makeText(this, "Could not open map. Please check internet connection.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
