package lt.gyvosistorijos

import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.android.synthetic.main.activity_main.*
import lt.gyvosistorijos.location.GeofenceHelper
import lt.gyvosistorijos.manager.RemoteConfigManager
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    companion object {
        val MAP_SAVED_STATE_KEY = "map"
    }

    internal var router: Router? = null

    internal lateinit var map: GoogleMap

    internal lateinit var geofenceHelper: GeofenceHelper
    internal lateinit var locationService: LocationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RemoteConfigManager.instance.fetchConfig()
        geofenceHelper = GeofenceHelper(this, RemoteConfigManager.instance)
        locationService = LocationService(this)

        mapView.onCreate(savedInstanceState?.getBundle(MAP_SAVED_STATE_KEY))

        mapView.getMapAsync { googleMap ->
            map = googleMap

            map.uiSettings.isMyLocationButtonEnabled = false

            initMapStyle()

            val router = Conductor.attachRouter(this, controller_container, savedInstanceState)
            if (!router.hasRootController()) {
                router.setRoot(RouterTransaction.with(SyncController()))
            }
            this.router = router
        }
    }

    private fun initMapStyle() {
        try {
            val success = map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
            if (!success) {
                Timber.e("Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Timber.e("MapsActivityRaw", "Can't find style.", e)
        }
    }

    override fun onBackPressed() {
        val handled = router?.handleBack() ?: false
        if (!handled) {
            super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val mapState = Bundle()
        mapView.onSaveInstanceState(mapState)
        outState.putBundle(MAP_SAVED_STATE_KEY, mapState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}
