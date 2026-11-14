package com.sushil.madassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sushil.madassistant.MainActivity
import com.sushil.madassistant.R
import com.sushil.madassistant.voice.VoiceEngine

class MadVoiceService : Service() {

    companion object {
        const val CHANNEL_ID = "mad_voice"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.sushil.madassistant.action.STOP"
        const val ACTION_TALK = "com.sushil.madassistant.action.TALK"
        const val ACTION_SPEAK = "com.sushil.madassistant.action.SPEAK"

        fun start(context: Context) {
            val i = Intent(context, MadVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) = context.stopService(Intent(context, MadVoiceService::class.java))
    }

    private var voiceEngine: VoiceEngine? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, "MAD Voice",
                NotificationManager.IMPORTANCE_LOW)
            ch.description = "Background voice assistant"
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }

        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_TALK -> startListeningSession()
            ACTION_SPEAK -> {
                val text = intent.getStringExtra("text") ?: ""
                if (text.isNotBlank()) {
                    ensureVoiceEngine()
                    voiceEngine?.speak(text)
                }
            }
        }
        return START_STICKY
    }

    private fun ensureVoiceEngine() {
        if (voiceEngine == null) {
            voiceEngine = VoiceEngine(this)
            voiceEngine?.initTts()
        }
    }

    private fun startListeningSession() {
        ensureVoiceEngine()
        voiceEngine?.startListening { text ->
            if (text.isNotBlank()) {
                MadConversationBridge.onVoiceInput(this, text)
            }
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val talkPi = PendingIntent.getService(this, 1,
            Intent(this, MadVoiceService::class.java).setAction(ACTION_TALK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopPi = PendingIntent.getService(this, 2,
            Intent(this, MadVoiceService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("MAD जाग रही है")
            .setContentText("बोलिए दबाकर बात कीजिए")
            .setContentIntent(openApp)
            .addAction(0, "🎙 बोलिए", talkPi)
            .addAction(0, "⏹ बंद", stopPi)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        voiceEngine?.destroy()
        voiceEngine = null
        super.onDestroy()
    }
}
