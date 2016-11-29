package lt.gyvosistorijos.ui.onboarding

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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.controller_onboarding_intro.view.*
import kotlinx.android.synthetic.main.layout_show_story.view.*
import lt.gyvosistorijos.MainActivity
import lt.gyvosistorijos.R
import lt.gyvosistorijos.ShowStoryPresenter
import lt.gyvosistorijos.StoryController
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.location.LocationService
import lt.gyvosistorijos.storage.StoryDb
import lt.gyvosistorijos.utils.*

class OnboardingMechanicController : Controller(), LocationListener {

    companion object {
        val SCREEN_NAME = "Onboarding mechanic"
    }

    private val showStoryPresenter: ShowStoryPresenter = ShowStoryPresenter()
    private var nearestStory: Story? = null
    private var latestLocation: LatLng? = null

    internal lateinit var locationService: LocationService
    internal lateinit var map: GoogleMap

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_onboarding_mechanic, container, false)
        return view
    }

    override fun onAttach(view: View) {
        AppEvent.trackCurrentScreen(activity!!, SCREEN_NAME)

        showStoryPresenter.init(view)

        map = (activity as MainActivity).map
        map.isMyLocationEnabled = true

        val stories = StoryDb.getAll()
        map.clear()
        addTaggedStoryMarkers(view.context, map, stories)

        locationService = (activity as MainActivity).locationService
        locationService.attach(this)

        view.onboardingText.setText(R.string.onboarding_mechanic_text)
        view.onboardingButton.visibility = View.INVISIBLE
        if (null != nearestStory) {
            showShowStory(nearestStory!!)
        }
        view.showStoryButton.setOnClickListener {
            showStoryPresenter.hideShowStory(view)
            router.pushController(RouterTransaction.with(OnboardingPushController()))
            router.pushController(RouterTransaction.with(StoryController(nearestStory!!)))
        }
    }

    override fun onLocationChanged(last_location: Location?) {
        if (null == last_location) {
            return
        }
        latestLocation = LatLng(last_location.latitude, last_location.longitude)

        val nearest = StoryDb.getNearest(last_location.latitude, last_location.longitude)

        if (null == nearestStory && null != nearest) {
            nearestStory = nearest
            showShowStory(nearest)
        }
    }

    private fun showShowStory(story: Story) {
        showStoryPresenter.showShowStory(view!!, story)

        map.clear()
        val drawable = ContextCompat.getDrawable(activity, R.drawable.marker_story)
        val icon = BitmapDescriptorFactory.fromBitmap(drawableToBitmap(drawable))
        val marker = map.addMarker(MarkerOptions()
                .icon(icon)
                .position(LatLng(story.latitude, story.longitude)))

        val mapPadding = resources!!.getDimensionPixelSize(R.dimen.map_padding)
        val bounds = LatLngBounds.builder().include(marker.position).include(latestLocation).build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, mapPadding))
    }

    override fun onDetach(view: View) {
        locationService.detach(this)
        showStoryPresenter.deinit()
    }
}
