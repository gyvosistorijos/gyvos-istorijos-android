package lt.gyvosistorijos.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.controller_onboarding_intro.view.*
import lt.gyvosistorijos.MainActivity
import lt.gyvosistorijos.MainController
import lt.gyvosistorijos.R
import lt.gyvosistorijos.storage.OnboardingSharedPrefs
import lt.gyvosistorijos.storage.StoryDb
import lt.gyvosistorijos.utils.AppEvent
import lt.gyvosistorijos.utils.addTaggedStoryMarkers

class OnboardingPushController : Controller() {

    companion object {
        val SCREEN_NAME = "Onboarding push"
    }

    internal lateinit var map: GoogleMap

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_onboarding_push, container, false)
        return view
    }

    override fun onAttach(view: View) {
        AppEvent.trackCurrentScreen(activity!!, SCREEN_NAME)

        map = (activity as MainActivity).map
        map.isMyLocationEnabled = true

        val stories = StoryDb.getAll()
        map.clear()
        addTaggedStoryMarkers(view.context, map, stories)

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(54.6872, 25.2797), 11f))

        view.onboardingButton.setOnClickListener {
            OnboardingSharedPrefs(applicationContext!!).setOnboardingCompleted()
            router.setBackstack(listOf(
                    RouterTransaction.with(MainController(animateZoomIn = true))), null)
        }
    }
}
