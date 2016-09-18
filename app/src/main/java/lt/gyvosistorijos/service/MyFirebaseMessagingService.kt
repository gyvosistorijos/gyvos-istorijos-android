package lt.gyvosistorijos.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import lt.gyvosistorijos.MainActivity
import lt.gyvosistorijos.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    val NOTIFICATION_ID = 0

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        val notification = remoteMessage!!.notification

        if (notification != null) {
            val notificationIntent = Intent(this, MainActivity::class.java)


            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(this).
                    setContentTitle(notification.title)
                    .setContentText(notification.body).setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }
}