package com.example.atm_finder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavouriteFragment extends Fragment {

    private static final int LOCATION_PERMISSION_CODE = 99;

    private RecyclerView recyclerView;
    private FavouriteAdapter adapter;
    private final List<HomeFragment.FavoriteLocation> favoriteList = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_favourite, container, false);

        recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FavouriteAdapter(favoriteList, new FavouriteAdapter.Listener() {
            @Override
            public void onShow(HomeFragment.FavoriteLocation loc) {
                openMap(loc);
            }

            @Override
            public void onDelete(HomeFragment.FavoriteLocation loc) {
                deleteItem(loc);
            }
        });

        recyclerView.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            fetchLocationAndFavorites();
        }

        return root;
    }

    private void fetchLocationAndFavorites() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            currentLocation = loc;
            loadSavedATMs();
        });
    }

    private void loadSavedATMs() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "anonymous_user";

        FirebaseFirestore.getInstance()
                .collection("favourites").document(uid)
                .collection("locations")
                .get()
                .addOnSuccessListener(query -> {
                    favoriteList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        HomeFragment.FavoriteLocation loc = doc.toObject(HomeFragment.FavoriteLocation.class);
                        loc.docId = doc.getId();

                        // Calculate and set distance
                        if (currentLocation != null) {
                            float[] result = new float[1];
                            Location.distanceBetween(
                                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                                    loc.lat, loc.lng, result);
                            loc.setDistanceKm(result[0] / 1000f); // Use setter method
                        }

                        favoriteList.add(loc);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteItem(HomeFragment.FavoriteLocation location) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "anonymous_user";

        FirebaseFirestore.getInstance()
                .collection("favourites").document(uid)
                .collection("locations").document(location.docId)
                .delete()
                .addOnSuccessListener(v -> {
                    favoriteList.remove(location);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Removed from Favourite", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openMap(HomeFragment.FavoriteLocation loc) {
        Intent intent = new Intent(getContext(), MapsActivity.class);
        intent.putExtra("dest_lat", loc.lat);
        intent.putExtra("dest_lng", loc.lng);
        intent.putExtra("dest_name", loc.name);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] res) {
        if (requestCode == LOCATION_PERMISSION_CODE && res.length > 0 &&
                res[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndFavorites();
        }
    }
}
