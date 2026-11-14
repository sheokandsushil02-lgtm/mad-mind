package com.sushil.madassistant.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sushil.madassistant.MainActivity
import com.sushil.madassistant.R
import com.sushil.madassistant.service.MadVoiceService

object ReminderChannels {
    const val CHANNEL = "mad_reminders"
    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Reminders",
                NotificationManager.IMPORTANCE_HIGH))
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "रिमाइंडर"
        val taskId = intent.getLongExtra("taskId", 0L)
        ReminderChannels.ensure(context)

        val openApp = PendingIntent.getActivity(context, taskId.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(context, ReminderChannels.CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("MAD रिमाइंडर")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(taskId.toInt(), notif)

        val speakIntent = Intent(context, MadVoiceService::class.java)
        speakIntent.action = MadVoiceService.ACTION_SPEAK
        speakIntent.putExtra("text", "जादूगर, याद दिला दूँ: " + title)
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(speakIntent)
            else context.startService(speakIntent)
        } catch (_: Exception) {}
    }
}
