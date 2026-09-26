package com.example.data.backend

import android.util.Log
import com.example.data.model.ClassStatus
import com.example.data.model.Message
import com.example.data.model.QuranClass
import com.example.data.model.StudentInfo
import com.example.data.model.TeacherInfo
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AcademyBackendService {

    companion object {
        private const val TAG = "AcademyBackend"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    // Status tracking for UI indicators
    var isSupabaseReachable: Boolean = false
        private set
    var lastSyncTimestamp: Long = 0
        private set
    var syncErrorMessage: String? = null
        private set

    // Supabase Realtime WebSocket engine
    private var realtimeWebSocket: WebSocket? = null
    private var realtimeHeartbeatJob: Job? = null
    private val realtimeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Instant Realtime callbacks
    var onChatMessageReceived: ((Message) -> Unit)? = null
    var onVideoFrameReceived: ((classId: String, role: String, base64Frame: String) -> Unit)? = null

    /**
     * Subscribe directly to Supabase Postgres Changes WebSocket (realtime.channel).
     * Replaces aggressive HTTP polling with zero-latency push events (<50ms).
     */
    fun subscribeToRealtimeActiveCalls(onCallChanged: (ActiveCallInfo) -> Unit) {
        try {
            realtimeWebSocket?.close(1000, "Reconnecting")
            realtimeHeartbeatJob?.cancel()

            val baseWs = SupabaseConfig.projectUrl
                .replace("https://", "wss://")
                .replace("http://", "ws://")
            val wsUrl = "$baseWs/realtime/v1/websocket?apikey=${SupabaseConfig.anonKey}&vsn=1.0.0"

            val request = Request.Builder().url(wsUrl).build()
            realtimeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.i(TAG, "Supabase Realtime WebSocket connected successfully")
                    // Join active_calls postgres_changes topic
                    val joinMessage = JSONObject().apply {
                        put("topic", "realtime:public:active_calls")
                        put("event", "phx_join")
                        put("payload", JSONObject().apply {
                            put("config", JSONObject().apply {
                                put("postgres_changes", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("event", "*")
                                        put("schema", "public")
                                        put("table", "active_calls")
                                    })
                                })
                            })
                        })
                        put("ref", "1")
                    }
                    webSocket.send(joinMessage.toString())

                    // Start heartbeat every 25s
                    realtimeHeartbeatJob = realtimeScope.launch {
                        var hbRef = 0
                        while (isActive) {
                            delay(25000)
                            val hb = JSONObject().apply {
                                put("topic", "phoenix")
                                put("event", "heartbeat")
                                put("payload", JSONObject())
                                put("ref", "hb_${++hbRef}")
                            }
                            webSocket.send(hb.toString())
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val event = json.optString("event")
                        if (event == "postgres_changes") {
                            val payload = json.optJSONObject("payload")
                            val data = payload?.optJSONObject("data")
                            val record = data?.optJSONObject("record")
                            if (record != null) {
                                val isRinging = record.optBoolean("is_ringing", false)
                                val classId = record.optString("class_id", "")
                                val teacherName = record.optString("teacher_name", "Quran Teacher")
                                val studentName = record.optString("student_name", "")
                                val roomName = record.optString("room_name", "room_$classId")

                                if (classId == "chat") {
                                    // Parse instant real-time chat message (<50ms delivery)
                                    val id = record.optString("id", "msg_${System.currentTimeMillis()}")
                                    var textMsg = roomName
                                    var senderId = ""
                                    var senderName = teacherName
                                    var senderRole = UserRole.STUDENT
                                    var receiverId = ""
                                    var receiverName = studentName
                                    var timestamp = "Now"

                                    try {
                                        if (roomName.startsWith("{")) {
                                            val p = JSONObject(roomName)
                                            textMsg = p.optString("text", textMsg)
                                            senderId = p.optString("senderId", "")
                                            senderName = p.optString("senderName", teacherName)
                                            val roleStr = p.optString("senderRole", "STUDENT")
                                            senderRole = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.STUDENT }
                                            receiverId = p.optString("receiverId", "")
                                            receiverName = p.optString("receiverName", studentName)
                                            timestamp = p.optString("timestamp", timestamp)
                                        }
                                    } catch (_: Exception) {}

                                    val chatMsg = Message(
                                        id = id,
                                        senderId = senderId,
                                        senderName = senderName,
                                        senderRole = senderRole,
                                        receiverId = receiverId,
                                        receiverName = receiverName,
                                        text = textMsg,
                                        timestamp = timestamp,
                                        isRead = true,
                                        isFromMe = false
                                    )
                                    onChatMessageReceived?.invoke(chatMsg)
                                    return
                                }

                                if (classId.startsWith("frame_")) {
                                    // Instant real-time video frame relay
                                    val base64Frame = roomName
                                    if (base64Frame.isNotBlank()) {
                                        onVideoFrameReceived?.invoke(classId, teacherName, base64Frame)
                                    }
                                    return
                                }

                                if (classId.isNotBlank()) {
                                    onCallChanged(
                                        ActiveCallInfo(
                                            classId = classId,
                                            teacherName = teacherName,
                                            studentName = studentName,
                                            roomName = roomName,
                                            isRinging = isRinging
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Realtime message parse exception: ${e.message}")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Supabase Realtime socket error: ${t.message}")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i(TAG, "Supabase Realtime socket closed: code=$code, reason=$reason")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Supabase Realtime WebSocket: ${e.message}")
        }
    }

    /**
     * Upsert user profile to Supabase backend
     */
    suspend fun syncUserProfile(user: UserProfile): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/profiles"
            val json = JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("email", user.email)
                put("role", user.role.name)
                put("avatar_url", user.avatarUrl)
                put("assigned_teacher_name", user.assignedTeacherName)
                put("tajweed_level", user.tajweedLevel)
            }

            val body = json.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                isSupabaseReachable = response.isSuccessful
                lastSyncTimestamp = System.currentTimeMillis()
                if (response.isSuccessful || response.code == 201 || response.code == 200 || response.code == 204) {
                    Result.success(true)
                } else {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    syncErrorMessage = "Profile sync: $err"
                    Log.w(TAG, "Sync user profile notice: $err")
                    Result.failure(Exception(err))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sync user profile error: ${e.message}")
            syncErrorMessage = e.message
            Result.failure(e)
        }
    }

    /**
     * Fetch all registered students and teachers from backend
     */
    suspend fun fetchProfiles(): Result<Pair<List<StudentInfo>, List<TeacherInfo>>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/profiles?select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                isSupabaseReachable = response.isSuccessful
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(raw)
                    val students = mutableListOf<StudentInfo>()
                    val teachers = mutableListOf<TeacherInfo>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optString("id", "usr_$i")
                        val name = obj.optString("name", "User")
                        val email = obj.optString("email", "")
                        val roleStr = obj.optString("role", "STUDENT")
                        val teacherName = obj.optString("assigned_teacher_name", "")
                        val tajweed = obj.optString("tajweed_level", "Intermediate Tajweed")

                        if (roleStr.equals("TEACHER", ignoreCase = true)) {
                            teachers.add(
                                TeacherInfo(
                                    id = id,
                                    name = name,
                                    email = email,
                                    title = "Certified Quran & Tajweed Instructor",
                                    tajweedIjazah = "Certified Hafiz & Qari",
                                    assignedStudentCount = 0,
                                    availability = "Available for Live Classes"
                                )
                            )
                        } else if (roleStr.equals("STUDENT", ignoreCase = true)) {
                            students.add(
                                StudentInfo(
                                    id = id,
                                    name = name,
                                    assignedTeacherName = teacherName,
                                    tajweedLevel = tajweed,
                                    currentSurah = "Surah Al-Mulk",
                                    attendanceRate = "100%",
                                    email = email
                                )
                            )
                        }
                    }
                    // Calculate real assigned student count for teachers
                    val studentsByTeacher = students.groupBy { it.assignedTeacherName.trim().lowercase() }
                    val calculatedTeachers = teachers.map { t ->
                        val count = studentsByTeacher[t.name.trim().lowercase()]?.size ?: 0
                        t.copy(assignedStudentCount = count)
                    }

                    Result.success(Pair(students, calculatedTeachers))
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upsert a scheduled class to Supabase
     */
    suspend fun saveClass(qClass: QuranClass): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/classes"
            val json = JSONObject().apply {
                put("id", qClass.id)
                put("title", qClass.title)
                put("teacher_name", qClass.teacherName)
                put("teacher_title", qClass.teacherTitle)
                put("student_name", qClass.studentName)
                put("date", qClass.date)
                put("start_time", qClass.startTime)
                put("duration_minutes", qClass.durationMinutes)
                put("status", qClass.status.name)
                put("description", qClass.description)
                put("livekit_room_name", qClass.liveKitRoomName)
                put("surah_topic", qClass.surahTopic)
            }

            val body = json.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..204) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch classes from Supabase
     */
    suspend fun fetchClasses(): Result<List<QuranClass>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/classes?select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(raw)
                    val list = mutableListOf<QuranClass>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optString("id", "cls_$i")
                        val title = obj.optString("title", "Quran Reading")
                        val teacherName = obj.optString("teacher_name", "Teacher")
                        val teacherTitle = obj.optString("teacher_title", "Certified Qari")
                        val studentName = obj.optString("student_name", "Student")
                        val date = obj.optString("date", "Today")
                        val startTime = obj.optString("start_time", "6:00 PM")
                        val duration = obj.optInt("duration_minutes", 45)
                        val statusStr = obj.optString("status", "SCHEDULED")
                        val status = try { ClassStatus.valueOf(statusStr) } catch (e: Exception) { ClassStatus.SCHEDULED }
                        val desc = obj.optString("description", "")
                        val room = obj.optString("livekit_room_name", "room_$id")
                        val topic = obj.optString("surah_topic", "Tajweed Recitation")

                        list.add(
                            QuranClass(
                                id = id,
                                title = title,
                                teacherName = teacherName,
                                teacherTitle = teacherTitle,
                                studentName = studentName,
                                date = date,
                                startTime = startTime,
                                durationMinutes = duration,
                                status = status,
                                description = desc,
                                liveKitRoomName = room,
                                surahTopic = topic,
                                syllabusNotes = desc
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signal an incoming call: Teacher calls student -> writes to active_calls table
     */
    suspend fun triggerCallSignal(
        qClass: QuranClass,
        teacherName: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Also update the class status to LIVE_NOW in classes table
            val updateClassUrl = "${SupabaseConfig.projectUrl}/rest/v1/classes?id=eq.${qClass.id}"
            val updateBody = JSONObject().apply {
                put("status", ClassStatus.LIVE_NOW.name)
            }.toString().toRequestBody(JSON_MEDIA)

            val updateReq = Request.Builder()
                .url(updateClassUrl)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .patch(updateBody)
                .build()

            client.newCall(updateReq).execute().close()

            // Insert into active_calls table
            val activeCallUrl = "${SupabaseConfig.projectUrl}/rest/v1/active_calls"
            val callJson = JSONObject().apply {
                put("id", "call_${qClass.id}")
                put("class_id", qClass.id)
                put("teacher_name", teacherName)
                put("student_name", qClass.studentName)
                put("room_name", qClass.liveKitRoomName)
                put("is_ringing", true)
            }

            val body = callJson.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url(activeCallUrl)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            // Also dispatch high-priority FCM push to wake up device when app is closed / phone locked
            try {
                com.example.service.FcmNotificationSender.sendIncomingCallPush(
                    classId = qClass.id,
                    teacherName = teacherName,
                    studentName = qClass.studentName,
                    roomName = qClass.liveKitRoomName
                )
            } catch (fcmEx: Exception) {
                Log.w(TAG, "FCM Push dispatch error: ${fcmEx.message}")
            }

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code in 200..204)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop ringing / dismiss call signal
     */
    suspend fun dismissCallSignal(classId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Also dispatch FCM cancel push
            try {
                com.example.service.FcmNotificationSender.sendCancelCallPush(classId)
            } catch (fcmEx: Exception) {
                Log.w(TAG, "FCM Cancel dispatch error: ${fcmEx.message}")
            }

            val activeCallUrl = "${SupabaseConfig.projectUrl}/rest/v1/active_calls?class_id=eq.$classId"
            val callJson = JSONObject().apply {
                put("is_ringing", false)
            }

            val body = callJson.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url(activeCallUrl)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .patch(body)
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code in 200..204)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Poll for active incoming calls matching this student's name
     */
    suspend fun checkIncomingCallForStudent(studentName: String): Result<ActiveCallInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/active_calls?is_ringing=eq.true&order=updated_at.desc&limit=5"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(raw)
                    val cleanStudentName = studentName.trim().lowercase()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val classId = obj.optString("class_id", "")
                        val teacherName = obj.optString("teacher_name", "Quran Teacher")
                        val callStudent = obj.optString("student_name", "").trim().lowercase()
                        val roomName = obj.optString("room_name", "room_$classId")
                        val isRinging = obj.optBoolean("is_ringing", false)

                        val isMatch = isRinging && classId.isNotBlank() && (
                            cleanStudentName.isEmpty() ||
                            callStudent.isEmpty() ||
                            callStudent == cleanStudentName ||
                            callStudent.contains(cleanStudentName) ||
                            cleanStudentName.contains(callStudent)
                        )

                        if (isMatch) {
                            return@withContext Result.success(
                                ActiveCallInfo(
                                    classId = classId,
                                    teacherName = teacherName,
                                    studentName = obj.optString("student_name", studentName),
                                    roomName = roomName,
                                    isRinging = true
                                )
                            )
                        }
                    }
                }
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete student or teacher profile from Supabase
     */
    suspend fun deleteProfile(profileId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/profiles?id=eq.$profileId"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code in 200..204)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Delete profile error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete class from Supabase
     */
    suspend fun deleteClass(classId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Delete active_calls for this class first if any
            try {
                val activeCallsUrl = "${SupabaseConfig.projectUrl}/rest/v1/active_calls?class_id=eq.$classId"
                val delActiveCallReq = Request.Builder()
                    .url(activeCallsUrl)
                    .addHeader("apikey", SupabaseConfig.anonKey)
                    .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                    .delete()
                    .build()
                client.newCall(delActiveCallReq).execute().close()
            } catch (ignored: Exception) {}

            // Delete class from classes table
            val url = "${SupabaseConfig.projectUrl}/rest/v1/classes?id=eq.$classId"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code in 200..204)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Delete class error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Persist teacher assignment to student profile in Supabase
     */
    suspend fun updateStudentTeacherAssignment(studentId: String, teacherName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/profiles?id=eq.$studentId"
            val body = JSONObject().apply {
                put("assigned_teacher_name", teacherName)
            }.toString().toRequestBody(JSON_MEDIA)

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .patch(body)
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code in 200..204)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update assignment error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Send a chat message via Supabase active_calls storage
     */
    suspend fun sendChatMessage(msg: Message): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/active_calls"
            val payloadObj = JSONObject().apply {
                put("text", msg.text)
                put("senderId", msg.senderId)
                put("senderName", msg.senderName)
                put("senderRole", msg.senderRole.name)
                put("receiverId", msg.receiverId)
                put("receiverName", msg.receiverName)
                put("timestamp", msg.timestamp)
            }

            val json = JSONObject().apply {
                put("id", msg.id)
                put("class_id", "chat")
                put("teacher_name", msg.senderName)
                put("student_name", msg.receiverName.ifBlank { msg.receiverId })
                put("room_name", payloadObj.toString())
                put("is_ringing", false)
            }

            val body = json.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code in 200..204)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Send chat message error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetch all chat messages synchronized across devices
     */
    suspend fun fetchChatMessages(): Result<List<Message>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/active_calls?class_id=eq.chat&is_ringing=eq.false&order=updated_at.asc&limit=150"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(raw)
                    val messages = mutableListOf<Message>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optString("id", "msg_$i")
                        val teacherName = obj.optString("teacher_name", "")
                        val studentName = obj.optString("student_name", "")
                        val roomName = obj.optString("room_name", "")

                        // Try to parse structured JSON from room_name
                        var text = roomName
                        var senderId = ""
                        var senderName = teacherName
                        var senderRole = UserRole.STUDENT
                        var receiverId = ""
                        var receiverName = studentName
                        var timestamp = "Now"

                        try {
                            if (roomName.startsWith("{")) {
                                val p = JSONObject(roomName)
                                text = p.optString("text", text)
                                senderId = p.optString("senderId", "")
                                senderName = p.optString("senderName", teacherName)
                                val roleStr = p.optString("senderRole", "STUDENT")
                                senderRole = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.STUDENT }
                                receiverId = p.optString("receiverId", "")
                                receiverName = p.optString("receiverName", studentName)
                                timestamp = p.optString("timestamp", timestamp)
                            }
                        } catch (_: Exception) {}

                        messages.add(
                            Message(
                                id = id,
                                senderId = senderId,
                                senderName = senderName,
                                senderRole = senderRole,
                                receiverId = receiverId,
                                receiverName = receiverName,
                                text = text,
                                timestamp = timestamp,
                                isRead = true,
                                isFromMe = false
                            )
                        )
                    }
                    Result.success(messages)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Publish peer socket connection coordinates (IP:Port) for direct P2P streaming
     */
    suspend fun publishMediaSignal(classId: String, role: String, payload: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/active_calls"
            val json = JSONObject().apply {
                put("id", "sig_${classId}_${role.lowercase()}")
                put("class_id", "sig_${classId}")
                put("teacher_name", role)
                put("student_name", "SIGNAL")
                put("room_name", payload)
                put("is_ringing", false)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code in 200..204)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieve peer socket connection coordinates (IP:Port)
     */
    suspend fun fetchMediaSignal(classId: String, peerRole: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/active_calls?id=eq.sig_${classId}_${peerRole.lowercase()}&limit=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(raw)
                    if (jsonArray.length() > 0) {
                        val obj = jsonArray.getJSONObject(0)
                        return@withContext Result.success(obj.optString("room_name", null))
                    }
                }
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Publish live camera frame snapshot to cloud relay
     */
    suspend fun publishMediaFrame(
        classId: String,
        role: String,
        isCameraOn: Boolean,
        isMicMuted: Boolean,
        base64Frame: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/active_calls"
            val statusTag = "${if (isCameraOn) "CAM_ON" else "CAM_OFF"}|${if (isMicMuted) "MIC_MUTED" else "MIC_ON"}"
            val json = JSONObject().apply {
                put("id", "frame_${classId}_${role.lowercase()}")
                put("class_id", "frame_${classId}")
                put("teacher_name", role)
                put("student_name", statusTag)
                put("room_name", base64Frame)
                put("is_ringing", false)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code in 200..204)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch remote live camera frame snapshot from cloud relay
     */
    suspend fun fetchMediaFrame(classId: String, peerRole: String): Result<Triple<Boolean, Boolean, String>?> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/rest/v1/active_calls?id=eq.frame_${classId}_${peerRole.lowercase()}&limit=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(raw)
                    if (jsonArray.length() > 0) {
                        val obj = jsonArray.getJSONObject(0)
                        val statusTag = obj.optString("student_name", "CAM_ON|MIC_ON")
                        val isCameraOn = statusTag.contains("CAM_ON")
                        val isMicMuted = statusTag.contains("MIC_MUTED")
                        val frameData = obj.optString("room_name", "")
                        return@withContext Result.success(Triple(isCameraOn, isMicMuted, frameData))
                    }
                }
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ActiveCallInfo(
    val classId: String,
    val teacherName: String,
    val studentName: String,
    val roomName: String,
    val isRinging: Boolean
)
