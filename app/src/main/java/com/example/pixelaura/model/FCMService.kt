package com.example.pixelaura.model

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pixelaura.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

//was gonna work on notiffs outside of app but its too complicated now i dont rlly want to delete this (coz my app is done) but its prolly not used

class FCMService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "PixelAura"
        val body = remoteMessage.notification?.body ?: ""

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pixel_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "PixelAura Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "PixelAura post alerts"
                enableLights(true)
                lightColor = Color.RED
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notiff)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }
}
