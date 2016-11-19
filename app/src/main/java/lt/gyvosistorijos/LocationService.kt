package lt.gyvosistorijos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.annotation.RequiresPermission
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import java.util.*

internal class LocationService(activity: FragmentActivity) : LocationListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private val googleApiClient: GoogleApiClient

    private val context: Context
    private val listeners: MutableList<LocationListener>

    private var isStartPending = false

    init {
        this.context = activity
        this.googleApiClient = GoogleApiClient.Builder(activity)
                .enableAutoManage(activity, 1, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build()

        this.listeners = ArrayList<LocationListener>()
    }

    override fun onConnected(connectionHint: Bundle?) {
        if (isStartPending) {
            startInternal()
            isStartPending = false
        }
    }

    override fun onConnectionSuspended(cause: Int) {
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
    }

    val lastLocation: Location?
        @RequiresPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        get() = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)

    fun addLocationListener(listener: LocationListener) {
        listeners.add(listener)
    }

    fun removeLocationListener(listener: LocationListener) {
        listeners.remove(listener)
    }

    @RequiresPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    fun start() {
        if (googleApiClient.isConnected) {
            startInternal()
        } else if (googleApiClient.isConnecting) {
            isStartPending = true
        } else {
            throw IllegalStateException(
                    "Connect the GoogleApiClient before starting location updates")
        }
    }

    private fun startInternal() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient,
                LocationRequest()
                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                        .setFastestInterval(1000),
                this)
    }

    fun stop() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
    }

    override fun onLocationChanged(location: Location) {
        for (listener in listeners) {
            listener.onLocationChanged(location)
        }
    }

    fun areLocationPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}
