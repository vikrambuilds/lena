package com.vikram.lena.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    val timestamp: String,
    val sender: String,
    val message: String,
    val aiModel: String = "",
    val taskType: String = "",       // NEW: What task was performed
    val responseTime: String = "",   // NEW: AI response time
    val mood: String = ""            // NEW: Detected mood
)

class ConversationManager(private val context: Context) {

    private val csvFileName = "lena_conversations.csv"
    private val conversationDir: File
        get() {
            val dir = File(context.filesDir, "conversations")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val csvFile: File
        get() = File(conversationDir, csvFileName)

    init {
        if (!csvFile.exists()) initializeCSV()
    }

    private fun initializeCSV() {
        try {
            val writer = CSVWriter(FileWriter(csvFile))
            writer.writeNext(arrayOf(
                "Timestamp", "Sender", "Message", "AI_Model",
                "Task_Type", "Response_Time_ms", "Mood"
            ))
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Save message with all metadata */
    fun saveMessage(
        sender: String,
        message: String,
        aiModel: String = "",
        taskType: String = "",
        responseTime: Long = 0L,
        mood: String = ""
    ) {
        try {
            val timestamp = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
            ).format(Date())

            val writer = CSVWriter(FileWriter(csvFile, true))
            writer.writeNext(arrayOf(
                timestamp, sender, message, aiModel,
                taskType, responseTime.toString(), mood
            ))
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Get all messages */
    fun getAllMessages(): List<Message> {
        val messages = mutableListOf<Message>()
        try {
            if (!csvFile.exists()) return messages
            val reader = CSVReader(FileReader(csvFile))
            val allRows = reader.readAll()
            reader.close()

            for (i in 1 until allRows.size) {
                val row = allRows[i]
                if (row.size >= 3) {
                    messages.add(Message(
                        timestamp = row.getOrElse(0) { "" },
                        sender = row.getOrElse(1) { "" },
                        message = row.getOrElse(2) { "" },
                        aiModel = row.getOrElse(3) { "" },
                        taskType = row.getOrElse(4) { "" },
                        responseTime = row.getOrElse(5) { "" },
                        mood = row.getOrElse(6) { "" }
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return messages
    }

    fun getRecentMessages(count: Int = 20): List<Message> {
        val all = getAllMessages()
        return if (all.size <= count) all else all.takeLast(count)
    }

    /** Export to Downloads with analytics */
    fun exportToDownloads(): Pair<Boolean, String> {
        return try {
            val downloadsDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                ),
                "Lena"
            )
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val timestamp = SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.getDefault()
            ).format(Date())

            // Main conversation CSV
            val exportFile = File(downloadsDir, "lena_chat_$timestamp.csv")
            csvFile.copyTo(exportFile, overwrite = true)

            // Analytics CSV (BONUS!)
            val analyticsFile = File(downloadsDir, "lena_analytics_$timestamp.csv")
            generateAnalytics(analyticsFile)

            Pair(true, exportFile.absolutePath)
        } catch (e: Exception) {
            Pair(false, e.message ?: "Unknown error")
        }
    }

    /** Generate conversation analytics */
    private fun generateAnalytics(file: File) {
        val messages = getAllMessages()
        val writer = CSVWriter(FileWriter(file))
        
        writer.writeNext(arrayOf("=== LENA CONVERSATION ANALYTICS ===", "", ""))
        writer.writeNext(arrayOf("Metric", "Value", "Details"))
        writer.writeNext(arrayOf(
            "Total Messages",
            messages.size.toString(),
            ""
        ))
        writer.writeNext(arrayOf(
            "Vikram's Messages",
            messages.count { it.sender == "Vikram" }.toString(),
            ""
        ))
        writer.writeNext(arrayOf(
            "Lena's Messages",
            messages.count { it.sender == "Lena" }.toString(),
            ""
        ))
        writer.writeNext(arrayOf(
            "Gemini Used",
            messages.count { it.aiModel == "Gemini" }.toString(),
            ""
        ))
        writer.writeNext(arrayOf(
            "ChatGPT Used",
            messages.count { it.aiModel == "ChatGPT" }.toString(),
            ""
        ))
        
        // Task breakdown
        val taskCounts = messages
            .filter { it.taskType.isNotEmpty() }
            .groupBy { it.taskType }
            .map { (task, msgs) -> task to msgs.size }
        
        writer.writeNext(arrayOf("", "", ""))
        writer.writeNext(arrayOf("=== TASK BREAKDOWN ===", "", ""))
        taskCounts.forEach { (task, count) ->
            writer.writeNext(arrayOf(task, count.toString(), ""))
        }

        writer.close()
    }

    fun getCSVFileUri(): Uri? {
        return try {
            if (csvFile.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )
            } else null
        } catch (e: Exception) { null }
    }

    fun shareCSV() {
        val uri = getCSVFileUri() ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Lena Conversations - ${
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            }")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Conversations")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun deleteAllConversations(): Boolean {
        return try {
            if (csvFile.exists()) csvFile.delete()
            initializeCSV()
            true
        } catch (e: Exception) { false }
    }

    fun getMessageCount(): Int = getAllMessages().size

    fun getFileSize(): String {
        return if (csvFile.exists()) {
            val sizeInKB = csvFile.length() / 1024.0
            when {
                sizeInKB > 1024 -> String.format("%.2f MB", sizeInKB / 1024)
                else -> String.format("%.2f KB", sizeInKB)
            }
        } else "0 KB"
    }

    /** Get daily summary */
    fun getDailySummary(): String {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayMessages = getAllMessages().filter { it.timestamp.startsWith(today) }
        
        val vikramCount = todayMessages.count { it.sender == "Vikram" }
        val lenaCount = todayMessages.count { it.sender == "Lena" }
        val tasksCompleted = todayMessages.count { it.taskType.isNotEmpty() }
        
        return "Aaj ki summary 📊:\n" +
               "- Tu ${vikramCount} baar bola\n" +
               "- Maine ${lenaCount} baar jawab diya\n" +
               "- ${tasksCompleted} tasks complete kiye"
    }
}