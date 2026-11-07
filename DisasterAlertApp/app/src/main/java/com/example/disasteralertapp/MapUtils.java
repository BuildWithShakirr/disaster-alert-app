package com.example.disasteralertapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class MapUtils {

    private static final String TAG = "MapUtils";

    /**
     * Open a location in Maps. Tries Google Maps app first, then any maps app via geo: URI,
     * then falls back to Google Maps web URL in browser.
     * Returns true if an activity was started, false otherwise.
     */
    public static boolean openMap(Context context, double latitude, double longitude, String label) {
        if (context == null) return false;

        String latLng = latitude + "," + longitude;
        if (label == null) label = "Location";

        // 1) Preferred: Google Maps app using navigation or maps URI
        try {
            // Try navigation intent (opens directions view) - works well when Maps is installed
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latLng + "(" + Uri.encode(label) + ")");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
                return true;
            }
        } catch (ActivityNotFoundException ex) {
            Log.w(TAG, "Google Maps app not found for navigation", ex);
        } catch (Exception ex) {
            Log.w(TAG, "Error launching Google Maps navigation", ex);
        }

        // 2) Try generic geo: URI which many map apps support
        try {
            Uri geoUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + Uri.encode(latLng + "(" + label + ")"));
            Intent geoIntent = new Intent(Intent.ACTION_VIEW, geoUri);
            if (geoIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(geoIntent);
                return true;
            }
        } catch (ActivityNotFoundException ex) {
            Log.w(TAG, "No app found for geo: URI", ex);
        } catch (Exception ex) {
            Log.w(TAG, "Error launching geo: URI", ex);
        }

        // 3) Final fallback: open Google Maps in a browser
        try {
            String url = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(latLng + "(" + label + ")");
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (browserIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(browserIntent);
                return true;
            }
        } catch (ActivityNotFoundException ex) {
            Log.w(TAG, "No browser available to open maps URL", ex);
        } catch (Exception ex) {
            Log.w(TAG, "Error launching browser maps URL", ex);
        }

        // Nothing worked, show user feedback if we have an Activity context
        if (context instanceof Activity) {
            Toast.makeText(context, "No maps or browser app available to open location.", Toast.LENGTH_LONG).show();
        }
        return false;
    }
}
