package lt.gyvosistorijos.utils

import com.bluelinelabs.conductor.Controller
import com.google.firebase.analytics.FirebaseAnalytics


object AppEvent {

    fun trackCurrentController(controller: Controller) {
        val controllerName = controller.javaClass.simpleName

        controller.activity ?: AppLog.e("Activity from controller $controllerName is null")

        controller.activity?.let {
            activity ->
            FirebaseAnalytics.getInstance(activity).setCurrentScreen(activity, controllerName, null)
        }
    }

}