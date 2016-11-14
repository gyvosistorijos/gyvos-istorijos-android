package lt.gyvosistorijos.utils

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AppEvent {

    fun trackCurrentScreen(activity: Activity, name: String) {
        FirebaseAnalytics.getInstance(activity).setCurrentScreen(activity, name, null)
    }

    fun trackStoryClicked(context: Context, id: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id)
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "story")

        logEvent(context, FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    private fun logEvent(context: Context, name: String, bundle: Bundle?) {
        FirebaseAnalytics.getInstance(context).logEvent(name, bundle)
    }

}