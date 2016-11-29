package lt.gyvosistorijos.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.gms.maps.GoogleMap
import kotlinx.android.synthetic.main.controller_onboarding_intro.view.*
import lt.gyvosistorijos.MainActivity
import lt.gyvosistorijos.MainController
import lt.gyvosistorijos.R
import lt.gyvosistorijos.storage.OnboardingSharedPrefs
import lt.gyvosistorijos.storage.StoryDb
import lt.gyvosistorijos.utils.AppEvent
import lt.gyvosistorijos.utils.addTaggedStoryMarkers

class OnboardingIntroController : Controller() {

    companion object {
        val SCREEN_NAME = "Onboarding intro"
    }

    internal lateinit var map: GoogleMap

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_onboarding_intro, container, false)
        return view
    }

    override fun onAttach(view: View) {

        // skip onboarding if user has already completed it
        val sharedPrefs = OnboardingSharedPrefs(applicationContext!!)
        if (sharedPrefs.onboardingCompleted()) {
            router.replaceTopController(RouterTransaction.with(MainController()))
            return
        }

        AppEvent.trackCurrentScreen(activity!!, SCREEN_NAME)

        map = (activity as MainActivity).map
        map.isMyLocationEnabled = true

        val stories = StoryDb.getAll()
        map.clear()
        addTaggedStoryMarkers(view.context, map, stories)

        view.onboardingButton.setOnClickListener {
            router.pushController(RouterTransaction.with(OnboardingMechanicController()))
        }
    }
}
