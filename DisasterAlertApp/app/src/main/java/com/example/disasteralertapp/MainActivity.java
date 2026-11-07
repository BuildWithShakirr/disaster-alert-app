package com.example.disasteralertapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db; 
    RecyclerView recyclerView;
    FloatingActionButton fabAdd, fabSos;
    TextView tvLogout, tvAdmin;

    List<DocumentSnapshot> reportList = new ArrayList<>();
    ReportAdapter adapter;

    FusedLocationProviderClient fusedLocationClient;
    SosService sosService;
    ReportNotificationListener notificationListener;
    




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sosService = new SosService();
        notificationListener = new ReportNotificationListener(this);
        // media picker removed: replaced with optional Media URL input in the report dialog

        tvLogout = findViewById(R.id.tvLogout);
        tvAdmin = findViewById(R.id.tvAdmin);
        fabAdd = findViewById(R.id.fabAdd);
        fabSos = findViewById(R.id.fabSos);
        recyclerView = findViewById(R.id.recyclerViewReports);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter();
        recyclerView.setAdapter(adapter);

        tvLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        fabAdd.setOnClickListener(v -> showReportDialog());
        fabSos.setOnClickListener(v -> showSosConfirmation());

        if (AdminConfig.isAdmin(mAuth.getCurrentUser().getUid())) {
            tvAdmin.setVisibility(View.VISIBLE);
            tvAdmin.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, AdminActivity.class)));
        }

        requestNotificationPermission();
        FcmTokenManager.registerToken();
        notificationListener.start();

        loadReports();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.stop();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void showSosConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Send SOS Alert?")
                .setMessage("Your live location will be shared with emergency responders.")
                .setPositiveButton("Send SOS", (dialog, which) -> sendSosAlert())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendSosAlert() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 102);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Toast.makeText(this, "Turn on GPS and try again", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String userId = mAuth.getCurrentUser().getUid();
                    sosService.sendSosAlert(userId, location, new SosService.SosCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, "SOS alert sent", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(MainActivity.this, "Failed to send SOS", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    





        private void showReportDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40,30,40,10);

        Spinner spinner = new Spinner(this);
        String[] types = {"Flood","Fire","Earthquake"};
        spinner.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, types));

        EditText etLocationName = new EditText(this);
        etLocationName.setHint("Enter Location Name");

        EditText etDescription = new EditText(this);
        etDescription.setHint("Enter Description");

        EditText etMediaUrl = new EditText(this);
        etMediaUrl.setHint("Media URL (Optional)");

        layout.addView(spinner);
        layout.addView(etLocationName);
        layout.addView(etDescription);
        layout.addView(etMediaUrl);

        new AlertDialog.Builder(this)
            .setTitle("Add Disaster Report")
            .setView(layout)
            .setPositiveButton("Submit", (dialog, which) -> {

                if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                    100);
                return;
                }

                fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                    if(location != null) {
                        String mediaUrl = etMediaUrl.getText() != null ? etMediaUrl.getText().toString().trim() : null;
                        submitReportWithOptionalMedia(
                            spinner.getSelectedItem().toString(),
                            etLocationName.getText().toString(),
                            etDescription.getText().toString(),
                            location,
                            mediaUrl);
                    } else {
                        Toast.makeText(this,"Turn on GPS and try again",Toast.LENGTH_SHORT).show();
                    }
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
        }

    private void submitReportWithOptionalMedia(String type, String locationName,
                                               String description, Location location,
                                               String mediaUrl) {
        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            String lower = mediaUrl.toLowerCase(Locale.ROOT);
            String mediaType = "unknown";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp")) {
                mediaType = "image";
            } else if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm") || lower.endsWith(".avi") || lower.endsWith(".mkv")) {
                mediaType = "video";
            }
            saveReport(type, locationName, description, location, mediaUrl, mediaType);
        } else {
            saveReport(type, locationName, description, location);
        }
    }

    private void saveReport(String type, String locationName,
                            String description, Location location) {
        saveReport(type, locationName, description, location, null, null);
    }

    private void saveReport(String type, String locationName,
                            String description, Location location,
                            String mediaUrl, String mediaType) {

        Map<String,Object> map = new HashMap<>();
        map.put("type", type);
        map.put("locationName", locationName);
        map.put("description", description);
        map.put("latitude", location.getLatitude());
        map.put("longitude", location.getLongitude());
        map.put("timestamp", FieldValue.serverTimestamp());
        map.put("addedBy", mAuth.getCurrentUser().getEmail());

        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            map.put("mediaUrl", mediaUrl);
            map.put("mediaType", mediaType != null ? mediaType : "unknown");
        }

        db.collection("disaster_reports")
                .add(map)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this,"Report Saved",Toast.LENGTH_SHORT).show());
    }

    private void loadReports() {
        db.collection("disaster_reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if(value != null) {
                        reportList.clear();
                        reportList.addAll(value.getDocuments());
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvType, tvAddedBy, tvLocationName,
                    tvLatLng, tvDescription, tvDateTime;
                Button btnDelete, btnViewDetails, btnLocation;

            ViewHolder(android.view.View itemView) {
                super(itemView);

                tvType = itemView.findViewById(R.id.tvType);
                tvAddedBy = itemView.findViewById(R.id.tvAddedBy);
                tvLocationName = itemView.findViewById(R.id.tvLocationName);
                tvLatLng = itemView.findViewById(R.id.tvLatLng);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
                btnLocation = itemView.findViewById(R.id.btnLocation);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater()
                    .inflate(R.layout.item_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            DocumentSnapshot doc = reportList.get(position);

            holder.tvType.setText("Type: " + doc.getString("type"));
            holder.tvAddedBy.setText("Added by: " + doc.getString("addedBy"));
            holder.tvLocationName.setText("Location: " + doc.getString("locationName"));
            holder.tvDescription.setText("Description: " + doc.getString("description"));

            double lat = 0.0;
            double lng = 0.0;
            Object latObj = doc.get("latitude");
            Object lngObj = doc.get("longitude");
            if (latObj instanceof Number) lat = ((Number) latObj).doubleValue();
            if (lngObj instanceof Number) lng = ((Number) lngObj).doubleValue();
            holder.tvLatLng.setText("Lat: " + lat + " | Lng: " + lng);

            // Make final copies for use inside lambda listeners
            final double fLat = lat;
            final double fLng = lng;

            Timestamp ts = doc.getTimestamp("timestamp");
            if(ts != null) {
                Date date = ts.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                holder.tvDateTime.setText("Reported: " + sdf.format(date));
            } else {
                holder.tvDateTime.setText("Reported: Just now");
            }

            // Wire the new buttons: View Details and Location. Card clicks no longer open maps.
            holder.btnLocation.setOnClickListener(v -> openLocationInMap(fLat, fLng));

            holder.btnViewDetails.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, DisasterDetailActivity.class);
                intent.putExtra("type", doc.getString("type"));
                intent.putExtra("description", doc.getString("description"));
                intent.putExtra("locationName", doc.getString("locationName"));
                intent.putExtra("latitude", fLat);
                intent.putExtra("longitude", fLng);
                Timestamp t = doc.getTimestamp("timestamp");
                if (t != null) {
                    SimpleDateFormat sdf2 = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                    intent.putExtra("timestamp", sdf2.format(t.toDate()));
                } else {
                    intent.putExtra("timestamp", "Just now");
                }
                intent.putExtra("addedBy", doc.getString("addedBy"));
                intent.putExtra("docId", doc.getId());
                intent.putExtra("status", doc.getString("status"));
                intent.putExtra("mediaUrl", doc.getString("mediaUrl"));
                intent.putExtra("mediaType", doc.getString("mediaType"));
                startActivity(intent);
            });

            String currentUser = null;

            if (mAuth.getCurrentUser() != null) {
                currentUser = mAuth.getCurrentUser().getEmail();
            }
            String addedBy = doc.getString("addedBy");

            if (currentUser != null && currentUser.equals(addedBy)) {
                holder.btnDelete.setVisibility(View.VISIBLE);

                holder.btnDelete.setOnClickListener(v -> {
                    db.collection("disaster_reports")
                            .document(doc.getId())
                            .delete();
                });

            } else {
                holder.btnDelete.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return reportList.size();
        }
    }

    private void insertFakeData() {

        List<Map<String,Object>> fakeReports = new ArrayList<>();

        Map<String,Object> r1 = new HashMap<>();
        r1.put("type", "Flood");
        r1.put("locationName", "Market Block 5");
        r1.put("description", "Heavy water accumulation on main road.");
        r1.put("latitude", 24.8138);
        r1.put("longitude", 67.0305);
        r1.put("addedBy", "user1@test.com");
        r1.put("timestamp", FieldValue.serverTimestamp());
        fakeReports.add(r1);

        Map<String,Object> r2 = new HashMap<>();
        r2.put("type", "Fire");
        r2.put("locationName", "Gulshan-e-Iqbal");
        r2.put("description", "Small market fire reported.");
        r2.put("latitude", 24.9200);
        r2.put("longitude", 67.0900);
        r2.put("addedBy", "user2@test.com");
        r2.put("timestamp", FieldValue.serverTimestamp());
        fakeReports.add(r2);

        Map<String,Object> r3 = new HashMap<>();
        r3.put("type", "Earthquake");
        r3.put("locationName", "PECHS Block 2");
        r3.put("description", "Mild tremors felt for 5 seconds.");
        r3.put("latitude", 24.8607);
        r3.put("longitude", 67.0011);
        r3.put("addedBy", "user1@test.com");
        r3.put("timestamp", FieldValue.serverTimestamp());
        fakeReports.add(r3);

        for (Map<String,Object> report : fakeReports) {
            db.collection("disaster_reports").add(report);
        }

        Toast.makeText(this, "Fake data inserted", Toast.LENGTH_SHORT).show();
    }

    // Reusable method to open location in Maps or browser. Tries Chrome, then any handler.
    private void openLocationInMap(double latitude, double longitude) {
        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Invalid location coordinates", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String url = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude + "&title=Disaster%20Location";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            // Try Chrome first for best experience
            intent.setPackage("com.android.chrome");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }

            // Try any app that can handle the URL (maps or browser)
            intent.setPackage(null);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }

            // Final fallback: launch with a fresh ACTION_VIEW
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

        } catch (Exception e) {
            Toast.makeText(this, "Could not open map. Please check internet connection.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}