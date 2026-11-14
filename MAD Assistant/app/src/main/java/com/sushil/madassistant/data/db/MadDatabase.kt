package com.sushil.madassistant.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val fatherName: String = "",
    val mobile: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions", indices = [Index("customerId")])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val date: String,
    val work: String = "",
    val lene: Double = 0.0,
    val aaye: Double = 0.0,
    val discount: Double = 0.0,
    val paymentMode: String = "Cash",
    val totalDelta: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories", indices = [Index("workType"), Index("parentId")])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workType: String,
    val name: String,
    val parentId: Long = 0,
    val sortOrder: Int = 0
)

@Entity(tableName = "items", indices = [Index("categoryId")])
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val unit: String = "Piece",
    val sortOrder: Int = 0
)

@Entity(tableName = "work_records", indices = [Index("customerId")])
data class WorkRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val workType: String,
    val category: String,
    val subcategory: String,
    val date: String,
    val pdfPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "work_record_items", indices = [Index("workRecordId")])
data class WorkRecordItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workRecordId: Long,
    val itemName: String,
    val quantity: String,
    val unit: String = "",
    val remark: String = ""
)

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopName: String,
    val isTemporary: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_list_items", indices = [Index("listId")])
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val itemName: String,
    val quantity: String = "",
    val isDone: Boolean = false
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String = "",
    val time: String = "",
    val reminderAtMillis: Long = 0,
    val repeat: String = "None",
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_notes")
data class MemoryNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val isPermanent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "technician_profile")
data class TechnicianProfileEntity(
    @PrimaryKey val id: Long = 1,
    val name: String = "Sushil Sheokand",
    val mobile: String = "",
    val businessName: String = "RAJAT ELECTRO-MECH SOLUTIONS (REMS)",
    val address: String = "Saffa Kheri Road, Kharak Bhura, Uchana"
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR fatherName LIKE '%' || :query || '%' OR mobile LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    suspend fun searchCustomers(query: String): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(c: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(c: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Long)

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE customerId = :customerId AND work = :work AND lene = :lene AND aaye = :aaye AND date = :date")
    suspend fun countSimilar(customerId: Long, work: String, lene: Double, aaye: Double, date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(t: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE date = :date")
    suspend fun getTransactionsByDate(date: String): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM categories WHERE workType = :workType AND parentId = 0 ORDER BY sortOrder, id")
    fun getTopCategories(workType: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY workType, parentId, sortOrder")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sortOrder, id")
    fun getSubCategories(parentId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sortOrder, id")
    suspend fun getSubCategoriesOnce(parentId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE workType = :workType AND parentId = 0 AND name LIKE '%' || :name || '%' ORDER BY sortOrder LIMIT 1")
    suspend fun findTopCategory(workType: String, name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE parentId = :parentId AND name LIKE '%' || :name || '%' LIMIT 1")
    suspend fun findSubCategory(parentId: Long, name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(c: CategoryEntity): Long

    @Update
    suspend fun updateCategory(c: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("SELECT * FROM items WHERE categoryId = :categoryId ORDER BY sortOrder, id")
    fun getItemsForCategory(categoryId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE categoryId = :categoryId ORDER BY sortOrder, id")
    suspend fun getItemsForCategoryOnce(categoryId: Long): List<ItemEntity>

    @Query("SELECT * FROM items")
    suspend fun getAllItemsOnce(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(i: ItemEntity): Long

    @Update
    suspend fun updateItem(i: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}

@Dao
interface WorkRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkRecord(r: WorkRecordEntity): Long

    @Update
    suspend fun updateWorkRecord(r: WorkRecordEntity)

    @Query("SELECT * FROM work_records WHERE id = :id")
    suspend fun getWorkRecordById(id: Long): WorkRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkRecordItem(i: WorkRecordItemEntity): Long

    @Query("SELECT * FROM work_records WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getWorkRecordsForCustomer(customerId: Long): Flow<List<WorkRecordEntity>>

    @Query("SELECT * FROM work_records ORDER BY createdAt DESC")
    fun getAllWorkRecords(): Flow<List<WorkRecordEntity>>

    @Query("SELECT * FROM work_record_items WHERE workRecordId = :workRecordId ORDER BY id")
    suspend fun getWorkRecordItems(workRecordId: Long): List<WorkRecordItemEntity>
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_lists ORDER BY createdAt DESC")
    fun getAllLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE shopName LIKE '%' || :shop || '%' ORDER BY createdAt DESC LIMIT 1")
    suspend fun findListByShop(shop: String): ShoppingListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(l: ShoppingListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(i: ShoppingItemEntity): Long

    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId ORDER BY id")
    suspend fun getShoppingItems(listId: Long): List<ShoppingItemEntity>

    @Query("UPDATE shopping_list_items SET isDone = :done WHERE id = :id")
    suspend fun markItemDone(id: Long, done: Boolean)

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteListById(listId: Long)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId")
    suspend fun deleteItemsOfList(listId: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isDone ASC, reminderAtMillis ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDone = 0")
    suspend fun getPendingTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(t: TaskEntity): Long

    @Update
    suspend fun updateTask(t: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("UPDATE tasks SET isDone = :done WHERE id = :id")
    suspend fun markDone(id: Long, done: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<MemoryNoteEntity>>

    @Query("SELECT * FROM memory_notes")
    suspend fun getAllNotesOnce(): List<MemoryNoteEntity>

    @Query("SELECT * FROM memory_notes WHERE content LIKE '%' || :q || '%' ORDER BY createdAt DESC LIMIT 10")
    suspend fun searchNotes(q: String): List<MemoryNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(n: MemoryNoteEntity): Long

    @Query("DELETE FROM memory_notes WHERE id = :id AND isPermanent = 0")
    suspend fun deleteTemporaryNote(id: Long)
}

@Dao
interface TechnicianDao {
    @Query("SELECT * FROM technician_profile WHERE id = 1")
    suspend fun getProfile(): TechnicianProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(p: TechnicianProfileEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 30")
    suspend fun getRecentMessages(): List<ChatMessageEntity>

    @Insert
    suspend fun insertMessage(m: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Database(
    entities = [
        CustomerEntity::class, TransactionEntity::class,
        CategoryEntity::class, ItemEntity::class,
        WorkRecordEntity::class, WorkRecordItemEntity::class,
        ShoppingListEntity::class, ShoppingItemEntity::class,
        TaskEntity::class, MemoryNoteEntity::class,
        TechnicianProfileEntity::class, ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MadDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun catalogDao(): CatalogDao
    abstract fun workRecordDao(): WorkRecordDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun taskDao(): TaskDao
    abstract fun memoryDao(): MemoryDao
    abstract fun technicianDao(): TechnicianDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: MadDatabase? = null

        fun getDatabase(context: android.content.Context): MadDatabase =
            INSTANCE ?: synchronized(this) {
                androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    MadDatabase::class.java,
                    "mad_assistant_database"
                ).build().also { INSTANCE = it }
            }
    }
}
