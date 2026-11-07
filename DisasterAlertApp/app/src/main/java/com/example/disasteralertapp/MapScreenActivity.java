package com.example.disasteralertapp;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapScreenActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_screen);

        db = FirebaseFirestore.getInstance();

        Button btnBack = findViewById(R.id.btnBackMap);
        btnBack.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        loadReportMarkers();
    }

    private void loadReportMarkers() {
        db.collection("disaster_reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> docs = querySnapshot.getDocuments();
                    LatLng firstMarker = null;

                    for (DocumentSnapshot doc : docs) {
                        Object latObj = doc.get("latitude");
                        Object lngObj = doc.get("longitude");
                        if (!(latObj instanceof Number) || !(lngObj instanceof Number)) continue;

                        double lat = ((Number) latObj).doubleValue();
                        double lng = ((Number) lngObj).doubleValue();
                        if (lat == 0.0 && lng == 0.0) continue;

                        LatLng position = new LatLng(lat, lng);
                        if (firstMarker == null) firstMarker = position;

                        String type = doc.getString("type");
                        String snippet = buildSnippet(doc);

                        googleMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(type != null ? type : "Disaster")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(getMarkerHue(type))));
                    }

                    if (firstMarker != null) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstMarker, 11f));
                    }
                });
    }

    private String buildSnippet(DocumentSnapshot doc) {
        Timestamp ts = doc.getTimestamp("timestamp");
        if (ts != null) {
            Date date = ts.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            return sdf.format(date);
        }
        return "Time unknown";
    }

    private float getMarkerHue(String type) {
        if (type == null) return BitmapDescriptorFactory.HUE_RED;
        switch (type) {
            case "Fire":
                return BitmapDescriptorFactory.HUE_RED;
            case "Flood":
                return BitmapDescriptorFactory.HUE_AZURE;
            case "Earthquake":
                return BitmapDescriptorFactory.HUE_YELLOW;
            default:
                return BitmapDescriptorFactory.HUE_ORANGE;
        }
    }
}
