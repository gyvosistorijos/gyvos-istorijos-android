package lt.gyvosistorijos.location

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import lt.gyvosistorijos.manager.RemoteConfigManager
import lt.gyvosistorijos.service.GeofenceIntentService
import timber.log.Timber
import java.util.*


class GeofenceHelper(private val fragmentActivity: FragmentActivity,
                     private val remoteConfigManager: RemoteConfigManager) :
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private val googleApiClient: GoogleApiClient

    private var geofencePendingIntent: PendingIntent? = null
    private val geofenceList = ArrayList<Geofence>()

    /**
     * If true, this helper has pending geofences that have not yet been submitted to the geofence
     * service.
     */
    private var isGeofenceReplacePending = false


    init {
        googleApiClient = GoogleApiClient.Builder(fragmentActivity)
                .enableAutoManage(fragmentActivity, 0, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build()
    }

    override fun onConnected(bundle: Bundle?) {
        Timber.i("Connected to GoogleApiClient");

        if (isGeofenceReplacePending) {
            replaceGeofences()
            isGeofenceReplacePending = false
        }
    }

    override fun onConnectionSuspended(i: Int) {
        // The connection to Google Play services was lost for some reason.
        Timber.i("Connection suspended");

        // onConnected() will be called again automatically when the service reconnects
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Timber.w("GoogleApiClient connection failed " + connectionResult)
    }

    fun setGeofenceRegions(regions: List<GeofenceRegion>) {
        buildGeofenceList(regions)
        scheduleReplaceGeofences()
    }

    private fun buildGeofenceList(regions: List<GeofenceRegion>) {
        geofenceList.clear()
        if (regions.isEmpty()) {
            Timber.d("Clearing all regions")
        } else {
            val geofenceRadiusInMeters = remoteConfigManager.getGeofenceRadiusInMeters()

            for ((id, latitude, longitude) in regions) {
                Timber.d("Adding region $id latitude $latitude " +
                        "longitude $longitude radius ${geofenceRadiusInMeters}m ")

                geofenceList.add(Geofence.Builder()
                        .setRequestId(id)
                        .setCircularRegion(
                                latitude,
                                longitude,
                                geofenceRadiusInMeters
                        )
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER.or(Geofence.GEOFENCE_TRANSITION_EXIT))
                        .build())
            }
        }
    }

    /**
     * If currently connected to [GoogleApiClient], replace is executed immediately.
     */
    private fun scheduleReplaceGeofences() {
        if (googleApiClient.isConnected) {
            replaceGeofences()
        } else {
            isGeofenceReplacePending = true
        }
    }

    /**
     * Removes any existing geofences and adds all in [.geofenceList].
     */
    private fun replaceGeofences() {
        Timber.i("Replacing geofences")

        LocationServices.GeofencingApi.removeGeofences(
                googleApiClient,
                getGeofencePendingIntent()
        ).setResultCallback(ResultCallback<Status> { status ->
            Timber.d("Remove geofences result: " + status)
            if (status.isSuccess) {
                if (geofenceList.isEmpty()) {
                    // If list is empty, there aren't any geofences to add, so quit early
                    return@ResultCallback
                }

                if (!googleApiClient.isConnected) {
                    onReplaceGeofencesFailure(Exception(
                            "GoogleApiClient disconnected after old geofences were removed " +
                                    "but before new geofences could be added."))
                    return@ResultCallback
                }

                // Note - only add new geofences if previous ones have been successfully removed
                LocationServices.GeofencingApi.addGeofences(
                        googleApiClient,
                        buildGeofencingRequest(),
                        getGeofencePendingIntent()
                ).setResultCallback { status ->
                    Timber.d("Add geofences result: " + status)
                    if (!status.isSuccess) {
                        onReplaceGeofencesFailure(getFailure(status))
                    }
                }
            } else {
                onReplaceGeofencesFailure(getFailure(status))
            }
        })
    }

    private fun getFailure(status: Status): Exception {
        return Exception("" + status.statusCode + " " + status.statusMessage)
    }

    private fun onReplaceGeofencesFailure(e: Exception) {
        Timber.e(e)
        isGeofenceReplacePending = true
    }

    private fun buildGeofencingRequest(): GeofencingRequest {
        val builder = GeofencingRequest.Builder()
        builder.setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER
                        .or(GeofencingRequest.INITIAL_TRIGGER_EXIT)
        )
        builder.addGeofences(geofenceList)
        return builder.build()
    }

    private fun getGeofencePendingIntent(): PendingIntent? {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent
        }

        val intent = Intent(fragmentActivity, GeofenceIntentService::class.java)
        // Use FLAG_UPDATE_CURRENT so same PendingIntent is returned for adding & removing geofences
        geofencePendingIntent = PendingIntent
                .getService(fragmentActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return geofencePendingIntent
    }
}

