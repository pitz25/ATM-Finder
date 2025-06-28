package com.example.atm_finder;
public class FavoriteLocation {
    public String name, address;
    public double lat, lng;
    public String docId;
    public float distanceKm;

    public FavoriteLocation() {}
    public FavoriteLocation(String name, String address, double lat, double lng) {
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
    }
}
