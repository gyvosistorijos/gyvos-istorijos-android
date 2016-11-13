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

        config.fetch(FETCH_CACHE_EXPIRE_TIME).addOnCompleteListener { task ->
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

    fun getGeofenceLoiteringDelayInMilliseconds(): Int {
        return (config.getLong(GEOFENCE_LOITERING_DELAY_IN_SECONDS) * 1000).toInt()
    }

    private object Holder {
        val INSTANCE = RemoteConfigManager()
    }

    companion object {

        private val FETCH_CACHE_EXPIRE_TIME = 60L * 60 * 1000 //1h

        private val GEOFENCE_RADIUS_IN_METERS = "geofence_radius_in_meters"
        private val GEOFENCE_LOITERING_DELAY_IN_SECONDS = "geofence_loitering_delay_in_seconds"

        val instance: RemoteConfigManager by lazy { Holder.INSTANCE }
    }
}
