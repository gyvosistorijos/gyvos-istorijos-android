package lt.gyvosistorijos

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.gms.location.LocationListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.android.synthetic.main.layout_show_story.view.*
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.location.LocationService
import lt.gyvosistorijos.manager.RemoteConfigManager
import lt.gyvosistorijos.storage.StoryDb
import lt.gyvosistorijos.utils.*
import timber.log.Timber

class MainController(args: Bundle) : Controller(args), LocationListener,
        GoogleMap.OnMarkerClickListener {

    companion object {
        val SCREEN_NAME = "Main"
        val KEY_ANIMATE_ZOOM_IN = "animateZoomIn"

        val MAX_DISTANCE_METERS by lazy {
            RemoteConfigManager.instance.getStoryRadiusInMeters()
        }
    }

    private val animateZoomIn: Boolean = args.getBoolean(KEY_ANIMATE_ZOOM_IN, false)

    constructor(animateZoomIn: Boolean)
            : this(Bundle().set { bool(KEY_ANIMATE_ZOOM_IN to animateZoomIn) })

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
        val visitedStoryIds = StoryDb.getVisitedStories().map { it.id }.toHashSet()
        val unvisitedStories = stories.filterNot { visitedStoryIds.contains(it.id) }

        map.clear()
        storyMarkers = addTaggedStoryMarkers(view.context, map, stories)

        locationService = (activity as MainActivity).locationService
        locationService.attach(this)

        map.isMyLocationEnabled = true

        setGeofencingStories(unvisitedStories)
    }

    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            return
        }

        if (!zoomedIn) {
            // move the map camera to where the user location is
            val update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    resources!!.getFloat(R.dimen.narrow_map_zoom))
            if (animateZoomIn) {
                map.animateCamera(update)
            } else {
                map.moveCamera(update)
            }
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

        val distanceToStory = userLocation?.distanceMetersTo(story.latitude, story.longitude)

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
        locationService.detach(this)
        showStoryPresenter.deinit()
    }

    private fun getAlpha(markerPosition: LatLng, location: Location,
                         maxDistanceMeters: Float): Float {
        return Math.max(0.5f,
                1 - markerPosition.distanceMetersTo(location.latitude, location.longitude)
                        / (2 * maxDistanceMeters))
    }

    private fun setGeofencingStories(stories: List<Story>) {
        val geofenceRegions = stories.map { s -> Story.toGeofenceRegion(s) }

        (activity as MainActivity).geofenceHelper.setGeofenceRegions(geofenceRegions)
    }
}
