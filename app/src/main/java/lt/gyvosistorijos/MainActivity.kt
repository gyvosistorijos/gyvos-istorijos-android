package lt.gyvosistorijos

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.gms.maps.GoogleMap
import kotlinx.android.synthetic.main.activity_main.*
import lt.gyvosistorijos.location.GeofenceHelper
import lt.gyvosistorijos.manager.RemoteConfigManager

class MainActivity : AppCompatActivity() {

    internal lateinit var router: Router


    internal lateinit var geofenceHelper: GeofenceHelper
    internal lateinit var map: GoogleMap

    internal lateinit var locationService: LocationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        router = Conductor.attachRouter(this, controller_container, savedInstanceState)
        locationService = LocationService(this)

        mapView.onCreate(savedInstanceState)

        RemoteConfigManager.instance.fetchConfig()

        geofenceHelper = GeofenceHelper(this, RemoteConfigManager.instance)

        mapView.getMapAsync { googleMap ->
            map = googleMap

            map.uiSettings.isMyLocationButtonEnabled = false

            if (!router.hasRootController()) {
                router.setRoot(RouterTransaction.with(SyncController()))
            }
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        locationService.start()
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
        locationService.stop()
        mapView.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
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
