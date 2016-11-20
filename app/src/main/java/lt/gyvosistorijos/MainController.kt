package lt.gyvosistorijos

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.gms.location.LocationListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.controller_main.view.*
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.location.LocationService
import lt.gyvosistorijos.manager.RemoteConfigManager
import lt.gyvosistorijos.utils.AppEvent
import java.util.*


class MainController : Controller(), LocationListener, GoogleMap.OnMarkerClickListener {

    companion object {
        val SCREEN_NAME = "Main"
    }

    internal lateinit var showStoryAnimator: ValueAnimator
    internal lateinit var map: GoogleMap
    internal lateinit var locationService: LocationService

    internal var zoomedIn: Boolean = false
    internal var activeStory: Story? = null

    internal val storyMarkers: MutableList<Marker> = ArrayList()
    internal val markerIdToStory = HashMap<String, Story>()

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
            map.setOnMarkerClickListener(this)
        }

        val stories = StoryDb.getAll()
        map.clear()
        storyMarkers.clear()
        markerIdToStory.clear()

        val drawable = ContextCompat.getDrawable(activity, R.drawable.dot)
        val icon = BitmapDescriptorFactory.fromBitmap(drawableToBitmap(drawable))
        for (story in stories) {
            val marker = map.addMarker(MarkerOptions()
                    .icon(icon)
                    .flat(true)
                    .position(LatLng(story.latitude, story.longitude)))

            // call after #addMarker due to Mapbox SDK bug
            marker.alpha = 0f

            markerIdToStory.put(marker.id, story)
            storyMarkers.add(marker)
        }

        locationService = (activity as MainActivity).locationService
        onLocationChanged(locationService.lastLocation)
        locationService.addLocationListener(this)
        locationService.start()

        map.isMyLocationEnabled = true

        setGeofencingStories(stories)
    }

    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            return
        }

        if (!zoomedIn) {
            // Move the map camera to where the user location is
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude), 16.0f))
            zoomedIn = true
        }

        val maxDistanceMeters = RemoteConfigManager.instance.getStoryRadiusInMeters()

        for (marker in storyMarkers) {
            marker.alpha = getAlpha(marker.position, location, maxDistanceMeters)
        }

        // find 'active' story, if one exists
        val prevActiveStory = activeStory
        var closestDistanceMeters = Float.MAX_VALUE
        val userLocation = LatLng(location.latitude, location.longitude)
        for ((id, story) in markerIdToStory) {
            val distanceMeters =
                    userLocation.distanceMetersTo(LatLng(story.latitude, story.longitude))
            if (distanceMeters < closestDistanceMeters) {
                activeStory = story
                closestDistanceMeters = distanceMeters
            }
        }
        if (closestDistanceMeters > maxDistanceMeters) {
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
    override fun onMarkerClick(marker: Marker): Boolean {
        val story = markerIdToStory[marker.id]
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
            map.setOnMarkerClickListener(null)
        }
        locationService.removeLocationListener(this)
        locationService.stop()
    }

    private fun getAlpha(markerPosition: LatLng, location: Location,
                         maxDistanceMeters: Float): Float {
        return Math.max(0.5f,
                1 - markerPosition.distanceMetersTo(
                        LatLng(location.latitude, location.longitude)) / (2 * maxDistanceMeters))
    }

    private fun setGeofencingStories(stories: List<Story>) {
        val geofenceRegions = stories.map { s -> Story.toGeofenceRegion(s) }

        (activity as MainActivity).geofenceHelper.setGeofenceRegions(geofenceRegions)
    }
}

private fun LatLng.distanceMetersTo(other: LatLng): Float {
    val distance = FloatArray(1)
    Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, distance)
    return distance[0]
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        if (drawable.bitmap != null) {
            return drawable.bitmap
        }
    }

    val bitmap: Bitmap
    if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        bitmap = Bitmap.createBitmap(1, 1,
                Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
    } else {
        bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888)
    }

    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
