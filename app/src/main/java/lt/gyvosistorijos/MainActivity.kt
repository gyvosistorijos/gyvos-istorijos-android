package lt.gyvosistorijos

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.util.LongSparseArray
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.util.Linkify
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MainActivity : AppCompatActivity(), MapboxMap.OnMarkerViewClickListener {

    companion object {
        private val PERMISSIONS_LOCATION = 0
    }

    internal lateinit var locationServices: LocationServices
    internal lateinit var map: MapboxMap

    internal var zoomedIn: Boolean = false
    internal var initialized: Boolean = false
    internal var showingStory: Boolean = false

    internal val storyMarkers: MutableList<MarkerView> = ArrayList()
    internal val markerIdToStory = LongSparseArray<Story>()

    internal var activeStory: Story? = null

    internal val storyAnimator: StoryAnimator = StoryAnimator()
    internal lateinit var showStoryAnimator: ValueAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationServices = LocationServices.getLocationServices(this)

        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            map.uiSettings.isTiltGesturesEnabled = false
            map.uiSettings.isRotateGesturesEnabled = false

            if (BuildConfig.DEBUG) {
                map.markerViewManager.setOnMarkerViewClickListener(this@MainActivity)
            }

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
        hideStoryButton.setOnClickListener { clickHideButton() }
    }

    // debug builds only
    override fun onMarkerClick(marker: Marker, view: View,
                               adapter: MapboxMap.MarkerViewAdapter<*>): Boolean {
        val story = markerIdToStory.get(marker.id)
        if (null != story) {
            showStory(story)
            return true
        }
        return false
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
                if (!showingStory) {
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

        if (!story.url.isNullOrBlank()) {
            storyImage.visibility = View.VISIBLE
            Picasso.with(this).load(story.url)
                    .fit().centerCrop()
                    .placeholder(R.color.imagePlaceholder)
                    .into(storyImage)
        } else {
            storyImage.visibility = View.GONE
        }

        storyText.text = @Suppress("DEPRECATION") (Html.fromHtml(story.text))
        Linkify.addLinks(storyText, Linkify.WEB_URLS)

        if (!story.author.isNullOrBlank()) {
            storyAuthor.text = story.author
            storyAuthor.visibility = View.VISIBLE
            storyAuthorImage.visibility = View.VISIBLE
        } else {
            storyAuthor.visibility = View.GONE
            storyAuthorImage.visibility = View.GONE
        }

        storyContainer.visibility = View.VISIBLE
        storyAnimator.animateInStory(storyContainer)

        hideStoryButton.visibility = View.VISIBLE
        hideStoryButton.alpha = 0f
        hideStoryButton.animate()
                .alpha(1f)
                .setListener(null)

        showingStory = true
    }

    internal fun clickHideButton() {
        hideStoryButton.animate()
                .alpha(0f)
                .setListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                hideStoryButton.visibility = View.INVISIBLE
                            }
                        })

        storyAnimator.animateOutStory(storyContainer, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                storyContainer.visibility = View.GONE
                storyContainer.scrollTo(0, 0)
            }
        })

        map.isMyLocationEnabled = true
        showingStory = false
        activeStory = null
    }

    override fun onBackPressed() {
        if (showingStory) {
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
