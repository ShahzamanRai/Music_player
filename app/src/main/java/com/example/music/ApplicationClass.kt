package com.example.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ApplicationClass : Application() {
    companion object {
        const val CHANNEL_ID = "musicChannel"
        const val NEXT = "next"
        const val PREVIOUS = "previous"
        const val PLAY = "play"
        const val EXIT = "exit"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(CHANNEL_ID, "Now playing", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "Channel for showing playing song"
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }


    }

}