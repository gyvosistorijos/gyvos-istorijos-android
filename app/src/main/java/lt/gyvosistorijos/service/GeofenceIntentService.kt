package lt.gyvosistorijos.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Handler
import android.support.v7.app.NotificationCompat
import android.text.TextUtils
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import lt.gyvosistorijos.MainActivity
import lt.gyvosistorijos.R
import lt.gyvosistorijos.StoryDb
import lt.gyvosistorijos.entity.Story
import lt.gyvosistorijos.location.GeofenceErrorMessages
import timber.log.Timber
import java.util.*


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
        val geofenceTransition = geofencingEvent.geofenceTransition
        // Get the geofences that were triggered. A single event can trigger multiple geofences.
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        val triggeringGeofencesIdsList = triggeringGeofences.map { it.requestId }

        logGeofenceTransitionDetails(
                geofenceTransition,
                triggeredLocation,
                triggeringGeofencesIdsList
        )

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Collections.shuffle(triggeringGeofencesIdsList)

                StoryDb.getById(triggeringGeofencesIdsList.first())?.let { story ->
                    sendNotification(story)
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT ->
                triggeringGeofencesIdsList.forEach { storyId ->
                    cancelNotification(getNotificationId(storyId))
                }
        }
    }

    private fun logGeofenceTransitionDetails(
            transition: Int,
            triggeredLocation: Location?,
            triggeringGeofencesIdsList: List<String>) {

        val triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)

        Timber.i("Geofence triggered at $triggeredLocation with transition $transition: $triggeringGeofencesIdsString")
    }

    // Picasso holds Target instance with weak reference.
    // So it is better to hold Target as instance field.
    // see: http://stackoverflow.com/a/29274669/5183999
    private var notificationTarget: Target? = null

    private fun sendNotification(story: Story) {
        val notificationIntent = Intent(applicationContext, MainActivity::class.java)

        val stackBuilder = TaskStackBuilder.create(this)

        stackBuilder.addParentStack(MainActivity::class.java)

        stackBuilder.addNextIntent(notificationIntent)

        val notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this)

        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.geofence_notification_text))
                .setContentText(story.text)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setContentIntent(notificationPendingIntent).priority = NotificationCompat.PRIORITY_LOW


        notificationTarget = object : Target {
            override fun onBitmapFailed(errorDrawable: Drawable?) {
                showStoryNotification(story, builder.build())
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)

                    showStoryNotification(story, builder.build())
                } else {
                    Timber.d("Notification loaded bitmap is null")
                }
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }

        }

        Handler(mainLooper).post({
            Picasso.with(this)
                    .load(story.url)
                    .into(notificationTarget)
        })
    }

    private fun showStoryNotification(story: Story, notification: Notification) {
        val notificationId = getNotificationId(story.id)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        notification.defaults = notification.defaults.or(Notification.DEFAULT_VIBRATE)

        notificationManager.notify(notificationId, notification)
    }

    private fun getNotificationId(storyId: String): Int {
        return Math.abs(storyId.hashCode())
    }

    private fun cancelNotification(notificationId: Int) {

        Timber.i("Canceling notification with notification id = $notificationId")

        (getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager).cancel(notificationId)
    }


    companion object {
        private val TAG = "GeofenceTransitionsIS"
    }
}