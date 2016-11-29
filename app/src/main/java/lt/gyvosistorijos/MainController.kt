package lt.gyvosistorijos

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.gms.location.LocationListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.layout_show_story.view.*
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.location.LocationService
import lt.gyvosistorijos.manager.RemoteConfigManager
import lt.gyvosistorijos.utils.AppEvent
import timber.log.Timber

class MainController : Controller(), LocationListener, GoogleMap.OnMarkerClickListener {

    companion object {
        val SCREEN_NAME = "Main"

        val MAX_DISTANCE_METERS by lazy {
            RemoteConfigManager.instance.getStoryRadiusInMeters()
        }
    }

    private val showStoryPresenter: ShowStoryPresenter = ShowStoryPresenter()
    internal lateinit var map: GoogleMap
    internal lateinit var locationService: LocationService

    internal var zoomedIn: Boolean = false
    internal var activeStory: Story? = null

    internal var storyMarkers = listOf<Marker>()
    internal var userLocation: LatLng? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_main, container, false)
        return view
    }

    override fun onAttach(view: View) {
        AppEvent.trackCurrentScreen(activity!!, SCREEN_NAME)

        showStoryPresenter.init(view)

        view.showStoryButton.setOnClickListener { clickShowStory() }
        view.showStoryImage.setOnClickListener { clickShowStory() }

        map = (activity as MainActivity).map
        map.setOnMarkerClickListener(this)

        val stories = StoryDb.getAll()
        map.clear()

        val storyDrawable = ContextCompat.getDrawable(activity, R.drawable.marker_story)
        val storyVisitedDrawable = ContextCompat.getDrawable(activity, R.drawable.marker_story_visited)

        val storyIcon = BitmapDescriptorFactory.fromBitmap(drawableToBitmap(storyDrawable))
        val storyVisitedIcon = BitmapDescriptorFactory.fromBitmap(drawableToBitmap(storyVisitedDrawable))

        val visitedStoryIds = StoryDb.getVisitedStories().map { it.id }.toHashSet()

        storyMarkers = stories.map { story ->
            val icon = if (visitedStoryIds.contains(story.id)) storyVisitedIcon else storyIcon
            val marker = map.addMarker(MarkerOptions()
                    .icon(icon)
                    .position(LatLng(story.latitude, story.longitude)))
            marker.tag = story

            marker
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

        for (marker in storyMarkers) {
            marker.alpha = getAlpha(marker.position, location, MAX_DISTANCE_METERS)
        }

        // find 'active' story, if one exists
        val prevActiveStory = activeStory

        userLocation = LatLng(location.latitude, location.longitude)

        val closestMarker = storyMarkers.minBy { m ->
            userLocation!!.distanceMetersTo(m.position)
        }

        if (closestMarker == null) {
            Timber.w("No closest markers found")
            return
        }

        val closestDistanceMeters = userLocation!!.distanceMetersTo(closestMarker.position)

        if (closestDistanceMeters > MAX_DISTANCE_METERS) {
            activeStory = null
        } else {
            activeStory = closestMarker.tag as Story
        }

        if (null == prevActiveStory && null != activeStory) {
            showStoryPresenter.showShowStory(view!!, activeStory!!)
        } else if (null != prevActiveStory && null == activeStory) {
            showStoryPresenter.hideShowStory(view!!)
        } else if (null != prevActiveStory && null != activeStory
                && prevActiveStory !== activeStory) {
            showStoryPresenter.updateShowStoryButton(view!!, activeStory!!)
        }
    }

    private fun createTempTravelMotivationStory(): Story {
        return Story(
                "lt.gyvosistorijos.MainController.createTempTravelMotivationStory",
                resources!!.getString(R.string.travel_to_story_cta),
                null,
                0.0, 0.0,
                resources!!.getString(R.string.travel_to_story_cta_author))
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val story = marker.tag as Story

        val distanceToStory = userLocation?.distanceMetersTo(
                LatLng(story.latitude, story.longitude))

        if ((distanceToStory != null &&
                distanceToStory < MAX_DISTANCE_METERS)
                || StoryDb.isStoryVisited(story) || BuildConfig.DEBUG) {
            showStory(story)
        } else {
            showStory(null)
        }
        return true
    }

    internal fun clickShowStory() {
        showStoryPresenter.hideShowStory(view!!)

        showStory(activeStory)
    }

    private fun showStory(story: Story?) {
        story?.let { StoryDb.setStoryVisited(it) }

        val selectedStory = story ?: createTempTravelMotivationStory()

        AppEvent.trackStoryClicked(activity!!, selectedStory.id)

        router.pushController(RouterTransaction.with(StoryController(selectedStory)))
    }

    override fun onDetach(view: View) {
        map.setOnMarkerClickListener(null)
        locationService.removeLocationListener(this)
        locationService.stop()
        showStoryPresenter.deinit()
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
