package com.example.data.local

import android.content.Context
import com.example.data.local.db.QuranAcademyDatabase
import com.example.data.local.db.entity.MessageEntity
import com.example.data.local.db.entity.QuranClassEntity
import com.example.data.model.ClassStatus
import com.example.data.model.Message
import com.example.data.model.QuranClass
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Offline persistent storage cache for Quran Academy Connect.
 * Upgraded to Full SQLite/Room DB architecture with JSON/memory fallback for instantaneous warm boot.
 */
object OfflineDataCache {

    private const val CACHE_DIR = "offline_db"
    private const val CLASSES_FILE = "cached_classes.json"
    private const val MESSAGES_FILE = "cached_messages.json"

    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    var database: QuranAcademyDatabase? = null
        private set

    fun initialize(context: Context) {
        val app = context.applicationContext
        appContext = app
        database = QuranAcademyDatabase.getInstance(app)
    }

    private fun getFile(filename: String): File? {
        val ctx = appContext ?: return null
        val dir = File(ctx.filesDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, filename)
    }

    fun getAllClassesFlow(): Flow<List<QuranClass>>? {
        return database?.quranClassDao()?.getAllClassesFlow()?.map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAllMessagesFlow(): Flow<List<Message>>? {
        return database?.messageDao()?.getAllMessagesFlow()?.map { list ->
            list.map { it.toDomain() }
        }
    }

    fun saveClasses(classes: List<QuranClass>) {
        // 1. Persist to Room SQLite Database
        val db = database
        if (db != null) {
            scope.launch {
                try {
                    val entities = classes.map { QuranClassEntity.fromDomain(it) }
                    db.quranClassDao().insertClasses(entities)
                } catch (e: Exception) {
                    android.util.Log.w("OfflineDataCache", "Room insertClasses error: ${e.message}")
                }
            }
        }

        // 2. Persist to instant warm-boot JSON file cache
        try {
            val file = getFile(CLASSES_FILE) ?: return
            val jsonArray = JSONArray()
            for (c in classes) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("title", c.title)
                    put("teacherName", c.teacherName)
                    put("teacherTitle", c.teacherTitle)
                    put("studentName", c.studentName)
                    put("date", c.date)
                    put("startTime", c.startTime)
                    put("durationMinutes", c.durationMinutes)
                    put("status", c.status.name)
                    put("description", c.description)
                    put("liveKitRoomName", c.liveKitRoomName)
                    put("surahTopic", c.surahTopic)
                    put("syllabusNotes", c.syllabusNotes)
                    put("isNextClass", c.isNextClass)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (_: Exception) {}
    }

    fun loadClasses(): List<QuranClass>? {
        // Try Room SQLite DB first if initialized
        val db = database
        if (db != null) {
            try {
                val dbClasses = runBlocking(Dispatchers.IO) {
                    db.quranClassDao().getAllClasses().map { it.toDomain() }
                }
                if (dbClasses.isNotEmpty()) return dbClasses
            } catch (_: Exception) {}
        }

        // Fallback to warm-boot file cache
        return try {
            val file = getFile(CLASSES_FILE) ?: return null
            if (!file.exists()) return null
            val text = file.readText()
            if (text.isBlank()) return null
            val array = JSONArray(text)
            val list = mutableListOf<QuranClass>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val statusStr = obj.optString("status", "SCHEDULED")
                val status = try { ClassStatus.valueOf(statusStr) } catch (_: Exception) { ClassStatus.SCHEDULED }
                list.add(
                    QuranClass(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        teacherName = obj.optString("teacherName", ""),
                        teacherTitle = obj.optString("teacherTitle", "Certified Qari & Hifz Instructor"),
                        studentName = obj.optString("studentName", ""),
                        date = obj.optString("date", "Today"),
                        startTime = obj.optString("startTime", "04:00 PM"),
                        durationMinutes = obj.optInt("durationMinutes", 45),
                        status = status,
                        description = obj.optString("description", ""),
                        liveKitRoomName = obj.optString("liveKitRoomName", ""),
                        surahTopic = obj.optString("surahTopic", "Surah Al-Mulk (Ayah 1-15)"),
                        syllabusNotes = obj.optString("syllabusNotes", "Makharij Al-Huroof and Ghunnah rules recitation practice."),
                        isNextClass = obj.optBoolean("isNextClass", false)
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (_: Exception) {
            null
        }
    }

    fun saveMessages(messages: List<Message>) {
        // 1. Persist to Room SQLite Database
        val db = database
        if (db != null) {
            scope.launch {
                try {
                    val entities = messages.map { MessageEntity.fromDomain(it) }
                    db.messageDao().insertMessages(entities)
                } catch (e: Exception) {
                    android.util.Log.w("OfflineDataCache", "Room insertMessages error: ${e.message}")
                }
            }
        }

        // 2. Persist to warm-boot file cache
        try {
            val file = getFile(MESSAGES_FILE) ?: return
            val jsonArray = JSONArray()
            for (m in messages) {
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("senderId", m.senderId)
                    put("senderName", m.senderName)
                    put("senderRole", m.senderRole.name)
                    put("receiverId", m.receiverId)
                    put("receiverName", m.receiverName)
                    put("text", m.text)
                    put("timestamp", m.timestamp)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (_: Exception) {}
    }

    fun loadMessages(): List<Message>? {
        // Try Room SQLite DB first
        val db = database
        if (db != null) {
            try {
                val dbMessages = runBlocking(Dispatchers.IO) {
                    db.messageDao().getAllMessages().map { it.toDomain() }
                }
                if (dbMessages.isNotEmpty()) return dbMessages
            } catch (_: Exception) {}
        }

        // Fallback to warm-boot file cache
        return try {
            val file = getFile(MESSAGES_FILE) ?: return null
            if (!file.exists()) return null
            val text = file.readText()
            if (text.isBlank()) return null
            val array = JSONArray(text)
            val list = mutableListOf<Message>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val roleStr = obj.optString("senderRole", "STUDENT")
                val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.STUDENT }
                list.add(
                    Message(
                        id = obj.optString("id", ""),
                        senderId = obj.optString("senderId", ""),
                        senderName = obj.optString("senderName", ""),
                        senderRole = role,
                        receiverId = obj.optString("receiverId", ""),
                        receiverName = obj.optString("receiverName", ""),
                        text = obj.optString("text", ""),
                        timestamp = obj.optString("timestamp", ""),
                        isRead = true,
                        isFromMe = false
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (_: Exception) {
            null
        }
    }
}
