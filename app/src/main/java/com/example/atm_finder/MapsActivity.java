package com.example.atm_finder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.annotations.SerializedName;
import com.google.maps.android.PolyUtil;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION = 44;
    private GoogleMap map;
    private FusedLocationProviderClient fused;
    private LatLng destLatLng;
    private String destName;
    private TextView txtInfo;
    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_maps);

        destLatLng = new LatLng(
                getIntent().getDoubleExtra("dest_lat", 0),
                getIntent().getDoubleExtra("dest_lng", 0));
        destName   = getIntent().getStringExtra("dest_name");

        txtInfo = findViewById(R.id.txt_info);
        fused   = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mf = (SupportMapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mf != null) mf.getMapAsync(this);
    }

    @Override public void onMapReady(@NonNull GoogleMap g) {
        map = g;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }
        map.setMyLocationEnabled(true);

        fused.getLastLocation().addOnSuccessListener(loc -> {
            if (loc == null) { Toast.makeText(this,"Location missing",Toast.LENGTH_SHORT).show(); return; }
            LatLng start = new LatLng(loc.getLatitude(), loc.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 14));
            map.addMarker(new MarkerOptions()
                    .position(destLatLng).title(destName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            fetchDirections(start, destLatLng);
        });
    }

    private void fetchDirections(LatLng start, LatLng end) {
        String url = "https://maps.googleapis.com/maps/api/directions/json"
                + "?origin=" + start.latitude  + "," + start.longitude
                + "&destination=" + end.latitude + "," + end.longitude
                + "&mode=driving"
                + "&key=" + "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI";

        new AsyncTask<Void,Void,DirResult>() {
            IOException err;
            @Override protected DirResult doInBackground(Void... v){
                try(Response r = http.newCall(new Request.Builder().url(url).build()).execute()){
                    if(!r.isSuccessful()) throw new IOException(r.toString());
                    return gson.fromJson(r.body().string(), DirResult.class);
                } catch(IOException e){ err=e; return null; }
            }
            @Override protected void onPostExecute(DirResult d){
                if(err!=null||d==null||d.routes.isEmpty()){
                    Toast.makeText(MapsActivity.this,"Route error",Toast.LENGTH_SHORT).show();return;}
                DirResult.Route route = d.routes.get(0);
                List<LatLng> pts = PolyUtil.decode(route.overviewPolyline.points);
                map.addPolyline(new PolylineOptions().addAll(pts).color(0xFF0B84FF).width(10));

                String dist  = route.legs.get(0).distance.text;
                String dur   = route.legs.get(0).duration.text;
                txtInfo.setText(dist + " • " + dur);
            }
        }.execute();
    }

    /* JSON model for Directions API */
    private static class DirResult {
        List<Route> routes;

        static class Route {
            @SerializedName("overview_polyline")
            Overview overviewPolyline;
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

}
