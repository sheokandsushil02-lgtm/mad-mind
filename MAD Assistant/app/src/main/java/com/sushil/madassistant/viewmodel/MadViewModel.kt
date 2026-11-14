package com.sushil.madassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sushil.madassistant.ai.AiEngine
import com.sushil.madassistant.ai.MadToolExecutor
import com.sushil.madassistant.data.CatalogSeeder
import com.sushil.madassistant.data.db.ChatMessageEntity
import com.sushil.madassistant.data.db.MadDatabase
import com.sushil.madassistant.data.settings.SettingsStore
import com.sushil.madassistant.service.MadConversationBridge
import com.sushil.madassistant.voice.VoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ChatState {
    object Idle : ChatState()
    object Thinking : ChatState()
    object Listening : ChatState()
}

class MadViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MadDatabase.getDatabase(application)
    private val settings = SettingsStore(application)
    private val aiEngine = AiEngine(settings)
    private val toolExecutor = MadToolExecutor(application, db)

    val voiceEngine = VoiceEngine(application)

    val chatMessages = db.chatDao().observeAll()
    val allCustomers = db.customerDao().getAllCustomers()
    val allTasks = db.taskDao().getAllTasks()
    val allShoppingLists = db.shoppingDao().getAllLists()
    val allWorkRecords = db.workRecordDao().getAllWorkRecords()
    val allMemoryNotes = db.memoryDao().getAllNotes()

    val settingsStore = settings
    val database = db

    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState: StateFlow<ChatState> = _chatState

    init {
        voiceEngine.initTts()
        viewModelScope.launch {
            CatalogSeeder.seedIfNeeded(db)
        }
        MadConversationBridge.handler = { _, text ->
            viewModelScope.launch { processInput(text, speak = true) }
        }
    }

    fun sendUserMessage(text: String, speak: Boolean = false) {
        if (text.isBlank()) return
        viewModelScope.launch { processInput(text, speak) }
    }

    private suspend fun processInput(text: String, speak: Boolean) {
        db.chatDao().insertMessage(ChatMessageEntity(role = "user", content = text))
        _chatState.value = ChatState.Thinking

        val history = db.chatDao().getRecentMessages()
            .sortedBy { it.timestamp }
            .takeLast(10)
            .map { (if (it.role == "assistant") "assistant" else "user") to it.content }

        val reply = aiEngine.converse(
            userText = text,
            history = history,
            toolDefs = toolExecutor.buildToolDefinitions(),
            executor = { name, args -> toolExecutor.execute(name, args) }
        )

        db.chatDao().insertMessage(ChatMessageEntity(role = "assistant", content = reply))
        _chatState.value = ChatState.Idle
        if (speak) voiceEngine.speak(reply)
    }

    fun startVoiceInput() {
        if (_chatState.value != ChatState.Listening) {
            _chatState.value = ChatState.Listening
            voiceEngine.startListening { text ->
                _chatState.value = ChatState.Idle
                if (text.isNotBlank()) sendUserMessage(text, speak = true)
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch { db.chatDao().clearChat() }
    }

    override fun onCleared() {
        MadConversationBridge.handler = null
        voiceEngine.destroy()
        super.onCleared()
    }
}
