package com.sushil.madassistant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sushil.madassistant.data.db.CustomerEntity
import com.suchil.madassistant.data.db.MadDatabase
import com.sushil.madassistant.data.db.MemoryNoteEntity
import com.sushil.madassistant.data.db.ShoppingListEntity
import com.sushil.madassistant.data.db.TaskEntity
import com.sushil.madassistant.data.db.WorkRecordEntity
import com.sushil.madassistant.service.MadVoiceService
import com.sushil.madassistant.ui.theme.MadCyan
import com.sushil.madassistant.ui.theme.MadGold
import com.sushil.madassistant.ui.theme.MadSurface
import com.sushil.madassistant.ui.theme.MadTextPrimary
import com.sushil.madassistant.viewmodel.MadViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: MadViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val customers by vm.allCustomers.collectAsState(initial = emptyList())
    val tasks by vm.allTasks.collectAsState(initial = emptyList())
    val shoppingLists by vm.allShoppingLists.collectAsState(initial = emptyList())
    val workRecords by vm.allWorkRecords.collectAsState(initial = emptyList())
    val memoryNotes by vm.allMemoryNotes.collectAsState(initial = emptyList())

    var addCustomerVisible by remember { mutableStateOf(false) }
    var addTaskVisible by remember { mutableStateOf(false) }
    var addNoteVisible by remember { mutableStateOf(false) }
    var addListVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAD Dashboard", color = MadCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MadGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MadSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status bar: Sleep / Wake
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { MadVoiceService.sleep(context) }) {
                    Text("😴 सुलाओ MAD", color = MadGold)
                }
                TextButton(onClick = { MadVoiceService.wake(context) }) {
                    Text("☀️ जगाओ MAD", color = MadCyan)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stats cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard("Customers", customers.size.toString(), Modifier.weight(1f), MadCyan)
                        StatCard("Tasks", tasks.count { !it.isDone }.toString(), Modifier.weight(1f), MadGold)
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard("Shopping Lists", shoppingLists.size.toString(), Modifier.weight(1f), MadCyan)
                        StatCard("Work Records", workRecords.size.toString(), Modifier.weight(1f), MadGold)
                    }
                }

                // Tasks section
                item {
                    SectionHeader("Tasks / Reminders", Modifier.fillMaxWidth()) {
                        addTaskVisible = !addTaskVisible
                    }
                }
                if (addTaskVisible) item {
                    AddTaskForm(vm) { addTaskVisible = }
                }
                items(tasks.size) { idx ->
                    val t = tasks[idx]
                    TaskRow(vm, t)
                }

                // Shopping lists section
                item {
                    SectionHeader("Shopping Lists", Modifier.fillMaxWidth()) {
                        addListVisible = !addListVisible
                    }
                }
                if (addListVisible) item {
                    AddShoppingListForm(vm) { addListVisible = false }
                }
                items(shoppingLists.size) { idx ->
                    ShoppingListRow(vm, shoppingLists[idx])
                }

                // Work records section
                item {
                    SectionHeader("Work Records", Modifier.fillMaxWidth()) { }
                }
                items(workRecords.size) { idx ->
                    val r = workRecords[idx]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MadSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${r.category} — ${r.subcategory}", color = MadTextPrimary, fontSize = 15.sp)
                            Text("Date: ${r.date}  Customer #${r.customerId}", color = MadCyan, fontSize = 12.sp)
                        }
                    }
                }

                // Memory notes section
                item {
                    SectionHeader("Memory Notes", Modifier.fillMaxWidth()) {
                        addNoteVisible = !addNoteVisible
                    }
                }
                if (addNoteVisible) item {
                    AddNoteForm(vm) { addNoteVisible = false }
                }
                items(memoryNotes.size) { idx ->
                    val n = memoryNotes[idx]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MadSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp)) {
                            Text(n.content, color = MadTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(
                                if (n.isPermanent) "P" else "T",
                                color = if (n.isPermanent) MadGold else MadCyan,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MadSurface),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MadTextPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = MadGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, "Add", tint = MadCyan)
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Delete, "Del", tint = MadCyan)
        }
    }
}
