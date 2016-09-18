package com.trafi.istorijos

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.util.LongSparseArray
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.util.Linkify
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerView
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationServices
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MainActivity : AppCompatActivity(), MapboxMap.OnMarkerViewClickListener {

    companion object {
        private val PERMISSIONS_LOCATION = 0

        internal val REQUEST_IMAGE_CAPTURE = 1
    }

    internal lateinit var locationServices: LocationServices
    internal lateinit var map: MapboxMap
    internal var zoomedIn: Boolean = false
    internal var initialized: Boolean = false
    internal var showingStory: Boolean = false
    internal var addingStory: Boolean = false

    internal var storyMarkers: MutableList<MarkerView> = ArrayList()
    internal var markerIdToStory = LongSparseArray<Story>()

    internal var activeStory: Story? = null

    internal lateinit var showStoryAnimator: ValueAnimator

    internal var lastLat: Double = 0.toDouble()
    internal var lastLng: Double = 0.toDouble()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationServices = LocationServices.getLocationServices(this)

        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            map.uiSettings.isTiltGesturesEnabled = false
            map.uiSettings.isRotateGesturesEnabled = false

            map.markerViewManager.setOnMarkerViewClickListener(this@MainActivity)

            // Check if user has granted location permission
            if (!locationServices.areLocationPermissionsGranted()) {
                ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSIONS_LOCATION)
            } else {
                enableLocation(true)
            }
        }

        val imageHeight = resources.getDimensionPixelSize(R.dimen.image_height)
        val attractorHeightOffset =
                resources.getDimensionPixelOffset(R.dimen.attractor_height_offset)
        val attractorHeightDelta =
                resources.getDimensionPixelOffset(R.dimen.attractor_height_delta)

        showStoryAnimator = ValueAnimator.ofFloat(
                (imageHeight - attractorHeightOffset - attractorHeightDelta).toFloat(),
                (imageHeight - attractorHeightOffset).toFloat())
        showStoryAnimator.repeatCount = ValueAnimator.INFINITE
        showStoryAnimator.repeatMode = ValueAnimator.REVERSE
        showStoryAnimator.interpolator = AccelerateDecelerateInterpolator()
        showStoryAnimator.duration = 1400
        showStoryAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            showStoryImage.translationY = value
        }

        showStoryButton.setOnClickListener { clickShowStory() }
        showStoryImage.setOnClickListener { clickShowStory() }
        addPhotoButton.setOnClickListener { addPhoto() }
        hideStoryButton.setOnClickListener { clickHideButton() }
        submitStoryButton.setOnClickListener { clickSubmitButton() }
        addStoryButton.setOnClickListener { clickAddStory() }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_LOCATION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocation(true)
                }
            }
        }
    }

    private fun enableLocation(enabled: Boolean) {
        if (enabled) {
            onUserLocationUpdated(locationServices.lastLocation)
            locationServices.addLocationListener { location -> onUserLocationUpdated(location) }
        }
        // Enable or disable the location layer on the map
        map.isMyLocationEnabled = enabled
    }

    private fun getAlpha(markerPosition: LatLng, location: Location): Float {
        return Math.max(0.5,
                1 - markerPosition.distanceTo(
                        LatLng(location.latitude, location.longitude)) / 1000).toFloat()
    }

    private fun onUserLocationUpdated(location: Location?) {
        if (location == null) {
            return
        }
        lastLat = location.latitude
        lastLng = location.longitude

        if (!zoomedIn) {
            // Move the map camera to where the user location is
            map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(location)).zoom(16.0)
                    .build()
            zoomedIn = true
        }

        if (!initialized) {
            Api.getStoriesService().listStories().enqueue(object : Callback<List<Story>> {
                override fun onResponse(call: Call<List<Story>>, response: Response<List<Story>>) {
                    if (response.isSuccessful && !initialized) {
                        val stories = response.body()
                        for (story in stories) {
                            val marker = map.addMarker(MarkerViewOptions()
                                    .icon(IconFactory.getInstance(this@MainActivity)
                                            .fromResource(R.drawable.dot))
                                    .flat(true)
                                    .position(LatLng(story.latitude, story.longitude)))

                            // call after #addMarker due to Mapbox SDK bug
                            marker.alpha = getAlpha(marker.position, location)

                            markerIdToStory.put(marker.id, story)
                            storyMarkers.add(marker)
                        }
                        initialized = true
                    }
                }

                override fun onFailure(call: Call<List<Story>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            for (marker in storyMarkers) {
                marker.alpha = getAlpha(marker.position, location)
            }

            // find 'active' story, if one exists
            val prevActiveStory = activeStory
            var closestDistanceMeters = java.lang.Double.MAX_VALUE
            val userLocation = LatLng(location.latitude, location.longitude)
            for (i in 0..markerIdToStory.size() - 1) {
                val story = markerIdToStory.valueAt(i)

                val distanceMeters =
                        userLocation.distanceTo(LatLng(story.latitude, story.longitude))
                if (distanceMeters < closestDistanceMeters) {
                    activeStory = story
                    closestDistanceMeters = distanceMeters
                }
            }
            if (null != activeStory && closestDistanceMeters > 1000) {
                activeStory = null
            }

            if (null == prevActiveStory && null != activeStory) {
                if (!showingStory && !addingStory) {
                    showShowStory()
                }
            } else if (null != prevActiveStory && null == activeStory) {
                if (showingStory) {
                    clickHideButton()
                } else {
                    hideShowStory()
                }
            } else if (null != prevActiveStory && null != activeStory
                    && prevActiveStory !== activeStory) {
                updateShowStoryButton()
            }
        }
    }

    private fun showShowStory() {
        showStoryAnimator.start()
        showStoryButton.visibility = View.VISIBLE
        showStoryImage.visibility = View.VISIBLE
        updateShowStoryButton()
    }

    private fun updateShowStoryButton() {
        Picasso.with(this@MainActivity)
                .load(activeStory!!.url).fit().centerCrop().into(showStoryImage)
    }

    private fun hideShowStory() {
        showStoryAnimator.cancel()
        showStoryButton.visibility = View.INVISIBLE
        showStoryImage.visibility = View.INVISIBLE
    }

    internal fun clickShowStory() {
        hideShowStory()
        showStory(activeStory!!)
    }

    private fun showStory(story: Story) {
        map.isMyLocationEnabled = false

        Picasso.with(this).load(story.url).fit().centerCrop().into(storyImage)
        storyText.text = @Suppress("DEPRECATION") (Html.fromHtml(story.text))
        Linkify.addLinks(storyText, Linkify.WEB_URLS)

        storyContainer.visibility = View.VISIBLE
        storyContainer.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        storyContainer.viewTreeObserver.removeOnPreDrawListener(this)

                        storyImage.scaleX = 0.8f
                        storyImage.scaleY = 0.8f

                        storyImage.animate()
                                .scaleX(1f).scaleY(1f)
                                .setInterpolator(OvershootInterpolator())
                                .setListener(null)
                                .start()

                        storyText.alpha = 0f
                        storyText.translationY = 64f

                        storyText.animate()
                                .alpha(1f).translationY(0f)
                                .setInterpolator(DecelerateInterpolator())
                                .setListener(null)
                                .start()
                        return true
                    }
                })

        addStoryButton.visibility = View.INVISIBLE
        hideStoryButton.visibility = View.VISIBLE
        hideStoryButton.alpha = 0f
        hideStoryButton.animate()
                .alpha(1f)
                .setListener(null)

        showingStory = true
    }

    internal fun addPhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val extras = data.extras
            val imageBitmap = extras.get("data") as Bitmap
            submitStoryImage.setImageBitmap(imageBitmap)
        }
    }

    internal fun clickHideButton() {
        if (addingStory) {
            submitStoryContainer.visibility = View.INVISIBLE
            hideStoryButton.visibility = View.INVISIBLE
            addStoryButton.visibility = View.VISIBLE
            activeStory = null
            addingStory = false
            map.isMyLocationEnabled = true

            submitStoryImage.setImageResource(0)
            submitStoryEditText.setText("")
        } else if (showingStory) {
            hideStoryButton.animate()
                    .alpha(0f)
                    .setListener(
                            object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    hideStoryButton.visibility = View.INVISIBLE
                                    addStoryButton.visibility = View.VISIBLE
                                }
                            })

            storyImage.animate()
                    .scaleX(0f).scaleY(0f)
                    .interpolator = AccelerateInterpolator()
            storyText.animate()
                    .alpha(0f).translationY(64f)
                    .setInterpolator(AccelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            storyContainer.visibility = View.GONE
                        }
                    })

            map.isMyLocationEnabled = true
            showingStory = false
            activeStory = null
        }
    }

    internal fun clickSubmitButton() {
        val story = Story(
                id = "",
                text = submitStoryEditText.text.toString(),
                url = "http://istorijosmessengerbot.azurewebsites.net/kairys.jpg",
                latitude = lastLat,
                longitude = lastLng)
        Api.getStoriesService().createStory(story).enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                Toast.makeText(this@MainActivity, t?.message, Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@MainActivity,
                            "Error " + response.code(), Toast.LENGTH_SHORT)
                            .show()
                } else {
                    Toast.makeText(this@MainActivity,
                            "Ačiū! Tavo istorija padaro miesta gyvesniu.", Toast.LENGTH_SHORT)
                            .show()
                }
            }
        })
        clickHideButton()
    }

    internal fun clickAddStory() {
        hideShowStory()
        map.isMyLocationEnabled = false
        submitStoryContainer.visibility = View.VISIBLE
        submitStoryContainer.alpha = 0f
        submitStoryContainer.animate().alpha(1f)

        hideStoryButton.visibility = View.VISIBLE
        addStoryButton.visibility = View.INVISIBLE

        addingStory = true
    }

    override fun onMarkerClick(marker: Marker, view: View,
                               adapter: MapboxMap.MarkerViewAdapter<*>): Boolean {
        val story = markerIdToStory.get(marker.id)
        if (null != story) {
            showStory(story)
            return true
        }
        return false
    }

    override fun onBackPressed() {
        if (showingStory || addingStory) {
            clickHideButton()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
