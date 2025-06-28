package com.example.atm_finder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.libraries.places.api.Places;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final int REQUEST_LOCATION = 11;
    private static final int RADIUS_METERS = 3000;

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLoc;

    private EditText searchBar;
    private FloatingActionButton recenterBtn;
    private TextView routeBanner;
    private Polyline currentRoute;
    private final List<Marker> liveMarkers = new ArrayList<>();

    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup parent, Bundle b) {
        View root = inf.inflate(R.layout.fragment_home, parent, false);

        searchBar = root.findViewById(R.id.search_bar);
        recenterBtn = root.findViewById(R.id.btn_recenter);
        routeBanner = root.findViewById(R.id.route_banner);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI");
        }

        createNotificationChannel();

        SupportMapFragment mf = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mf != null) mf.getMapAsync(this);

        searchBar.setOnKeyListener((v, key, ev) -> {
            if (ev.getAction() == KeyEvent.ACTION_DOWN && key == KeyEvent.KEYCODE_ENTER) {
                String q = searchBar.getText().toString().trim();
                if (!q.isEmpty()) requestNearby(q);
                return true;
            }
            return false;
        });

        recenterBtn.setOnClickListener(v -> recenter());

        return root;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap gMap) {
        map = gMap;
        map.setOnMarkerClickListener(this);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }
        map.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc == null) {
                Toast.makeText(getContext(), "Location unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            currentLoc = loc;

            LatLng here = new LatLng(loc.getLatitude(), loc.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 15));
            map.addMarker(new MarkerOptions()
                    .position(here)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            requestNearby("atm");
        });
    }

    private void requestNearby(String keyword) {
        if (currentLoc == null) {
            Toast.makeText(getContext(), "Location not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + currentLoc.getLatitude() + "," + currentLoc.getLongitude()
                + "&radius=" + RADIUS_METERS
                + "&keyword=" + Uri.encode(keyword)
                + "&key=" + "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI";

        new AsyncTask<Void, Void, NearbyResult>() {
            IOException err;

            @Override
            protected NearbyResult doInBackground(Void... voids) {
                try (Response res = http.newCall(new Request.Builder().url(url).build()).execute()) {
                    if (!res.isSuccessful()) throw new IOException(res.toString());
                    return gson.fromJson(res.body().string(), NearbyResult.class);
                } catch (IOException e) {
                    err = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(NearbyResult result) {
                if (err != null || result == null) {
                    Toast.makeText(getContext(), "Search failed: " + (err != null ? err.getMessage() : "?"), Toast.LENGTH_SHORT).show();
                } else {
                    dropMarkers(result);
                }
            }
        }.execute();
    }

    private void dropMarkers(NearbyResult r) {
        if (currentRoute != null) {
            currentRoute.remove();
            currentRoute = null;
        }
        routeBanner.setVisibility(View.GONE);

        for (Marker m : liveMarkers) m.remove();
        liveMarkers.clear();

        LatLng user = new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude());

        for (NearbyResult.PlaceRes pr : r.results) {
            LatLng pos = new LatLng(pr.geometry.location.lat, pr.geometry.location.lng);
            float[] d = new float[1];
            Location.distanceBetween(user.latitude, user.longitude, pos.latitude, pos.longitude, d);
            int meters = Math.round(d[0]);

            Marker mk = map.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(pr.name + " (" + meters + " m)")
                    .snippet(pr.address));
            if (mk != null) {
                mk.setTag(pr);
                liveMarkers.add(mk);
            }
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker m) {
        if (m.getTag() instanceof NearbyResult.PlaceRes)
            showBottomSheet((NearbyResult.PlaceRes) m.getTag());
        return true;
    }

    private void showBottomSheet(NearbyResult.PlaceRes p) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_place, null, false);

        ((TextView) v.findViewById(R.id.txt_name)).setText(p.name);
        ((TextView) v.findViewById(R.id.txt_address)).setText(p.address != null ? p.address : "—");

        TextView distTv = v.findViewById(R.id.txt_distance);
        float[] res = new float[1];
        Location.distanceBetween(currentLoc.getLatitude(), currentLoc.getLongitude(), p.geometry.location.lat, p.geometry.location.lng, res);
        distTv.setText(String.format("%.2f km away", res[0] / 1000f));

        v.findViewById(R.id.btn_direction).setOnClickListener(btn -> {
            fetchAndDrawRoute(p);
            sheet.dismiss();
        });

        v.findViewById(R.id.btn_save).setOnClickListener(btn -> {
            savePlaceToFirebase(p);
            sheet.dismiss();
        });

        sheet.setContentView(v);
        sheet.show();
    }

    private void fetchAndDrawRoute(NearbyResult.PlaceRes p) {
        if (currentLoc == null) {
            Toast.makeText(getContext(), "Location not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://maps.googleapis.com/maps/api/directions/json"
                + "?origin=" + currentLoc.getLatitude() + "," + currentLoc.getLongitude()
                + "&destination=" + p.geometry.location.lat + "," + p.geometry.location.lng
                + "&mode=driving"
                + "&key=" + "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI";

        new AsyncTask<Void, Void, DirResult>() {
            IOException err;

            @Override
            protected DirResult doInBackground(Void... v) {
                try (Response r = http.newCall(new Request.Builder().url(url).build()).execute()) {
                    if (!r.isSuccessful()) throw new IOException(r.toString());
                    return gson.fromJson(r.body().string(), DirResult.class);
                } catch (IOException e) {
                    err = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(DirResult d) {
                if (err != null || d == null || d.routes.isEmpty()) {
                    Toast.makeText(getContext(), "Route error", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentRoute != null) currentRoute.remove();
                List<LatLng> pts = PolyUtil.decode(d.routes.get(0).overviewPolyline.points);
                currentRoute = map.addPolyline(new PolylineOptions().addAll(pts).color(0xFF0B84FF).width(10));

                String dist = d.routes.get(0).legs.get(0).distance.text;
                String eta = d.routes.get(0).legs.get(0).duration.text;
                routeBanner.setText(dist + " • " + eta);
                routeBanner.setVisibility(View.VISIBLE);

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(pts.get(pts.size() / 2), 14));
            }
        }.execute();
    }

    private void savePlaceToFirebase(NearbyResult.PlaceRes p) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "anonymous_user";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("favourites")
                .document(uid)
                .collection("locations")
                .document(p.placeId);

        ref.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Toast.makeText(getContext(), "Location already saved to favourites", Toast.LENGTH_SHORT).show();
            } else {
                ref.set(new FavoriteLocation(p.name, p.address, p.geometry.location.lat, p.geometry.location.lng, p.placeId))
                        .addOnSuccessListener(v -> {
                            showSuccessNotification(p.name);
                            Toast.makeText(getContext(), "Saved to favourites", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Error checking data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showSuccessNotification(String placeName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "fav_channel")
                .setSmallIcon(R.drawable.baseline_notifications_24) // Make sure this icon exists
                .setContentTitle("Favourites Updated")
                .setContentText(placeName + " has been added to your favourites!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(requireContext());
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "fav_channel",
                    "Favourites",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications for favourite places");

            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void recenter() {
        if (currentLoc == null || map == null) return;
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude()), 15));
        if (currentRoute != null) {
            currentRoute.remove();
            currentRoute = null;
        }
        routeBanner.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] p, @NonNull int[] g) {
        if (code == REQUEST_LOCATION && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED && map != null)
            onMapReady(map);
    }

    private static class NearbyResult {
        List<PlaceRes> results;

        static class PlaceRes {
            @SerializedName("place_id") String placeId;
            String name;
            @SerializedName("vicinity") String address;
            Geometry geometry;
        }

        static class Geometry {
            Loc location;
        }

        static class Loc {
            double lat, lng;
        }
    }

    private static class DirResult {
        List<Route> routes;

        static class Route {
            @SerializedName("overview_polyline") Overview overviewPolyline;
            List<Leg> legs;
        }

        static class Overview {
            String points;
        }

        static class Leg {
            TextVal distance, duration;
        }

        static class TextVal {
            String text;
        }
    }

    public static class FavoriteLocation {
        public String name, address, placeId;
        public double lat, lng;
        public String docId;

        private float distanceKm;

        public FavoriteLocation() {}

        public FavoriteLocation(String n, String a, double la, double ln, String pid) {
            name = n;
            address = a;
            lat = la;
            lng = ln;
            placeId = pid;
        }

        public float getDistanceKm() {
            return distanceKm;
        }

        public void setDistanceKm(float distanceKm) {
            this.distanceKm = distanceKm;
        }
    }


}
