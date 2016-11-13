package lt.gyvosistorijos.utils

import android.app.Activity
import com.google.firebase.analytics.FirebaseAnalytics

object AppEvent {

    fun tackCurrentScreen(activity: Activity, name: String) {
        FirebaseAnalytics.getInstance(activity).setCurrentScreen(activity, name, null)
    }

}