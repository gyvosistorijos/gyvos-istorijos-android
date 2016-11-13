package lt.gyvosistorijos

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.mapbox.mapboxsdk.maps.MapboxMap
import kotlinx.android.synthetic.main.activity_main.*
import lt.gyvosistorijos.location.GeofenceHelper
import lt.gyvosistorijos.manager.RemoteConfigManager

class MainActivity : AppCompatActivity() {

    internal lateinit var router: Router

    internal lateinit var map: MapboxMap

    internal lateinit var geofenceHelper: GeofenceHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        router = Conductor.attachRouter(this, controller_container, savedInstanceState)

        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            map.uiSettings.isTiltGesturesEnabled = false
            map.uiSettings.isRotateGesturesEnabled = false

            if (!router.hasRootController()) {
                router.setRoot(RouterTransaction.with(SyncController()))
            }
        }

        RemoteConfigManager.instance.fetchConfig()

        geofenceHelper = GeofenceHelper(this, RemoteConfigManager.instance)
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        geofenceHelper.connect()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        geofenceHelper.disconnect()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
