package com.example.data.local

import android.content.Context
import com.example.data.model.ClassStatus
import com.example.data.model.Message
import com.example.data.model.QuranClass
import com.example.data.model.UserRole
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Offline persistent storage cache for Quran Academy Connect.
 * Provides Single Source of Truth (SSOT) disk fallback when network is unavailable.
 */
object OfflineDataCache {

    private const val CACHE_DIR = "offline_db"
    private const val CLASSES_FILE = "cached_classes.json"
    private const val MESSAGES_FILE = "cached_messages.json"

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun getFile(filename: String): File? {
        val ctx = appContext ?: return null
        val dir = File(ctx.filesDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, filename)
    }

    fun saveClasses(classes: List<QuranClass>) {
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
