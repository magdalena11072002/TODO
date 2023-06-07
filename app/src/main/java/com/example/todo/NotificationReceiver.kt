package com.example.todo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        val notificationTitle = intent.getStringExtra("title")
        val notificationMessage = intent.getStringExtra("message")

        showNotification(context, notificationTitle, notificationMessage)
    }
    // Wyświetlanie powiadomienia
    private fun showNotification(context: Context, title: String?, message: String?) {
        if (title != null && message != null) {
            NotificationUtils.showNotification(context, title, message)
        }
    }
}

object NotificationUtils {
    private var notificationID = 0
    private const val CHANNEL_ID = "my_channel_id"
    private const val CHANNEL_NAME = "My Channel"
    // Metoda wyświetlająca powiadomienie
    fun showNotification(context: Context, title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Tworzenie kanału powiadomień
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        // Tworzenie intencji dla aktywności, która ma zostać uruchomiona po kliknięciu w powiadomienie
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Tworzenie obiektu powiadomienia
        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        // Wyświetlanie powiadomienia
        notificationManager.notify(notificationID++, notification)
    }
}

