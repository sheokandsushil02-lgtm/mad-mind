package com.sushil.madassistant.backup

import android.content.Context
import com.sushil.madassistant.data.db.MadDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun exportAll(context: Context, db: MadDatabase): File? = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("app", "MAD Assistant")
            root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            root.put("customers", JSONArray().apply {
                db.customerDao().getAllCustomersOnce().forEach {
                    put(JSONObject().put("id", it.id).put("name", it.name)
                        .put("fatherName", it.fatherName).put("mobile", it.mobile)
                        .put("address", it.address).put("createdAt", it.createdAt))
                }
            })

            root.put("transactions", JSONArray().apply {
                db.transactionDao().getAllTransactionsOnce().forEach {
                    put(JSONObject().put("id", it.id).put("customerId", it.customerId)
                        .put("date", it.date).put("work", it.work).put("lene", it.lene)
                        .put("aaye", it.aaye).put("discount", it.discount)
                        .put("paymentMode", it.paymentMode).put("totalDelta", it.totalDelta)
                        .put("timestamp", it.timestamp))
                }
            })

            root.put("categories", JSONArray().apply {
                db.catalogDao().getAllCategoriesOnce().forEach {
                    put(JSONObject().put("id", it.id).put("workType", it.workType)
                        .put("name", it.name).put("parentId", it.parentId)
                        .put("sortOrder", it.sortOrder))
                }
            })

            root.put("items", JSONArray().apply {
                db.catalogDao().getAllItemsOnce().forEach {
                    put(JSONObject().put("id", it.id).put("categoryId", it.categoryId)
                        .put("name", it.name).put("unit", it.unit).put("sortOrder", it.sortOrder))
                }
            })

            root.put("tasks", JSONArray().apply {
                db.taskDao().getAllTasksOnce().forEach {
                    put(JSONObject().put("id", it.id).put("title", it.title)
                        .put("date", it.date).put("time", it.time)
                        .put("reminderAtMillis", it.reminderAtMillis)
                        .put("repeat", it.repeat).put("isDone", it.isDone))
                }
            })

            root.put("memoryNotes", JSONArray().apply {
                db.memoryDao().getAllNotesOnce().forEach {
                    put(JSONObject().put("id", it.id).put("content", it.content)
                        .put("isPermanent", it.isPermanent).put("createdAt", it.createdAt))
                }
            })

            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) dir.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "mad_backup_" + stamp + ".json")
            FileOutputStream(file).use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            file
        } catch (e: Exception) {
            null
        }
    }
}
