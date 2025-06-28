package com.example.atm_finder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class FavouriteAdapter extends RecyclerView.Adapter<FavouriteAdapter.ViewHolder> {

    public interface Listener {
        void onShow(HomeFragment.FavoriteLocation location);
        void onDelete(HomeFragment.FavoriteLocation location);
    }

    private final List<HomeFragment.FavoriteLocation> locations;
    private final Listener listener;

    public FavouriteAdapter(List<HomeFragment.FavoriteLocation> locations, Listener listener) {
        this.locations = locations;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtAddress, txtDistance;
        Button   btnShow, btnDelete;

        public ViewHolder(View v) {
            super(v);
            txtName     = v.findViewById(R.id.txt_name);
            txtAddress  = v.findViewById(R.id.txt_address);
            txtDistance = v.findViewById(R.id.txt_distance);
            btnShow     = v.findViewById(R.id.btn_show);
            btnDelete   = v.findViewById(R.id.btn_delete);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favourite, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        HomeFragment.FavoriteLocation item = locations.get(pos);

        h.txtName.setText(item.name);
        h.txtAddress.setText(item.address);
        h.txtDistance.setText(String.format(Locale.getDefault(), "%.2f km away", item.getDistanceKm()));

        h.btnShow.setOnClickListener(v -> listener.onShow(item));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }
}
