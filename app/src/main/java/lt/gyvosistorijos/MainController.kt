package lt.gyvosistorijos

import android.animation.ValueAnimator
import android.location.Location
import android.support.v4.util.LongSparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerView
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationListener
import com.mapbox.mapboxsdk.location.LocationServices
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.controller_main.view.*
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.location.GeofenceHelper
import lt.gyvosistorijos.manager.RemoteConfigManager
import lt.gyvosistorijos.utils.AppEvent
import java.util.*

class MainController : Controller(), MapboxMap.OnMarkerViewClickListener, LocationListener {

    companion object {
        val SCREEN_NAME = "Main"
    }

    internal lateinit var showStoryAnimator: ValueAnimator
    internal lateinit var map: MapboxMap
    internal lateinit var locationServices: LocationServices

    internal var zoomedIn: Boolean = false
    internal var activeStory: Story? = null

    internal val storyMarkers: MutableList<MarkerView> = ArrayList()
    internal val markerIdToStory = LongSparseArray<Story>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_main, container, false)
        return view
    }

    override fun onAttach(view: View) {
        AppEvent.trackCurrentScreen(activity!!, SCREEN_NAME)

        val imageHeight = resources!!.getDimensionPixelSize(R.dimen.image_height)
        val attractorHeightOffset =
                resources!!.getDimensionPixelOffset(R.dimen.attractor_height_offset)
        val attractorHeightDelta =
                resources!!.getDimensionPixelOffset(R.dimen.attractor_height_delta)

        showStoryAnimator = ValueAnimator.ofFloat(
                (imageHeight - attractorHeightOffset - attractorHeightDelta).toFloat(),
                (imageHeight - attractorHeightOffset).toFloat())
        showStoryAnimator.repeatCount = ValueAnimator.INFINITE
        showStoryAnimator.repeatMode = ValueAnimator.REVERSE
        showStoryAnimator.interpolator = AccelerateDecelerateInterpolator()
        showStoryAnimator.duration = 1400
        showStoryAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            view.showStoryImage.translationY = value
        }

        view.showStoryButton.setOnClickListener { clickShowStory() }
        view.showStoryImage.setOnClickListener { clickShowStory() }

        map = (activity as MainActivity).map
        if (BuildConfig.DEBUG) {
            map.markerViewManager.setOnMarkerViewClickListener(this)
        }

        val stories = StoryDb.getAll()
        map.clear()
        storyMarkers.clear()
        markerIdToStory.clear()
        for (story in stories) {
            val marker = map.addMarker(MarkerViewOptions()
                    .icon(IconFactory.getInstance(activity!!)
                            .fromResource(R.drawable.dot))
                    .flat(true)
                    .position(LatLng(story.latitude, story.longitude)))

            // call after #addMarker due to Mapbox SDK bug
            marker.alpha = 0f

            markerIdToStory.put(marker.id, story)
            storyMarkers.add(marker)
        }

        locationServices = LocationServices.getLocationServices(applicationContext!!)
        onLocationChanged(locationServices.lastLocation)
        locationServices.addLocationListener(this)
        map.isMyLocationEnabled = true

        setGeofencingStories(stories)
    }

    override fun onLocationChanged(location: Location?) {
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
            showShowStory()
        } else if (null != prevActiveStory && null == activeStory) {
            hideShowStory()
        } else if (null != prevActiveStory && null != activeStory
                && prevActiveStory !== activeStory) {
            updateShowStoryButton()
        }
    }

    // debug builds only
    override fun onMarkerClick(marker: Marker, view: View,
                               adapter: MapboxMap.MarkerViewAdapter<*>): Boolean {
        val story = markerIdToStory.get(marker.id)
        if (null != story) {
            router.pushController(RouterTransaction.with(StoryController(story)))
            return true
        }
        return false
    }

    private fun showShowStory() {
        showStoryAnimator.start()
        view!!.showStoryButton.visibility = View.VISIBLE
        view!!.showStoryImage.visibility = View.VISIBLE
        updateShowStoryButton()
    }

    private fun updateShowStoryButton() {
        Picasso.with(activity)
                .load(activeStory!!.url).fit().centerCrop().into(view!!.showStoryImage)
    }

    private fun hideShowStory() {
        showStoryAnimator.cancel()
        view!!.showStoryButton.visibility = View.INVISIBLE
        view!!.showStoryImage.visibility = View.INVISIBLE
    }

    internal fun clickShowStory() {
        hideShowStory()

        AppEvent.trackStoryClicked(activity!!, activeStory!!.id)
        router.pushController(RouterTransaction.with(StoryController(activeStory!!)))
    }

    override fun onDetach(view: View) {
        if (BuildConfig.DEBUG) {
            map.markerViewManager.setOnMarkerViewClickListener(null)
        }
        locationServices.removeLocationListener(this)
    }

    private fun getAlpha(markerPosition: LatLng, location: Location): Float {
        return Math.max(0.5,
                1 - markerPosition.distanceTo(
                        LatLng(location.latitude, location.longitude)) / 1000).toFloat()
    }

    private fun setGeofencingStories(stories: List<Story>) {
        val mainActivity = activity as MainActivity

        mainActivity.geofenceHelper =
                GeofenceHelper(mainActivity, RemoteConfigManager.instance)

        val geofenceRegions = stories.map { s -> Story.toGeofenceRegion(s) }

        mainActivity.geofenceHelper.setGeofenceRegions(geofenceRegions)
    }
}
