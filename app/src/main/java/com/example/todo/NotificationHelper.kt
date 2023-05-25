package com.example.todo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {

    fun createNotificationChannel() {
        val channelId = "todo_channel"
        val channelName = "TODO Channel"
        val channelDescription = "Channel for TODO notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val channel = NotificationChannel(channelId, channelName, importance)
        channel.description = channelDescription

        val notificationManager =
            context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "TodoChannel"

        fun showNotification(context: Context, title: String, message: String) {
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_add) //TODO: ic_notification
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)

            val notificationManager =
                ContextCompat.getSystemService(
                    context,
                    NotificationManager::class.java
                ) as NotificationManager

            notificationManager.notify(0, notificationBuilder.build())
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")
        val message = intent.getStringExtra("message")

        if (title != null && message != null) {
            NotificationHelper.showNotification(context, title, message)
        }
    }
}
