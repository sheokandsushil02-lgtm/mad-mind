package com.sushil.madassistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sushil.madassistant.backup.BackupManager
import com.sushil.madassistant.service.FloatingButtonService
import com.sushil.madassistant.service.MadVoiceService
import com.sushil.madassistant.ui.theme.MadCyan
import com.sushil.madassistant.ui.theme.MadGold
import com.sushil.madassistant.ui.theme.MadSurface
import com.sushil.madassistant.viewmodel.MadViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MadViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val provider by vm.settingsStore.provider.collectAsState(initial = "gemini")
    val geminiKey by vm.settingsStore.geminiKey.collectAsState(initial = "")
    val openaiKey by vm.settingsStore.openaiKey.collectAsState(initial = "")
    val techName by vm.settingsStore.techName.collectAsState(initial = "Sushil Sheokand")
    val techMobile by vm.settingsStore.techMobile.collectAsState(initial = "")
    val techBusiness by vm.settingsStore.techBusiness.collectAsState(initial = "")
    val techAddress by vm.settingsStore.techAddress.collectAsState(initial = "")

    var gKey by remember { mutableStateOf("") }
    var oKey by remember { mutableStateOf("") }
    var tName by remember { mutableStateOf("") }
    var tMobile by remember { mutableStateOf("") }
    var tBusiness by remember { mutableStateOf("") }
    var tAddress by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = MadCyan) },
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
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AI Provider", color = MadGold, fontSize = 16.sp)
            Row {
                RadioButton(selected = provider == "gemini", onClick = {
                    scope.launch { vm.settingsStore.setProvider("gemini") }
                })
                Text("Gemini", color = MadCyan, modifier = Modifier.padding(top = 12.dp))
                RadioButton(selected = provider == "openai", onClick = {
                    scope.launch { vm.settingsStore.setProvider("openai") }
                })
                Text("OpenAI", color = MadCyan, modifier = Modifier.padding(top = 12.dp))
            }

            OutlinedTextField(
                value = gKey.ifBlank { geminiKey },
                onValueChange = { gKey = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                scope.launch { vm.settingsStore.setGeminiKey(gKey) }
            }) { Text("Save Gemini Key") }

            OutlinedTextField(
                value = oKey.ifBlank { openaiKey },
                onValueChange = { oKey = it },
                label = { Text("OpenAI API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                scope.launch { vm.settingsStore.setOpenaiKey(oKey) }
            }) { Text("Save OpenAI Key") }

            Text("Technician Profile (PDF header)", color = MadGold, fontSize = 16.sp)
            OutlinedTextField(
                value = tName.ifBlank { techName },
                onValueChange = { tName = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = tMobile.ifBlank { techMobile },
                onValueChange = { tMobile = it },
                label = { Text("Mobile") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = tBusiness.ifBlank { techBusiness },
                onValueChange = { tBusiness = it },
                label = { Text("Business Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = tAddress.ifBlank { techAddress },
                onValueChange = { tAddress = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                scope.launch {
                    vm.settingsStore.setTechnician(
                        tName.ifBlank { techName },
                        tMobile.ifBlank { techMobile },
                        tBusiness.ifBlank { techBusiness },
                        tAddress.ifBlank { techAddress }
                    )
                }
            }) { Text("Save Profile") }

            Text("Background Assistant", color = MadGold, fontSize = 16.sp)
            Button(onClick = { MadVoiceService.start(context) }) {
                Text("Start MAD Background Service")
            }
            Button(onClick = { MadVoiceService.stop(context) }) {
                Text("Stop Background Service")
            }
            Button(onClick = {
                if (Settings.canDrawOverlays(context)) {
                    FloatingButtonService.start(context)
                } else {
                    context.startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName)
                    ))
                }
            }) { Text("Floating MAD Button") }

            if (Build.VERSION.SDK_INT >= 23) {
                Button(onClick = {
                    context.startActivity(Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + context.packageName)
                    ))
                }) { Text("Battery Optimization Exempt (recommended)") }
            }

            Text("Backup", color = MadGold, fontSize = 16.sp)
            Button(onClick = {
                scope.launch {
                    val f = BackupManager.exportAll(context, vm.database)
                    if (f != null && f.exists()) {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.packageName + ".fileprovider",
                            f
                        )
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(Intent.createChooser(share, "Backup Share"))
                        } catch (_: Exception) {}
                    }
                }
            }) { Text("Export Full Backup (JSON)") }

            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Chat")
            }
        }
    }
}
