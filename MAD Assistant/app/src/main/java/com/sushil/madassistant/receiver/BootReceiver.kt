package com.sushil.madassistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sushil.madassistant.data.db.MadDatabase
import com.sushil.madassistant.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MadDatabase.getDatabase(context)
                db.taskDao().getPendingTasksOnce().forEach { task ->
                    if (task.reminderAtMillis > System.currentTimeMillis()) {
                        ReminderScheduler.schedule(context, task.id, task.title, task.reminderAtMillis)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
