package com.sushil.madassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
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
import com.sushil.madassistant.data.db.TaskEntity
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

    var showAddTask by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }
    var showAddList by remember { mutableStateOf(false) }

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
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard("Customers", customers.size.toString(), Modifier.weight(1f), MadCyan)
                        StatCard("Pending Tasks", tasks.count { !it.isDone }.toString(), Modifier.weight(1f), MadGold)
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

                // ---- Tasks ----
                item {
                    SectionHeader("Tasks / Reminders") { showAddTask = !showAddTask }
                }
                if (showAddTask) item {
                    AddTaskForm(onAdd = { title, date, time ->
                        scope.launch { vm.addTask(title, date, time) }
                        showAddTask = false
                    })
                }
                items(tasks.size) { idx ->
                    TaskRow(
                        task = tasks[idx],
                        onToggle = { scope.launch { vm.toggleTask(tasks[idx]) } },
                        onDelete = { scope.launch { vm.deleteTask(tasks[idx]) } }
                    )
                }

                // ---- Shopping Lists ----
                item {
                    SectionHeader("Shopping Lists") { showAddList = !showAddList }
                }
                if (showAddList) item {
                    AddShoppingListForm(onAdd = { shop, item ->
                        scope.launch { vm.addShoppingItem(shop, item) }
                        showAddList = false
                    })
                }
                items(shoppingLists.size) { idx ->
                    val list = shoppingLists[idx]
                    ShoppingListRow(
                        listName = list.shopName,
                        onDelete = { scope.launch { vm.deleteShoppingList(list) } }
                    )
                }

                // ---- Work Records ----
                item { SectionHeader("Work Records") { } }
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

                // ---- Memory Notes ----
                item {
                    SectionHeader("Memory Notes") { showAddNote = !showAddNote }
                }
                if (showAddNote) item {
                    AddNoteForm(onAdd = { content, permanent ->
                        scope.launch { vm.addMemoryNote(content, permanent) }
                        showAddNote = false
                    })
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
                                if (n.isPermanent) "Permanent" else "Temp",
                                color = if (n.isPermanent) MadGold else MadCyan,
                                fontSize = 12.sp
                            )
                            TextButton(onClick = { scope.launch { vm.deleteMemoryNote(n) } }) {
                                Text("Delete", color = MadCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
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
fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = MadGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, "Add", tint = MadCyan)
        }
    }
}

@Composable
fun AddTaskForm(onAdd: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MadSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Task title") }, modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date, onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = time, onValueChange = { time = it },
                    label = { Text("Time (HH:MM)") }, modifier = Modifier.weight(1f)
                )
            }
            Button(onClick = { if (title.isNotBlank()) onAdd(title.trim(), date.trim(), time.trim()) }) {
                Text("Save Task")
            }
        }
    }
}

@Composable
fun TaskRow(task: TaskEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MadSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(task.title, color = MadTextPrimary, fontSize = 15.sp)
                if (task.date.isNotBlank()) {
                    Text("${task.date} ${task.time}", color = MadCyan, fontSize = 12.sp)
                }
            }
            TextButton(onClick = onDelete) { Text("Delete", color = MadGold, fontSize = 12.sp) }
        }
    }
}

@Composable
fun AddShoppingListForm(onAdd: (String, String) -> Unit) {
    var shop by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MadSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = shop, onValueChange = { shop = it },
                label = { Text("Shop name") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = item, onValueChange = { item = it },
                label = { Text("Item") }, modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                if (shop.isNotBlank() && item.isNotBlank()) onAdd(shop.trim(), item.trim())
            }) { Text("Add to Shopping List") }
        }
    }
}

@Composable
fun ShoppingListRow(listName: String, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MadSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(listName, color = MadTextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onDelete) { Text("Delete", color = MadGold, fontSize = 12.sp) }
        }
    }
}

@Composable
fun AddNoteForm(onAdd: (String, Boolean) -> Unit) {
    var content by remember { mutableStateOf("") }
    var permanent by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MadSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = content, onValueChange = { content = it },
                label = { Text("Note (memory)") }, modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = permanent, onCheckedChange = { permanent = it })
                Text("Permanent", color = MadTextPrimary, fontSize = 13.sp)
            }
            Button(onClick = { if (content.isNotBlank()) onAdd(content.trim(), permanent) }) {
                Text("Save Note")
            }
        }
    }
}
