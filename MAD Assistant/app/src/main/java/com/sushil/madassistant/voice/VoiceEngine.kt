package com.sushil.madassistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class VoiceEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening

    private var recognizeCallback: ((String) -> Unit)? = null
    private var utteranceDone: (() -> Unit)? = null

    fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                try {
                    tts?.language = Locale("hi", "IN")
                    tts?.setPitch(1.15f)
                    tts?.setSpeechRate(0.95f)
                    pickFemaleVoice()
                } catch (_: Exception) {}
            }
        }
    }

    private fun pickFemaleVoice() {
        try {
            val voices = tts?.voices ?: return
            val preferred = voices.firstOrNull { v ->
                v.locale.toLanguageTag().startsWith("hi") &&
                        (v.name.contains("hie", true) || v.name.contains("female", true) ||
                                v.name.contains("ria", true))
            } ?: voices.firstOrNull { it.locale.language == "hi" }
            if (preferred != null) tts?.voice = preferred
        } catch (_: Exception) {}
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) { onDone?.invoke(); return }
        if (!ttsReady) { onDone?.invoke(); return }
        utteranceDone = onDone
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                utteranceDone?.invoke()
                utteranceDone = null
            }
            override fun onError(id: String?) {
                utteranceDone?.invoke()
                utteranceDone = null
            }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mad_utt")
    }

    fun stopSpeaking() { try { tts?.stop() } catch (_: Exception) {} }

    fun isRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(onRecognized: (String) -> Unit) {
        recognizeCallback = onRecognized
        if (recognizer == null) {
            recognizer = if (SpeechRecognizer.isRecognitionAvailable(context))
                SpeechRecognizer.createSpeechRecognizer(context) else null
        }
        if (recognizer == null) { onRecognized(""); return }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _listening.value = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _listening.value = false }
            override fun onError(error: Int) {
                _listening.value = false
                if (error == 7 || error == 6) recognizeCallback?.invoke("")
            }
            override fun onResults(results: Bundle?) {
                _listening.value = false
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = list?.firstOrNull()?.trim() ?: ""
                recognizeCallback?.invoke(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        _listening.value = false
        try { recognizer?.stopListening() } catch (_: Exception) {}
    }

    fun destroy() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        recognizer = null
        tts = null
        ttsReady = false
    }
}
