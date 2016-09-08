package com.trafi.istorijos;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_LOCATION = 0;

    @BindView(R.id.map_view)
    MapView mapView;
    @BindView(R.id.scroll_view)
    View storyContainer;
    @BindView(R.id.image)
    ImageView storyImage;
    @BindView(R.id.text)
    TextView storyText;
    @BindView(R.id.close_button)
    View closeButton;

    LocationServices locationServices;
    MapboxMap map;
    boolean zoomedIn;
    boolean showingStory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        locationServices = LocationServices.getLocationServices(this);

        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;

                map.getUiSettings().setTiltGesturesEnabled(false);
                map.getUiSettings().setRotateGesturesEnabled(false);

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

            Api.getStoriesService().listStories().enqueue(new Callback<List<Story>>() {
                @Override
                public void onResponse(Call<List<Story>> call, Response<List<Story>> response) {
                    if (response.isSuccessful()) {
                        for (Story story : response.body()) {
                            map.addMarker(new MarkerViewOptions()
                                    .icon(IconFactory.getInstance(MainActivity.this)
                                            .fromResource(R.drawable.dot))
                                    .flat(true)
//                        .alpha((float) Math.max(0, 1 - latLng.distanceTo(new LatLng(location.getLatitude(), location.getLongitude())) / 100))
                                    .position(new LatLng(story.latitude, story.longitude)));
                        }

                        if (response.body().size() > 0) {
                            showStory(response.body().get(0));
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<Story>> call, Throwable t) {
                }
            });


        }
    }

    private void showStory(Story story) {
        map.setMyLocationEnabled(false);

        Picasso.with(this).load(story.url).fit().centerCrop().into(storyImage);
        //noinspection deprecation
        storyText.setText(Html.fromHtml(story.text));
        storyText.setMovementMethod(LinkMovementMethod.getInstance());

        storyContainer.setVisibility(View.VISIBLE);
        storyContainer.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        storyContainer.getViewTreeObserver().removeOnPreDrawListener(this);

                        storyImage.setScaleX(0.8f);
                        storyImage.setScaleY(0.8f);

                        storyImage.animate().scaleX(1).scaleY(1)
                                .setInterpolator(new OvershootInterpolator()).start();

                        storyText.setAlpha(0);
                        storyText.setTranslationY(64);

                        storyText.animate().alpha(1).translationY(0)
                                .setInterpolator(new DecelerateInterpolator()).start();
                        return true;
                    }
                });

        closeButton.setVisibility(View.VISIBLE);
        closeButton.setAlpha(0);
        closeButton.animate().alpha(1);

        showingStory = true;
    }

    @OnClick(R.id.close_button)
    void closeStory() {
        storyContainer.setVisibility(View.GONE);
        closeButton.setVisibility(View.INVISIBLE);
        map.setMyLocationEnabled(true);
        showingStory = false;
    }

    @Override
    public void onBackPressed() {
        if (showingStory) {
            closeStory();
        } else {
            super.onBackPressed();
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
