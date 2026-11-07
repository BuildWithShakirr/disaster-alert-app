package com.example.disasteralertapp;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private final List<DocumentSnapshot> reportList = new ArrayList<>();
    private AdminReportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        // If UID is in AdminConfig allow immediately, otherwise check Firestore `users/{uid}` for role == "admin"
        if (AdminConfig.isAdmin(uid)) {
            setupAdminUi();
            return;
        }

        // Async role check
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc != null ? doc.getString("role") : null;
                    if ("admin".equalsIgnoreCase(role)) {
                        setupAdminUi();
                    } else {
                        Toast.makeText(AdminActivity.this, "Access denied", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminActivity.this, "Access denied", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // Adapter for SOS alerts
    private class SosAdapter extends RecyclerView.Adapter<SosAdapter.SosViewHolder> {
        private final List<DocumentSnapshot> items = new ArrayList<>();

        class SosViewHolder extends RecyclerView.ViewHolder {
            TextView tvUser, tvCoords, tvTime;
            Button btnOpen;

            SosViewHolder(View v) {
                super(v);
                tvUser = v.findViewById(R.id.tvSosUser);
                tvCoords = v.findViewById(R.id.tvSosCoords);
                tvTime = v.findViewById(R.id.tvSosTimestamp);
                btnOpen = v.findViewById(R.id.btnOpenInMap);
            }
        }

        @NonNull
        @Override
        public SosViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sos_alert, parent, false);
            return new SosViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SosViewHolder holder, int position) {
            DocumentSnapshot doc = items.get(position);
            String user = doc.getString("userId");
            Double lat = doc.getDouble("latitude");
            Double lng = doc.getDouble("longitude");

            holder.tvUser.setText("User: " + (user == null ? "-" : user));
            holder.tvCoords.setText("Coords: " + (lat != null ? lat : "-") + ", " + (lng != null ? lng : "-"));

            Object tsObj = doc.get("timestamp");
            if (tsObj instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) tsObj;
                holder.tvTime.setText(new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(ts.toDate()));
            } else {
                holder.tvTime.setText("-");
            }

            final double fLat = lat != null ? lat : 0.0;
            final double fLng = lng != null ? lng : 0.0;
            holder.btnOpen.setOnClickListener(v -> {
                if (fLat == 0.0 && fLng == 0.0) {
                    Toast.makeText(AdminActivity.this, "No coordinates available", Toast.LENGTH_SHORT).show();
                    return;
                }
                String url = "https://www.google.com/maps/search/?api=1&query=" + fLat + "," + fLng;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private void setupAdminUi() {
        setContentView(R.layout.activity_admin);
        db = FirebaseFirestore.getInstance();

        Button btnBack = findViewById(R.id.btnAdminBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnShowReports = findViewById(R.id.btnShowReports);
        Button btnShowSos = findViewById(R.id.btnShowSos);

        RecyclerView recyclerView = findViewById(R.id.recyclerAdminReports);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Default: show reports
        adapter = new AdminReportAdapter();
        recyclerView.setAdapter(adapter);
        loadReports();

        btnShowReports.setOnClickListener(v -> {
            recyclerView.setAdapter(adapter);
            loadReports();
        });

        btnShowSos.setOnClickListener(v -> {
            // switch to SOS adapter and load
            SosAdapter sosAdapter = new SosAdapter();
            recyclerView.setAdapter(sosAdapter);
            loadSosAlerts(sosAdapter);
        });
    }

    private void loadReports() {
        db.collection("disaster_reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        reportList.clear();
                        reportList.addAll(value.getDocuments());
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void loadSosAlerts(SosAdapter adapter) {
        db.collection("sos_alerts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        adapter.items.clear();
                        adapter.items.addAll(value.getDocuments());
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.ViewHolder> {
        private final String[] statusLabels = {"Verified", "Resolved", "False"};
        private final String[] statusValues = {"verified", "resolved", "false"};

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvType, tvDescription, tvLocation;
            Spinner spinnerStatus;
            Button btnDelete;
            TextView tvTimestamp;

            ViewHolder(View itemView) {
                super(itemView);
                tvType = itemView.findViewById(R.id.tvAdminType);
                tvDescription = itemView.findViewById(R.id.tvAdminDescription);
                tvLocation = itemView.findViewById(R.id.tvAdminLocation);
                spinnerStatus = itemView.findViewById(R.id.spinnerAdminStatus);
                btnDelete = itemView.findViewById(R.id.btnAdminDelete);
                    tvTimestamp = itemView.findViewById(R.id.tvAdminTimestamp);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentSnapshot doc = reportList.get(position);
            holder.tvType.setText("Type: " + safe(doc.getString("type")));
            holder.tvDescription.setText("Description: " + safe(doc.getString("description")));
            holder.tvLocation.setText("Location: " + safe(doc.getString("locationName")));

            ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                    AdminActivity.this,
                    android.R.layout.simple_spinner_dropdown_item,
                    statusLabels);
            holder.spinnerStatus.setAdapter(statusAdapter);

            String currentStatus = doc.getString("status");
            if (currentStatus != null) {
                // find matching value index (case-insensitive)
                for (int i = 0; i < statusValues.length; i++) {
                    if (statusValues[i].equalsIgnoreCase(currentStatus) || statusLabels[i].equalsIgnoreCase(currentStatus)) {
                        holder.spinnerStatus.setSelection(i);
                        break;
                    }
                }
            }

            // timestamp
            Object tsObj = doc.get("timestamp");
            if (tsObj instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) tsObj;
                holder.tvTimestamp.setText("Reported: " + new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(ts.toDate()));
            } else {
                holder.tvTimestamp.setText("Reported: -");
            }

            holder.spinnerStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                boolean firstCall = true;

                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                    if (firstCall) {
                        firstCall = false;
                        return;
                    }
                        String newStatus = statusValues[pos];
                        db.collection("disaster_reports")
                            .document(doc.getId())
                            .update("status", newStatus)
                            .addOnSuccessListener(aVoid ->
                                Toast.makeText(AdminActivity.this, "Status updated", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            holder.btnDelete.setOnClickListener(v ->
                    db.collection("disaster_reports")
                            .document(doc.getId())
                            .delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(AdminActivity.this, "Report deleted", Toast.LENGTH_SHORT).show()));
        }

        @Override
        public int getItemCount() {
            return reportList.size();
        }

        private String safe(String value) {
            return value == null ? "-" : value;
        }
    }
}
