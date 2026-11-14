package com.sushil.madassistant.service

import android.content.Context

object MadConversationBridge {
    @Volatile var handler: ((Context, String) -> Unit)? = null

    fun onVoiceInput(context: Context, text: String) {
        val h = handler
        if (h != null) {
            h(context, text)
        } else {
            val intent = android.content.Intent(context, com.sushil.madassistant.MainActivity::class.java).apply {
                putExtra("voice_text", text)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(intent) } catch (_: Exception) {}
        }
    }
}
