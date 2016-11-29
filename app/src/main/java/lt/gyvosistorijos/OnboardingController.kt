package lt.gyvosistorijos

import android.location.Location
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.gms.location.LocationListener
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.controller_onboarding.view.*
import kotlinx.android.synthetic.main.layout_show_story.view.*
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.location.LocationService
import lt.gyvosistorijos.utils.AppEvent
import lt.gyvosistorijos.utils.drawableToBitmap

class OnboardingController : Controller(), LocationListener {

    enum class OnboardingStep {
        INTRO,
        MECHANIC,
        PUSH,
    }

    companion object {
        val SCREEN_NAME = "Onboarding"
    }

    private val showStoryPresenter: ShowStoryPresenter = ShowStoryPresenter()
    private var step: OnboardingStep? = OnboardingStep.INTRO
    private var nearestStory: Story? = null

    internal lateinit var locationService: LocationService
    internal lateinit var map: GoogleMap

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_onboarding, container, false)
        return view
    }

    override fun onAttach(view: View) {
        AppEvent.trackCurrentScreen(activity!!, SCREEN_NAME)

        showStoryPresenter.init(view)

        map = (activity as MainActivity).map
        map.isMyLocationEnabled = true

        locationService = (activity as MainActivity).locationService
        onLocationChanged(locationService.lastLocation)
        locationService.addLocationListener(this)
        locationService.start()

        setup(view)
    }

    override fun onLocationChanged(last_location: Location?) {
        if (null == last_location) {
            return
        }

        val nearest = StoryDb.getNearest(last_location.latitude, last_location.longitude)

        if (null != nearest) {
            nearestStory = nearest

            if (step == OnboardingStep.MECHANIC) {
                showShowStory(nearest)
            }
        }
    }

    private fun showShowStory(story: Story) {
        showStoryPresenter.showShowStory(view!!, story)

        map.clear()
        val drawable = ContextCompat.getDrawable(activity, R.drawable.dot)
        val icon = BitmapDescriptorFactory.fromBitmap(drawableToBitmap(drawable))
        map.addMarker(MarkerOptions()
                .icon(icon)
                .position(LatLng(story.latitude, story.longitude)))
    }

    override fun onDetach(view: View) {
        locationService.removeLocationListener(this)
        locationService.stop()
        showStoryPresenter.deinit()
    }

    fun setup(view: View) {
        when (step) {
            OnboardingStep.INTRO -> {
                view.onboardingText.setText(R.string.onboarding_intro_text)
                view.onboardingButton.setText(R.string.onboarding_intro_cta)
                view.onboardingButton.visibility = View.VISIBLE
                view.onboardingButton.setOnClickListener {
                    step = OnboardingStep.MECHANIC
                    setup(view)
                }
            }
            OnboardingStep.MECHANIC -> {
                view.onboardingText.setText(R.string.onboarding_mechanic_text)
                view.onboardingButton.visibility = View.INVISIBLE
                if (null != nearestStory) {
                    showShowStory(nearestStory!!)
                }
                view.showStoryButton.setOnClickListener {
                    step = OnboardingStep.PUSH
                    showStoryPresenter.hideShowStory(view)
                    router.pushController(RouterTransaction.with(StoryController(nearestStory!!)))
                }
            }
            OnboardingStep.PUSH -> {
                view.onboardingText.setText(R.string.onboarding_push_text)
                view.onboardingButton.setText(R.string.onboarding_push_cta)
                view.onboardingButton.visibility = View.VISIBLE
                view.onboardingButton.setOnClickListener {
                    router.replaceTopController(RouterTransaction.with(MainController()))
                }
            }
        }
    }
}
