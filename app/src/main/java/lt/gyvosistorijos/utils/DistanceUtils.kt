package lt.gyvosistorijos.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng

fun LatLng.distanceMetersTo(other: LatLng): Float {
    return distanceMeters(latitude, longitude, other.latitude, other.longitude)
}

fun LatLng.distanceMetersTo(otherLatitude: Double, otherLongitude: Double): Float {
    return distanceMeters(latitude, longitude, otherLatitude, otherLongitude)
}

fun distanceMeters(lat: Double, lng: Double, lat2: Double, lng2: Double): Float {
    val distance = FloatArray(1)
    Location.distanceBetween(lat, lng, lat2, lng2, distance)
    return distance[0]
}
