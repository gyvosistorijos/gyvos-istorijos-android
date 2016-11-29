package lt.gyvosistorijos.utils

import com.google.android.gms.location.LocationListener
import lt.gyvosistorijos.location.LocationService

fun LocationService.attach(listener: LocationListener) {
    listener.onLocationChanged(lastLocation)
    addLocationListener(listener)
    start()
}

fun LocationService.detach(listener: LocationListener) {
    removeLocationListener(listener)
    stop()
}
