package com.example.atm_finder;

import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NearbySearchHelper {

    public static class ApiResult {
        public List<Result> results;
    }
    public static class Result {
        public Geometry geometry;
        public String name;
        @SerializedName("vicinity") public String address;
    }
    public static class Geometry {
        public LocationObj location;
    }
    public static class LocationObj {
        public double lat;
        public double lng;
    }

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    /* Blocking network call  */
    @WorkerThread
    public static ApiResult fetch(double lat, double lng, int radiusMeters,
                                  String keyword, String apiKey) throws IOException {

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=" + radiusMeters
                + "&keyword=" + keyword.replace(" ", "%20")
                + "&key=" + apiKey;

        Request req = new Request.Builder().url(url).build();
        Response res = client.newCall(req).execute();
        if (!res.isSuccessful()) throw new IOException(res.toString());

        return gson.fromJson(res.body().string(), ApiResult.class);
    }
}
