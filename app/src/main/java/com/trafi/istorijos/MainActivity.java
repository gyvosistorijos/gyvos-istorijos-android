package com.trafi.istorijos;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_LOCATION = 0;

    MapView mapView;
    MapboxMap map;
    LocationServices locationServices;
    boolean zoomedIn;

    static List<LatLng> coordinates;

    static {
        Random rand = new Random();
        coordinates = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            coordinates.add(new LatLng(
                    54.6872 + 2 * (rand.nextDouble() - 0.5) * 0.01,
                    25.2797 + 2 * (rand.nextDouble() - 0.5) * 0.01));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        locationServices = LocationServices.getLocationServices(this);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;

                // Check if user has granted location permission
                if (!locationServices.areLocationPermissionsGranted()) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
                } else {
                    enableLocation(true);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_LOCATION: {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocation(true);
                }
            }
        }
    }

    private void enableLocation(boolean enabled) {
        if (enabled) {
            setMapCameraPosition(locationServices.getLastLocation());
            locationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    setMapCameraPosition(location);
                }
            });
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    private void setMapCameraPosition(@Nullable Location location) {
        if (location != null && !zoomedIn) {
            // Move the map camera to where the user location is
            map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(location))
                    .zoom(16)
                    .build());
            zoomedIn = true;

            for (LatLng latLng : coordinates) {
                map.addMarker(new MarkerViewOptions()
                        .icon(IconFactory.getInstance(MainActivity.this)
                                .fromResource(R.drawable.dot))
                        .flat(true)
//                        .alpha((float) Math.max(0, 1 - latLng.distanceTo(new LatLng(location.getLatitude(), location.getLongitude())) / 100))
                        .position(latLng));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
