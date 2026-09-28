package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.QuranAcademyDatabase
import com.example.data.local.db.entity.MessageEntity
import com.example.data.local.db.entity.QuranClassEntity
import com.example.data.model.ClassStatus
import com.example.data.model.Message
import com.example.data.model.QuranClass
import com.example.data.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseTest {

    private lateinit var db: QuranAcademyDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, QuranAcademyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testQuranClassInsertAndRetrieve() = runBlocking {
        val quranClass = QuranClass(
            id = "class_test_101",
            title = "Surah Al-Mulk Tajweed Practice",
            teacherName = "Sheikh Abdullah Al-Mansoor",
            teacherTitle = "Senior Qari",
            studentName = "Zaid Ahmed",
            date = "Today",
            startTime = "05:00 PM",
            durationMinutes = 45,
            status = ClassStatus.SCHEDULED,
            description = "Tajweed rules recitation",
            liveKitRoomName = "quran-room-test",
            surahTopic = "Surah Al-Mulk",
            syllabusNotes = "Makharij practice",
            isNextClass = true
        )

        val entity = QuranClassEntity.fromDomain(quranClass)
        db.quranClassDao().insertClass(entity)

        val retrieved = db.quranClassDao().getClassById("class_test_101")
        assertNotNull(retrieved)
        assertEquals("class_test_101", retrieved?.id)
        assertEquals("Surah Al-Mulk Tajweed Practice", retrieved?.title)

        val domainObject = retrieved?.toDomain()
        assertNotNull(domainObject)
        assertEquals(ClassStatus.SCHEDULED, domainObject?.status)
        assertEquals(true, domainObject?.isNextClass)
    }

    @Test
    fun testMessageInsertAndRetrieve() = runBlocking {
        val message = Message(
            id = "msg_test_001",
            senderId = "teacher_01",
            senderName = "Sheikh Abdullah",
            senderRole = UserRole.TEACHER,
            receiverId = "student_01",
            receiverName = "Zaid Ahmed",
            text = "Assalamu Alaikum Zaid, are you ready for recitation?",
            timestamp = "05:02 PM",
            isRead = true,
            isFromMe = false
        )

        val entity = MessageEntity.fromDomain(message)
        db.messageDao().insertMessage(entity)

        val messages = db.messageDao().getAllMessages()
        assertEquals(1, messages.size)
        assertEquals("Assalamu Alaikum Zaid, are you ready for recitation?", messages[0].text)
        assertEquals(UserRole.TEACHER, messages[0].toDomain().senderRole)
    }

    @Test
    fun testClearClassesAndMessages() = runBlocking {
        val classEntity = QuranClassEntity(
            id = "class_c1",
            title = "Test Class",
            teacherName = "Teacher",
            teacherTitle = "Title",
            studentName = "Student",
            date = "Today",
            startTime = "04:00 PM",
            durationMinutes = 30,
            status = "SCHEDULED",
            description = "Desc",
            liveKitRoomName = "room",
            surahTopic = "Topic",
            syllabusNotes = "Notes",
            isNextClass = false
        )
        db.quranClassDao().insertClass(classEntity)
        assertEquals(1, db.quranClassDao().getAllClasses().size)

        db.quranClassDao().clearAllClasses()
        assertEquals(0, db.quranClassDao().getAllClasses().size)
    }
}
