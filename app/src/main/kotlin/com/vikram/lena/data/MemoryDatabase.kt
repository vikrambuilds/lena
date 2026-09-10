package com.vikram.lena.data

import android.content.Context
import androidx.room.*
import java.text.SimpleDateFormat
import java.util.*

// ========== Entity ==========

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "topic") val topic: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "category") val category: String = "general",
    @ColumnInfo(name = "importance") val importance: Int = 5,
    @ColumnInfo(name = "timestamp") val timestamp: String = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
    ).format(Date()),
    @ColumnInfo(name = "last_accessed") val lastAccessed: String = ""
)

// ========== DAO ==========

@Dao
interface MemoryDao {

    @Insert
    suspend fun insertMemory(memory: Memory): Long

    @Update
    suspend fun updateMemory(memory: Memory)

    @Delete
    suspend fun deleteMemory(memory: Memory)

    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    suspend fun getAllMemories(): List<Memory>

    @Query("SELECT * FROM memories WHERE topic LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY importance DESC LIMIT :limit")
    suspend fun searchMemories(query: String, limit: Int = 10): List<Memory>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY timestamp DESC")
    suspend fun getMemoriesByCategory(category: String): List<Memory>

    @Query("SELECT * FROM memories ORDER BY importance DESC LIMIT :limit")
    suspend fun getImportantMemories(limit: Int = 10): List<Memory>

    @Query("SELECT * FROM memories ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int = 20): List<Memory>

    @Query("DELETE FROM memories")
    suspend fun deleteAllMemories()

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getMemoryCount(): Int

    @Query("DELETE FROM memories WHERE importance < 3 AND timestamp < :olderThan")
    suspend fun cleanOldLowPriorityMemories(olderThan: String)
}

// ========== Database ==========

@Database(entities = [Memory::class], version = 1, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getDatabase(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "lena_memory_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ========== Memory Manager Helper ==========

class MemoryManager(context: Context) {

    private val database = MemoryDatabase.getDatabase(context)
    private val dao = database.memoryDao()

    /** Save a new memory */
    suspend fun remember(
        topic: String,
        content: String,
        category: String = "general",
        importance: Int = 5
    ) {
        dao.insertMemory(
            Memory(
                topic = topic,
                content = content,
                category = category,
                importance = importance.coerceIn(1, 10)
            )
        )
    }

    /** Search memories relevant to a query */
    suspend fun recall(query: String): List<Memory> {
        return dao.searchMemories(query)
    }

    /** Get context for AI prompt */
    suspend fun getContextForPrompt(userMessage: String): String {
        val relevant = recall(userMessage)
        val recent = dao.getRecentMemories(5)
        val important = dao.getImportantMemories(3)

        val allMemories = (relevant + recent + important)
            .distinctBy { it.id }
            .take(10)

        if (allMemories.isEmpty()) return ""

        return "VIKRAM KI YAADEIN (Memories):\n" +
                allMemories.joinToString("\n") { memory ->
                    "- [${memory.category}] ${memory.content} (${memory.timestamp.take(10)})"
                }
    }

    /** Auto-detect and save important info from conversation */
    suspend fun autoSaveFromMessage(userMessage: String) {
        val msg = userMessage.lowercase()

        // Detect exam/assignment mentions
        if (msg.contains("exam") || msg.contains("test") || msg.contains("paper")) {
            remember(
                topic = "exam",
                content = "Vikram ne bola: $userMessage",
                category = "academics",
                importance = 8
            )
        }

        // Detect project mentions
        if (msg.contains("project") || msg.contains("assignment") || 
            msg.contains("homework")) {
            remember(
                topic = "project",
                content = "Vikram ne bola: $userMessage",
                category = "academics",
                importance = 7
            )
        }

        // Detect personal info
        if (msg.contains("birthday") || msg.contains("janamdin")) {
            remember(
                topic = "birthday",
                content = userMessage,
                category = "personal",
                importance = 9
            )
        }

        // Detect mood
        if (msg.contains("sad") || msg.contains("dukhi") || msg.contains("upset") ||
            msg.contains("tension") || msg.contains("stress")) {
            remember(
                topic = "mood",
                content = "Vikram was feeling low: $userMessage",
                category = "mood",
                importance = 6
            )
        }
    }

    suspend fun getMemoryCount(): Int = dao.getMemoryCount()

    suspend fun clearAll() = dao.deleteAllMemories()
}