package lt.gyvosistorijos

import android.Manifest
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import kotlinx.android.synthetic.main.controller_permissions.view.*
import lt.gyvosistorijos.utils.AppEvent

class PermissionsController : Controller() {

    companion object {
        val REQUEST_PERMISSIONS_LOCATION = 0
        val SCREEN_NAME = "Permissions"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_permissions, container, false)
        return view
    }

    override fun onAttach(view: View) {
        AppEvent.trackCurrentScreen(activity!!, SCREEN_NAME)

        val locationService = (activity as MainActivity).locationService

        // Check if user has granted location permission
        if (!locationService.areLocationPermissionsGranted()) {
            requestLocation()
        } else {
            router.replaceTopController(RouterTransaction.with(MainController()))
        }

        view.permissionsRequestButton.setOnClickListener { requestLocation() }
    }

    private fun requestLocation() {
        requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_LOCATION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSIONS_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    router.replaceTopController(RouterTransaction.with(MainController()))
                }
            }
        }
    }
}
