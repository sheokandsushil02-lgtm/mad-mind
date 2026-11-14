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
        const val ACTION_SLEEP = "com.sushil.madassistant.action.SLEEP"
        const val ACTION_WAKE = "com.sushil.madassistant.action.WAKE"

        // Global state so MainActivity / Dashboard can reflect it immediately.
        @Volatile var isSleeping: Boolean = false
            private set

        fun start(context: Context) {
            val i = Intent(context, MadVoiceService::class.java)
            if (foregroundPermitted(context)) context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) = context.stopService(Intent(context, MadVoiceService::class.java))

        fun sleep(context: Context) {
            isSleeping = true
            val i = Intent(context, MadVoiceService::class.java).setAction(ACTION_SLEEP)
            try { context.startService(i) } catch (_: Exception) {}
        }

        fun wake(context: Context) {
            isSleeping = false
            val i = Intent(context, MadVoiceService::class.java).setAction(ACTION_WAKE)
            try { context.startService(i) } catch (_: Exception) {}
        }

        private fun foregroundPermitted(context: Context): Boolean = Build.VERSION.SDK_INT >= 26
    }

    private var voiceEngine: VoiceEngine? = null
    private var wakeWordLoop = false

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
            ACTION_STOP -> { stopListeningLoop(); stopSelf(); return START_NOT_STICKY }
            ACTION_SLEEP -> { stopListeningLoop(); }
            ACTION_WAKE -> { if (!wakeWordLoop) startWakeWordLoop() }
            ACTION_TALK -> {
                if (!isSleeping) startListeningSession()
            }
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

    /**
     * Wake-word loop: continuously listen; when the phrase "mad" appears in
     * a recognition result, hand the rest (or the next utterance) to MAD.
     */
    private fun startWakeWordLoop() {
        ensureVoiceEngine()
        if (voiceEngine?.isRecognitionAvailable() != true) return
        wakeWordLoop = true
        listenOnce()
    }

    private fun listenOnce() {
        if (!wakeWordLoop || isSleeping) { wakeWordLoop = false; return }
        voiceEngine?.startListening { text ->
            if (!wakeWordLoop) return@startListening
            val heard = text.lowercase().trim()
            if (heard.contains("mad") && heard.isNotBlank()) {
                // Wake word detected: pass command to MAD brain
                val command = heard.removePrefix("mad").trim()
                    .removePrefix("hey mad").trim()
                    .removePrefix("मैड").trim()
                if (command.isNotBlank()) {
                    MadConversationBridge.onVoiceInput(this, command)
                } else {
                    // Just "MAD" — greet and listen
                    speak("जी जादूगर, बोलिए।")
                    listenOnce()
                }
            } else {
                // No wake word: keep listening
                listenOnce()
            }
        }
    }

    private fun speak(text: String) {
        ensureVoiceEngine()
        voiceEngine?.speak(text)
    }

    private fun stopListeningLoop() {
        wakeWordLoop = false
        voiceEngine?.stopListening()
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
        val sleepPi = PendingIntent.getService(this, 3,
            Intent(this, MadVoiceService::class.java).setAction(ACTION_SLEEP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val wakePi = PendingIntent.getService(this, 4,
            Intent(this, MadVoiceService::class.java).setAction(ACTION_WAKE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(if (isSleeping) "MAD सो रही है" else "MAD जाग रही है")
            .setContentText(if (isSleeping) "जगाने के लिए दबाइए" else "बोलिए दबाकर बात कीजिए")
            .setContentIntent(openApp)
            .addAction(0, "🎙 बोलिए", talkPi)
            .addAction(0, if (isSleeping) "☀️ जगाओ" else "😴 सुलाओ", if (isSleeping) wakePi else sleepPi)
            .addAction(0, "⏹ बंद", stopPi)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        wakeWordLoop = false
        voiceEngine?.destroy()
        voiceEngine = null
        super.onDestroy()
    }
}
