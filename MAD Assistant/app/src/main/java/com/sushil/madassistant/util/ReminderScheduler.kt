package com.sushil.madassistant.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sushil.madassistant.receiver.AlarmReceiver

object ReminderScheduler {

    fun schedule(context: Context, taskId: Long, title: String, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, taskId, title)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    fun cancel(context: Context, taskId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, taskId, ""))
    }

    private fun pendingIntent(context: Context, taskId: Long, title: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction("com.sushil.madassistant.action.REMINDER")
            .putExtra("taskId", taskId)
            .putExtra("title", title)
        return PendingIntent.getBroadcast(context, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
