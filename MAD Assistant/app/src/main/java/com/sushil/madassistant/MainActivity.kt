package com.sushil.madassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sushil.madassistant.ui.theme.MadCyan
import com.sushil.madassistant.ui.theme.MadGold
import com.sushil.madassistant.ui.theme.MadSurface
import com.sushil.madassistant.ui.theme.MadTextPrimary
import com.sushil.madassistant.ui.theme.MadTheme
import com.sushil.madassistant.viewmodel.ChatState
import com.sushil.madassistant.viewmodel.MadViewModel

class MainActivity : ComponentActivity() {

    private var voiceInputText: String? = null

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingVoiceStart?.invoke()
    }

    private var pendingVoiceStart: (() -> Unit)? = null

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceInputText = intent.getStringExtra("voice_text")

        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MadTheme {
                val vm: MadViewModel = viewModel()
                LaunchedEffect(voiceInputText) {
                    voiceInputText?.let {
                        vm.sendUserMessage(it, speak = true)
                        voiceInputText = null
                    }
                }
                MadApp(vm) { startMic ->
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                        startMic()
                    } else {
                        pendingVoiceStart = startMic
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        }
    }
}

@Composable
fun MadApp(vm: MadViewModel, micLauncher: (() -> Unit) -> Unit) {
    var showSettings by remember { mutableStateOf(false) }

    if (!showSettings) {
        ChatScreen(vm = vm, micLauncher = micLauncher, onOpenSettings = { showSettings = true })
    } else {
        com.sushil.madassistant.ui.screens.SettingsScreen(vm = vm, onBack = { showSettings = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: MadViewModel,
    micLauncher: (() -> Unit) -> Unit,
    onOpenSettings: () -> Unit
) {
    val messages by vm.chatMessages.collectAsState(initial = emptyList())
    val chatState by vm.chatState.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAD Assistant", color = MadCyan, fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = MadGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MadSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.role == "user"
                    Text(
                        text = msg.content,
                        color = if (isUser) MadGold else MadTextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isUser) 40.dp else 0.dp,
                                end = if (isUser) 0.dp else 40.dp
                            )
                    )
                }
                when (chatState) {
                    is ChatState.Thinking -> item { Text("... MAD soch rahi hai", color = MadCyan, fontSize = 13.sp) }
                    is ChatState.Listening -> item { Text("... Sun rahi hoon", color = MadCyan, fontSize = 13.sp) }
                    else -> {}
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { micLauncher { vm.startVoiceInput() } }) {
                    Icon(Icons.Default.Mic, "Mic", tint = MadCyan, modifier = Modifier.size(32.dp))
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("बात कीजिए...") }
                )
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            vm.sendUserMessage(input)
                            input = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, "Send", tint = MadGold, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
