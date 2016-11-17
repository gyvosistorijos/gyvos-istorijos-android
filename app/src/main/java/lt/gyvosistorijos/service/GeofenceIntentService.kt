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
import com.google.android.gms.location.GeofencingEvent
import lt.gyvosistorijos.MainActivity
import lt.gyvosistorijos.R
import lt.gyvosistorijos.StoryDb
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.location.GeofenceErrorMessages
import timber.log.Timber


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
            Timber.e(errorMessage)

            return
        }

        val triggeredLocation = geofencingEvent.triggeringLocation
        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition
        // Get the geofences that were triggered. A single event can trigger multiple geofences.
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        val triggeringGeofencesIdsList = triggeringGeofences.map { it.requestId }

        // Get the transition details as a String.
        val geofenceTransitionDetails = getGeofenceTransitionDetails(
                geofenceTransition,
                triggeredLocation,
                triggeringGeofencesIdsList
        )

        Timber.i(geofenceTransitionDetails)

        val triggeredStory = getTriggeredStory(triggeringGeofencesIdsList)

        sendNotification(triggeredStory)

    }

    private fun getTriggeredStory(storyIds: List<String>): Story {
        return StoryDb.getByIds(storyIds).first()
    }

    private fun getGeofenceTransitionDetails(
            transition: Int,
            triggeredLocation: Location?,
            triggeringGeofencesIdsList: List<String>): String {

        val triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)

        return "Geofence triggered at $triggeredLocation with transition $transition: $triggeringGeofencesIdsString"
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private fun sendNotification(story: Story) {
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

        val builder = NotificationCompat.Builder(this)

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.geofence_notification_text))
                .setContentText(story.text)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setContentIntent(notificationPendingIntent).priority = NotificationCompat.PRIORITY_LOW

        val notification = builder.build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notification)
    }


    companion object {
        private val TAG = "GeofenceTransitionsIS"
    }
}