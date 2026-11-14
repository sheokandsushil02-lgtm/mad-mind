package com.sushil.madassistant.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.sushil.madassistant.MainActivity
import com.sushil.madassistant.R

class FloatingButtonService : Service() {

    companion object {
        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val i = Intent(context, FloatingButtonService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i)
            else context.startService(i)
        }
        fun stop(context: Context) = context.stopService(Intent(context, FloatingButtonService::class.java))
    }

    private var wm: WindowManager? = null
    private var orb: View? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
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
                2003,
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
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { orb?.let { wm?.removeView(it) } } catch (_: Exception) {}
        orb = null
        super.onDestroy()
    }
}
