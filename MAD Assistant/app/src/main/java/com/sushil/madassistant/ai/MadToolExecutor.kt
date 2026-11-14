package com.sushil.madassistant.ai

import android.content.Context
import com.sushil.madassistant.data.db.CategoryEntity
import com.sushil.madassistant.data.db.CustomerEntity
import com.sushil.madassistant.data.db.ItemEntity
import com.sushil.madassistant.data.db.MadDatabase
import com.sushil.madassistant.data.db.MemoryNoteEntity
import com.sushil.madassistant.data.db.ShoppingItemEntity
import com.sushil.madassistant.data.db.ShoppingListEntity
import com.sushil.madassistant.data.db.TaskEntity
import com.sushil.madassistant.data.db.TransactionEntity
import com.sushil.madassistant.data.db.WorkRecordEntity
import com.sushil.madassistant.data.db.WorkRecordItemEntity
import com.sushil.madassistant.pdf.PdfEngine
import com.sushil.madassistant.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MadToolExecutor(
    private val context: Context,
    private val db: MadDatabase
) {

    val today: String get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    private fun out(vararg pairs: Pair<String, Any>): JSONObject {
        val o = JSONObject()
        pairs.forEach { (k, v) -> o.put(k, v) }
        return o
    }

    private fun err(msg: String): JSONObject = JSONObject().put("error", msg)

    private fun customerJson(c: CustomerEntity): JSONObject = JSONObject()
        .put("id", c.id).put("name", c.name).put("fatherName", c.fatherName)
        .put("mobile", c.mobile).put("address", c.address)

    fun buildToolDefinitions(): JSONArray {
        val tools = JSONArray()

        fun prop(type: String, desc: String = ""): JSONObject =
            JSONObject().put("type", type).put("description", desc)

        fun tool(name: String, desc: String, params: Map<String, JSONObject> = emptyMap()) {
            val t = JSONObject().put("name", name).put("description", desc)
            t.put("parameters", JSONObject().put("type", "object")
                .put("properties", JSONObject().apply { params.forEach { (k, v) -> put(k, v) } }))
            tools.put(t)
        }

        tool("search_customer", "Search customers by name, father name, mobile or address. If multiple match, ask user which one.",
            mapOf("query" to prop("string", "Search text in Hindi or English")))
        tool("add_customer", "Add a new customer permanently.",
            mapOf("name" to prop("string"), "fatherName" to prop("string"),
                "mobile" to prop("string"), "address" to prop("string")))
        tool("add_credit_entry", "Record udhaar: customer owes us money (work done, payment pending).",
            mapOf("customerId" to prop("integer"), "amount" to prop("number", "Rupees"),
                "work" to prop("string", "Work description")))
        tool("record_payment", "Record payment received from customer.",
            mapOf("customerId" to prop("integer"), "amount" to prop("number"),
                "work" to prop("string"), "mode" to prop("string", "Cash or UPI or Online")))
        tool("get_customer_ledger", "Get full khata history and balance of a customer.",
            mapOf("customerId" to prop("integer")))
        tool("get_all_customers", "List all customers with balances.")
        tool("get_today_summary", "Today business summary: earned, received, entries count.")
        tool("get_work_types", "List available work types.")
        tool("get_categories", "List categories of a work type.",
            mapOf("workType" to prop("string")))
        tool("get_subcategories", "List subcategories under a category.",
            mapOf("workType" to prop("string"), "categoryName" to prop("string")))
        tool("get_items_for_stage", "Get material items for a work stage. Ask about these one by one.",
            mapOf("workType" to prop("string"), "categoryName" to prop("string"),
                "subcategoryName" to prop("string")))
        tool("create_work_record", "Create material list header for customer and stage. Returns workRecordId.",
            mapOf("customerId" to prop("integer"), "workType" to prop("string"),
                "categoryName" to prop("string"), "subcategoryName" to prop("string")))
        tool("add_material_item", "Add material with quantity to the work record.",
            mapOf("workRecordId" to prop("integer"), "itemName" to prop("string"),
                "quantity" to prop("string", "like 6 or 1 Bundle or 10 Meter"),
                "unit" to prop("string"), "remark" to prop("string")))
        tool("get_work_record_items", "Read materials collected so far.",
            mapOf("workRecordId" to prop("integer")))
        tool("finalize_work_pdf", "Generate the material list PDF. Returns path.",
            mapOf("workRecordId" to prop("integer")))
        tool("get_customer_work_history", "All material lists made for a customer.",
            mapOf("customerId" to prop("integer")))
        tool("add_category", "Add new category or subcategory.",
            mapOf("workType" to prop("string"), "name" to prop("string"),
                "parentCategoryName" to prop("string", "empty for top category")))
        tool("add_item", "Add new material item to a category or subcategory.",
            mapOf("workType" to prop("string"), "categoryName" to prop("string"),
                "subcategoryName" to prop("string"), "itemName" to prop("string"),
                "unit" to prop("string", "Piece Bundle Meter Roll Box Packet Kg Set")))
        tool("create_shopping_list", "Create or reuse a shop-wise shopping list.",
            mapOf("shopName" to prop("string")))
        tool("add_shopping_item", "Add item to a shop shopping list.",
            mapOf("shopName" to prop("string"), "itemName" to prop("string"),
                "quantity" to prop("string")))
        tool("get_shop_items", "Read a shop current shopping list.",
            mapOf("shopName" to prop("string")))
        tool("complete_shopping_list", "Delete a completed temporary shopping list. CONFIRM with user first.",
            mapOf("shopName" to prop("string")))
        tool("create_task", "Create personal task or reminder.",
            mapOf("title" to prop("string"), "date" to prop("string", "dd/MM/yyyy"),
                "time" to prop("string", "HH:mm"), "repeat" to prop("string", "None or Daily or Weekly")))
        tool("get_daily_schedule", "Today tasks and reminders.")
        tool("complete_task", "Mark task done by title match.",
            mapOf("title" to prop("string")))
        tool("remember_note", "Save personal memory note.",
            mapOf("content" to prop("string"), "permanent" to prop("boolean")))
        tool("search_memory", "Search personal memory notes.",
            mapOf("query" to prop("string")))

        return tools
    }

    suspend fun execute(name: String, args: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        try {
            when (name) {
                "search_customer" -> {
                    val q = args.optString("query").trim()
                    if (q.isEmpty()) return@withContext err("query empty")
                    val r = db.customerDao().searchCustomers(q)
                    out("count" to r.size,
                        "customers" to JSONArray().apply { r.forEach { put(customerJson(it)) } },
                        "instruction" to if (r.size > 1)
                            "MULTIPLE matches - ask which one using fatherName or address, NEVER auto-pick"
                        else "ok")
                }
                "add_customer" -> {
                    val nm = args.optString("name").trim()
                    if (nm.isEmpty()) return@withContext err("name required")
                    val exists = db.customerDao().searchCustomers(nm)
                        .firstOrNull { it.name.equals(nm, ignoreCase = true) }
                    if (exists != null) {
                        return@withContext out("status" to "exists",
                            "customer" to customerJson(exists),
                            "message" to "This name already exists - reusing it")
                    }
                    val id = db.customerDao().insertCustomer(CustomerEntity(
                        name = nm,
                        fatherName = args.optString("fatherName").trim(),
                        mobile = args.optString("mobile").trim(),
                        address = args.optString("address").trim()
                    ))
                    val saved = db.customerDao().getCustomerById(id)
                    if (saved != null) out("status" to "success", "id" to id)
                    else err("DB verify failed")
                }
                "add_credit_entry" -> {
                    val cid = args.optLong("customerId")
                    val amt = args.optDouble("amount", 0.0)
                    val work = args.optString("work").ifBlank { "काम" }
                    if (amt <= 0.0) return@withContext err("amount must be greater than 0")
                    if (db.customerDao().getCustomerById(cid) == null)
                        return@withContext err("customer not found - search first")
                    if (db.transactionDao().countSimilar(cid, work, amt, 0.0, today) > 0)
                        return@withContext out("status" to "duplicate",
                            "message" to "Same entry already exists today - not added again")
                    val id = db.transactionDao().insertTransaction(TransactionEntity(
                        customerId = cid, date = today, work = work,
                        lene = amt, aaye = 0.0, discount = 0.0,
                        paymentMode = "Udhaar", totalDelta = amt
                    ))
                    val ok = db.transactionDao().getTransactionById(id)
                    if (ok != null) out("status" to "success", "message" to "Credit added")
                    else err("DB verify failed")
                }
                "record_payment" -> {
                    val cid = args.optLong("customerId")
                    val amt = args.optDouble("amount", 0.0)
                    if (amt <= 0.0) return@withContext err("amount must be greater than 0")
                    if (db.customerDao().getCustomerById(cid) == null)
                        return@withContext err("customer not found")
                    if (db.transactionDao().countSimilar(cid, "", 0.0, amt, today) > 0)
                        return@withContext out("status" to "duplicate",
                            "message" to "Same payment already recorded today")
                    val id = db.transactionDao().insertTransaction(TransactionEntity(
                        customerId = cid, date = today, work = args.optString("work"),
                        lene = 0.0, aaye = amt, discount = 0.0,
                        paymentMode = args.optString("mode", "Cash"), totalDelta = -amt
                    ))
                    val ok = db.transactionDao().getTransactionById(id)
                    if (ok != null) out("status" to "success", "message" to "Payment recorded")
                    else err("DB verify failed")
                }
                "get_customer_ledger" -> {
                    val cid = args.optLong("customerId")
                    val c = db.customerDao().getCustomerById(cid)
                        ?: return@withContext err("customer not found")
                    val txs = db.transactionDao().getAllTransactionsOnce()
                        .filter { it.customerId == cid }
                    out("customer" to c.name,
                        "balance" to txs.sumOf { it.totalDelta },
                        "entries" to JSONArray().apply {
                            txs.take(20).forEach { t ->
                                put(JSONObject().put("date", t.date).put("work", t.work)
                                    .put("lene", t.lene).put("aaye", t.aaye))
                            }
                        })
                }
                "get_all_customers" -> {
                    val cs = db.customerDao().getAllCustomersOnce()
                    val txs = db.transactionDao().getAllTransactionsOnce()
                    out("customers" to JSONArray().apply {
                        cs.forEach { c ->
                            put(JSONObject().put("id", c.id).put("name", c.name)
                                .put("balance", txs.filter { it.customerId == c.id }.sumOf { it.totalDelta }))
                        }
                    })
                }
                "get_today_summary" -> {
                    val txs = db.transactionDao().getTransactionsByDate(today)
                    out("date" to today,
                        "totalLene" to txs.sumOf { it.lene },
                        "totalAaye" to txs.sumOf { it.aaye },
                        "entries" to txs.size)
                }
                else -> executePart2(name, args)
            }
        } catch (e: Exception) {
            err("error: " + e.message)
        }
    }

    private suspend fun executePart2(name: String, args: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        try {
            when (name) {
                "get_work_types" -> {
                    val types = db.catalogDao().getAllCategoriesOnce().map { it.workType }.distinct()
                    out("workTypes" to JSONArray(types))
                }
                "get_categories" -> {
                    val wt = args.optString("workType")
                    val cats = db.catalogDao().getAllCategoriesOnce()
                        .filter { it.workType == wt && it.parentId == 0L }
                    out("workType" to wt, "categories" to JSONArray(cats.map { it.name }))
                }
                "get_subcategories" -> {
                    val wt = args.optString("workType")
                    val parentName = args.optString("categoryName")
                    val parent = db.catalogDao().getAllCategoriesOnce()
                        .firstOrNull {
                            it.workType == wt && it.parentId == 0L &&
                                    it.name.equals(parentName, ignoreCase = true)
                        } ?: return@withContext err("category not found: " + parentName)
                    val subs = db.catalogDao().getSubCategoriesOnce(parent.id)
                    out("category" to parent.name, "subcategories" to JSONArray(subs.map { it.name }))
                }
                "get_items_for_stage" -> {
                    val cat = resolveStage(
                        args.optString("workType"),
                        args.optString("categoryName"),
                        args.optString("subcategoryName")
                    ) ?: return@withContext err("stage not found - check workType, category, subcategory")
                    val items = db.catalogDao().getItemsForCategoryOnce(cat.id)
                    out("stage" to cat.name, "items" to JSONArray().apply {
                        items.forEach { put(JSONObject().put("name", it.name).put("unit", it.unit)) }
                    })
                }
                "create_work_record" -> {
                    val cid = args.optLong("customerId")
                    if (db.customerDao().getCustomerById(cid) == null)
                        return@withContext err("customer not found - search or add first")
                    val wt = args.optString("workType")
                    val catName = args.optString("categoryName")
                    val subName = args.optString("subcategoryName")
                    val parent = db.catalogDao().findTopCategory(wt, catName)
                        ?: return@withContext err("category not found")
                    val sub = db.catalogDao().findSubCategory(parent.id, subName)
                    val id = db.workRecordDao().insertWorkRecord(WorkRecordEntity(
                        customerId = cid, workType = wt,
                        category = parent.name,
                        subcategory = sub?.name ?: "",
                        date = today
                    ))
                    val ok = db.workRecordDao().getWorkRecordById(id)
                    if (ok != null) out("status" to "success", "workRecordId" to id)
                    else err("DB verify failed")
                }
                "add_material_item" -> {
                    val rid = args.optLong("workRecordId")
                    if (db.workRecordDao().getWorkRecordById(rid) == null)
                        return@withContext err("work record not found")
                    val nm = args.optString("itemName").trim()
                    if (nm.isEmpty()) return@withContext err("itemName required")
                    val qty = args.optString("quantity").ifBlank { "1" }
                    val id = db.workRecordDao().insertWorkRecordItem(WorkRecordItemEntity(
                        workRecordId = rid, itemName = nm, quantity = qty,
                        unit = args.optString("unit"), remark = args.optString("remark")
                    ))
                    if (id > 0) out("status" to "success", "message" to nm + " added")
                    else err("DB verify failed")
                }
                "get_work_record_items" -> {
                    val rid = args.optLong("workRecordId")
                    val items = db.workRecordDao().getWorkRecordItems(rid)
                    out("items" to JSONArray().apply {
                        items.forEach {
                            put(JSONObject().put("name", it.itemName)
                                .put("quantity", it.quantity).put("unit", it.unit))
                        }
                    })
                }
                "finalize_work_pdf" -> {
                    val rid = args.optLong("workRecordId")
                    val rec = db.workRecordDao().getWorkRecordById(rid)
                        ?: return@withContext err("work record not found")
                    val f = PdfEngine.generateWorkListPdf(context, db, rec)
                    if (f != null && f.exists()) {
                        db.workRecordDao().updateWorkRecord(rec.copy(pdfPath = f.absolutePath))
                        out("status" to "success", "path" to f.absolutePath, "fileName" to f.name)
                    } else err("PDF generation failed")
                }
                "get_customer_work_history" -> {
                    val cid = args.optLong("customerId")
                    val recs = db.workRecordDao().getWorkRecordsForCustomer(cid)
                    val list = recs
                    out("records" to JSONArray().apply {
                        kotlinx.coroutines.flow.first(list).forEach { r ->
                            put(JSONObject().put("id", r.id).put("workType", r.workType)
                                .put("category", r.category).put("subcategory", r.subcategory)
                                .put("date", r.date).put("pdfPath", r.pdfPath))
                        }
                    })
                }
                "add_category" -> {
                    val wt = args.optString("workType")
                    val nm = args.optString("name").trim()
                    if (nm.isEmpty()) return@withContext err("name required")
                    val parentName = args.optString("parentCategoryName").trim()
                    if (parentName.isEmpty()) {
                        val id = db.catalogDao().insertCategory(
                            CategoryEntity(workType = wt, name = nm, parentId = 0))
                        if (id > 0) out("status" to "success", "id" to id) else err("DB verify failed")
                    } else {
                        val parent = db.catalogDao().findTopCategory(wt, parentName)
                            ?: return@withContext err("parent category not found")
                        val id = db.catalogDao().insertCategory(
                            CategoryEntity(workType = wt, name = nm, parentId = parent.id))
                        if (id > 0) out("status" to "success", "id" to id) else err("DB verify failed")
                    }
                }
                "add_item" -> {
                    val wt = args.optString("workType")
                    val catName = args.optString("categoryName")
                    val subName = args.optString("subcategoryName").trim()
                    val nm = args.optString("itemName").trim()
                    if (nm.isEmpty()) return@withContext err("itemName required")
                    val cat = resolveStage(wt, catName, subName)
                        ?: return@withContext err("category or subcategory not found")
                    val id = db.catalogDao().insertItem(
                        ItemEntity(categoryId = cat.id, name = nm,
                            unit = args.optString("unit").ifBlank { "Piece" }))
                    if (id > 0) out("status" to "success", "id" to id) else err("DB verify failed")
                }
                "create_shopping_list" -> {
                    val shop = args.optString("shopName").trim()
                    if (shop.isEmpty()) return@withContext err("shopName required")
                    val existing = db.shoppingDao().findListByShop(shop)
                    if (existing != null) return@withContext out("status" to "exists", "listId" to existing.id)
                    val id = db.shoppingDao().insertList(ShoppingListEntity(shopName = shop))
                    if (id > 0) out("status" to "success", "listId" to id) else err("DB verify failed")
                }
                "add_shopping_item" -> {
                    val shop = args.optString("shopName").trim()
                    val nm = args.optString("itemName").trim()
                    if (shop.isEmpty() || nm.isEmpty()) return@withContext err("shopName and itemName required")
                    var list = db.shoppingDao().findListByShop(shop)
                    if (list == null) {
                        db.shoppingDao().insertList(ShoppingListEntity(shopName = shop))
                        list = db.shoppingDao().findListByShop(shop)
                    }
                    if (list == null) return@withContext err("could not create list")
                    val id = db.shoppingDao().insertShoppingItem(ShoppingItemEntity(
                        listId = list.id, itemName = nm, quantity = args.optString("quantity")))
                    if (id > 0) out("status" to "success", "message" to nm + " added to " + shop + " list")
                    else err("DB verify failed")
                }
                "get_shop_items" -> {
                    val shop = args.optString("shopName").trim()
                    val list = db.shoppingDao().findListByShop(shop)
                        ?: return@withContext out("items" to JSONArray(), "message" to "No list for this shop yet")
                    val items = db.shoppingDao().getShoppingItems(list.id)
                    out("shop" to list.shopName, "items" to JSONArray().apply {
                        items.forEach {
                            put(JSONObject().put("name", it.itemName)
                                .put("quantity", it.quantity).put("done", it.isDone))
                        }
                    })
                }
                "complete_shopping_list" -> {
                    val shop = args.optString("shopName").trim()
                    val list = db.shoppingDao().findListByShop(shop)
                        ?: return@withContext err("no shopping list found for this shop")
                    db.shoppingDao().deleteItemsOfList(list.id)
                    db.shoppingDao().deleteListById(list.id)
                    out("status" to "success", "message" to "List deleted. Other shop lists untouched.")
                }
                "create_task" -> {
                    val title = args.optString("title").trim()
                    if (title.isEmpty()) return@withContext err("title required")
                    val date = args.optString("date")
                    val time = args.optString("time")
                    var remindAt = 0L
                    if (date.isNotEmpty() && time.isNotEmpty()) {
                        try {
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            remindAt = sdf.parse(date + " " + time)?.time ?: 0L
                        } catch (_: Exception) { remindAt = 0L }
                    }
                    val id = db.taskDao().insertTask(TaskEntity(
                        title = title, date = date, time = time,
                        reminderAtMillis = remindAt, repeat = args.optString("repeat", "None")
                    ))
                    val ok = db.taskDao().getTaskById(id)
                    if (ok != null) {
                        if (remindAt > System.currentTimeMillis()) {
                            ReminderScheduler.schedule(context, id, title, remindAt)
                        }
                        out("status" to "success", "id" to id, "reminderSet" to (remindAt > 0))
                    } else err("DB verify failed")
                }
                "get_daily_schedule" -> {
                    val tasks = db.taskDao().getAllTasksOnce().filter { !it.isDone }
                    out("tasks" to JSONArray().apply {
                        tasks.forEach {
                            put(JSONObject().put("title", it.title).put("date", it.date)
                                .put("time", it.time).put("done", it.isDone))
                        }
                    })
                }
                "complete_task" -> {
                    val title = args.optString("title").trim()
                    if (title.isEmpty()) return@withContext err("title required")
                    val task = db.taskDao().getAllTasksOnce()
                        .firstOrNull { !it.isDone && it.title.contains(title, ignoreCase = true) }
                        ?: return@withContext err("pending task not found")
                    db.taskDao().markDone(task.id, true)
                    if (task.reminderAtMillis > 0) ReminderScheduler.cancel(context, task.id)
                    out("status" to "success", "message" to "Task marked done")
                }
                "remember_note" -> {
                    val content = args.optString("content").trim()
                    if (content.isEmpty()) return@withContext err("content required")
                    val id = db.memoryDao().insertNote(MemoryNoteEntity(
                        content = content, isPermanent = args.optBoolean("permanent", false)))
                    if (id > 0) out("status" to "success", "id" to id) else err("DB verify failed")
                }
                "search_memory" -> {
                    val q = args.optString("query").trim()
                    if (q.isEmpty()) return@withContext err("query required")
                    val notes = db.memoryDao().searchNotes(q)
                    out("notes" to JSONArray().apply {
                        notes.forEach {
                            put(JSONObject().put("content", it.content)
                                .put("permanent", it.isPermanent))
                        }
                    })
                }
                else -> err("unknown tool: " + name)
            }
        } catch (e: Exception) {
            err("error: " + e.message)
        }
    }

    private suspend fun resolveStage(workType: String, catName: String, subName: String): CategoryEntity? {
        val parent = db.catalogDao().findTopCategory(workType, catName) ?: return null
        if (subName.isBlank()) return parent
        return db.catalogDao().findSubCategory(parent.id, subName) ?: parent
    }
}
