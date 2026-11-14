package com.sushil.madassistant.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.sushil.madassistant.MainActivity
import com.sushil.madassistant.R

class FloatingButtonService : Service() {

    companion object {
        const val CHANNEL_ID = "mad_floating"
        const val NOTIFICATION_ID = 1002

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context,
                    "पहले \"Display over other apps\" permission दीजिए।",
                    Toast.LENGTH_LONG).show()
                try {
                    context.startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {}
                return
            }
            val i = Intent(context, FloatingButtonService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) = context.stopService(Intent(context, FloatingButtonService::class.java))
    }

    private var wm: WindowManager? = null
    private var orb: View? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, "MAD Floating Button",
                NotificationManager.IMPORTANCE_MIN)
            ch.description = "Keeps the floating MAD button alive"
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground notification is mandatory for startForegroundService on API 26+
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("MAD बटन चालू")
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notif)

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (orb == null) showOrb()
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOrb() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val iv = ImageView(this)
        iv.setImageResource(R.drawable.ic_launcher)
        iv.alpha = 0.9f

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        params.x = 24
        params.y = 0

        var downY = 0f
        var moved = false
        iv.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = event.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(event.rawY - downY) > 10) moved = true
                    params.y = (event.rawY - 400).toInt()
                    try { wm?.updateViewLayout(iv, params) } catch (_: Exception) {}
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        val i = Intent(this, MainActivity::class.java)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { startActivity(i) } catch (_: Exception) {}
                    }
                }
            }
            true
        }
        try {
            wm?.addView(iv, params)
            orb = iv
        } catch (e: Exception) {
            // Overlay rejected (permission revoked while running): tell the user why.
            Toast.makeText(this,
                "Floating button नहीं खुल पाया: Display over other apps की permission नहीं है।",
                Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { orb?.let { wm?.removeView(it) } } catch (_: Exception) {}
        orb = null
        super.onDestroy()
    }
}
