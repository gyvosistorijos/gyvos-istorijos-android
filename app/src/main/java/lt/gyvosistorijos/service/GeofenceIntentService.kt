package lt.gyvosistorijos.service

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.location.Location
import android.support.v7.app.NotificationCompat
import android.text.TextUtils
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import lt.gyvosistorijos.MainActivity
import lt.gyvosistorijos.R
import lt.gyvosistorijos.location.GeofenceErrorMessages
import lt.gyvosistorijos.utils.AppLog


/**
 * Listener for geofence transition changes.

 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
/**
 * This constructor is required, and calls the super IntentService(String)
 * constructor with the name for a worker thread.
 */
class GeofenceIntentService : IntentService(GeofenceIntentService.TAG) {

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     * *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.errorCode)
            AppLog.e(errorMessage)

            return
        }

        val triggeredLocation = geofencingEvent.triggeringLocation
        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition
        // Get the geofences that were triggered. A single event can trigger multiple geofences.
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        // Get the transition details as a String.
        val geofenceTransitionDetails = getGeofenceTransitionDetails(
                geofenceTransition,
                triggeredLocation,
                triggeringGeofences
        )

        // Send notification and log the transition details.
        sendNotification()
        AppLog.i(geofenceTransitionDetails)
    }

    private fun getGeofenceTransitionDetails(
            transition: Int,
            triggeredLocation: Location?,
            triggeringGeofences: List<Geofence>): String {

        // Get the Ids of each geofence that was triggered.
        val triggeringGeofencesIdsList = triggeringGeofences.map { it.requestId }
        val triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)

        return "Geofence triggered at $triggeredLocation with transition $transition: $triggeringGeofencesIdsString"
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private fun sendNotification() {
        // Create an explicit content Intent that starts the main Activity.
        val notificationIntent = Intent(applicationContext, MainActivity::class.java)

        // Construct a task stack.
        val stackBuilder = TaskStackBuilder.create(this)

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity::class.java)

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent)

        // Get a PendingIntent containing the entire back stack.
        val notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        // Get a notification builder that's compatible with platform versions >= 4
        val builder = NotificationCompat.Builder(this)

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.geofence_notification_text))
                .setContentIntent(notificationPendingIntent).priority = NotificationCompat.PRIORITY_MIN

        // Get an instance of the Notification manager
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Issue the notification
        mNotificationManager.notify(0, builder.build())
    }


    companion object {
        private val TAG = "GeofenceTransitionsIS"
    }
}