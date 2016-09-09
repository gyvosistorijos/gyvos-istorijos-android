package com.trafi.istorijos;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements MapboxMap.OnMarkerViewClickListener {

    private static final int PERMISSIONS_LOCATION = 0;

    @BindView(R.id.map_view)
    MapView mapView;
    @BindView(R.id.scroll_view)
    View storyContainer;
    @BindView(R.id.image)
    ImageView storyImage;
    @BindView(R.id.text)
    TextView storyText;
    @BindView(R.id.hide_story_button)
    View hideStoryButton;

    @BindView(R.id.show_story_image)
    ImageView showStoryImage;
    @BindView(R.id.show_story_button)
    View showStoryButton;
    @BindDimen(R.dimen.image_height)
    int imageHeight;
    @BindDimen(R.dimen.attractor_height_offset)
    int attractorHeightOffset;
    @BindDimen(R.dimen.attractor_height_delta)
    int attractorHeightDelta;

    LocationServices locationServices;
    MapboxMap map;
    boolean zoomedIn;
    boolean initialized;
    boolean showingStory;

    List<MarkerView> storyMarkers = new ArrayList<>();
    LongSparseArray<Story> markerIdToStory = new LongSparseArray<>();

    @Nullable
    Story activeStory;

    ValueAnimator showStoryAnimator;

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

                map.getMarkerViewManager().setOnMarkerViewClickListener(MainActivity.this);

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

        showStoryAnimator = ValueAnimator.ofFloat(imageHeight - attractorHeightOffset - attractorHeightDelta, imageHeight - attractorHeightOffset);
        showStoryAnimator.setRepeatCount(ValueAnimator.INFINITE);
        showStoryAnimator.setRepeatMode(ValueAnimator.REVERSE);
        showStoryAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        showStoryAnimator.setDuration(1400);
        showStoryAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                showStoryImage.setTranslationY(value);
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
            onUserLocationUpdated(locationServices.getLastLocation());
            locationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    onUserLocationUpdated(location);
                }
            });
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    private float getAlpha(LatLng markerPosition, Location location) {
        return (float) Math.max(0.4,
                1 - markerPosition.distanceTo(
                        new LatLng(location.getLatitude(), location.getLongitude())) / 1000);
    }

    private void onUserLocationUpdated(@Nullable final Location location) {
        if (location == null) {
            return;
        }

        if (!zoomedIn) {
            // Move the map camera to where the user location is
            map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(location))
                    .zoom(16)
                    .build());
            zoomedIn = true;
        }

        if (!initialized) {
            Api.getStoriesService().listStories().enqueue(new Callback<List<Story>>() {
                @Override
                public void onResponse(Call<List<Story>> call, Response<List<Story>> response) {
                    if (response.isSuccessful() && !initialized) {
                        List<Story> stories = response.body();
                        for (Story story : stories) {
                            MarkerView marker = map.addMarker(new MarkerViewOptions()
                                    .icon(IconFactory.getInstance(MainActivity.this)
                                            .fromResource(R.drawable.dot))
                                    .flat(true)
                                    .position(new LatLng(story.latitude, story.longitude)));

                            marker.setAlpha(getAlpha(marker.getPosition(), location));

                            markerIdToStory.put(marker.getId(), story);
                            storyMarkers.add(marker);
                        }
                        initialized = true;
                    }
                }

                @Override
                public void onFailure(Call<List<Story>> call, Throwable t) {
                    Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            for (MarkerView marker : storyMarkers) {
                marker.setAlpha(getAlpha(marker.getPosition(), location));
            }

            // find 'active' story, if one exists
            Story prevActiveStory = activeStory;
            double closestDistanceMeters = Double.MAX_VALUE;
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            for (int i = 0; i < markerIdToStory.size(); i++) {
                Story story = markerIdToStory.valueAt(i);

                double distanceMeters = userLocation.distanceTo(new LatLng(story.latitude, story.longitude));
                if (distanceMeters < closestDistanceMeters) {
                    activeStory = story;
                    closestDistanceMeters = distanceMeters;
                }
            }
            if (null != activeStory && closestDistanceMeters > 1000) {
                activeStory = null;
            }

            if (null == prevActiveStory && null != activeStory) {
                if (!showingStory) {
                    showShowStory();
                }
            } else if (null != prevActiveStory && null == activeStory) {
                if (showingStory) {
                    clickHideStory();
                } else {
                    hideShowStory();
                }
            } else if (null != prevActiveStory && null != activeStory && prevActiveStory != activeStory) {
                updateShowStoryButton();
            }
        }
    }

    private void showShowStory() {
        showStoryAnimator.start();
        showStoryButton.setVisibility(View.VISIBLE);
        showStoryImage.setVisibility(View.VISIBLE);
        updateShowStoryButton();
    }

    private void updateShowStoryButton() {
        Picasso.with(MainActivity.this).load(activeStory.url).fit().centerCrop().into(showStoryImage);
    }

    private void hideShowStory() {
        showStoryAnimator.cancel();
        showStoryButton.setVisibility(View.INVISIBLE);
        showStoryImage.setVisibility(View.INVISIBLE);
    }

    @OnClick({R.id.show_story_button, R.id.show_story_image})
    void clickShowStory() {
        hideShowStory();
        showStory(activeStory);
    }

    private void showStory(Story story) {
        map.setMyLocationEnabled(false);

        Picasso.with(this).load(story.url).fit().centerCrop().into(storyImage);
        //noinspection deprecation
        storyText.setText(Html.fromHtml(story.text));
        Linkify.addLinks(storyText, Linkify.WEB_URLS);

        storyContainer.setVisibility(View.VISIBLE);
        storyContainer.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        storyContainer.getViewTreeObserver().removeOnPreDrawListener(this);

                        storyImage.setScaleX(0.8f);
                        storyImage.setScaleY(0.8f);

                        storyImage.animate().scaleX(1).scaleY(1)
                                .setInterpolator(new OvershootInterpolator())
                                .setListener(null)
                                .start();

                        storyText.setAlpha(0);
                        storyText.setTranslationY(64);

                        storyText.animate().alpha(1).translationY(0)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(null)
                                .start();
                        return true;
                    }
                });

        hideStoryButton.setVisibility(View.VISIBLE);
        hideStoryButton.setAlpha(0);
        hideStoryButton.animate().setListener(null).alpha(1);

        showingStory = true;
    }

    @OnClick(R.id.hide_story_button)
    void clickHideStory() {
        hideStoryButton.animate().alpha(0).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideStoryButton.setVisibility(View.INVISIBLE);
                    }
                });

        storyImage.animate().scaleX(0f).scaleY(0f)
                .setInterpolator(new AccelerateInterpolator());
        storyText.animate().alpha(0).translationY(64)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        storyContainer.setVisibility(View.GONE);
                    }
                });

        map.setMyLocationEnabled(true);
        showingStory = false;
        activeStory = null;
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker, @NonNull View view,
                                 @NonNull MapboxMap.MarkerViewAdapter adapter) {
        Story story = markerIdToStory.get(marker.getId());
        if (null != story) {
            showStory(story);
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (showingStory) {
            clickHideStory();
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
