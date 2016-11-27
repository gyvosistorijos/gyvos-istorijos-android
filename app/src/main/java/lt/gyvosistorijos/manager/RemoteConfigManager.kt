package lt.gyvosistorijos.manager

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import lt.gyvosistorijos.BuildConfig
import lt.gyvosistorijos.R
import timber.log.Timber


class RemoteConfigManager private constructor() {

    private val config: FirebaseRemoteConfig

    init {
        Timber.i("Initializing remote config")

        config = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
        config.setConfigSettings(configSettings)
        config.setDefaults(R.xml.remote_config_defaults)
    }

    fun fetchConfig() {
        Timber.d("Started fetching remote config")

        config.fetch().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.d("Remote config fetch successful")
                config.activateFetched()
            } else {
                Timber.e("Remote config fetch failed")
            }
        }
    }

    fun getGeofenceRadiusInMeters(): Float {
        return config.getDouble(GEOFENCE_RADIUS_IN_METERS).toFloat()
    }

    fun getStoryRadiusInMeters(): Float {
        return config.getDouble(STORY_RADIUS_IN_METERS).toFloat()
    }

    private object Holder {
        val INSTANCE = RemoteConfigManager()
    }

    companion object {

        private val GEOFENCE_RADIUS_IN_METERS = "geofence_radius_m"
        private val STORY_RADIUS_IN_METERS = "story_radius_m"

        val instance: RemoteConfigManager by lazy { Holder.INSTANCE }
    }
}
