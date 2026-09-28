package com.example.livekit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveKitTokenServiceTest {

    @Test
    fun testGenerateSandboxTokenStructure() = runBlocking {
        val user = UserProfile(
            id = "test_student_42",
            name = "Ahmad Tariq",
            email = "ahmad@example.com",
            role = UserRole.STUDENT,
            avatarUrl = ""
        )

        val result = LiveKitTokenService.generateToken(
            roomName = "test-livekit-room-101",
            userProfile = user
        )

        assertTrue("Token generation should succeed", result.isSuccess)
        val token = result.getOrNull()
        assertNotNull(token)

        // Verify valid JWT format (header.payload.signature)
        val parts = token!!.split(".")
        assertEquals("JWT should contain 3 parts", 3, parts.size)

        // Decode Header
        val headerJson = String(Base64.getUrlDecoder().decode(parts[0]))
        val header = JSONObject(headerJson)
        assertEquals("HS256", header.getString("alg"))
        assertEquals("JWT", header.getString("typ"))

        // Decode Payload
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))
        val payload = JSONObject(payloadJson)
        assertEquals("test_student_42", payload.getString("sub"))
        assertEquals("Ahmad Tariq", payload.getString("name"))

        val videoGrants = payload.getJSONObject("video")
        assertEquals("test-livekit-room-101", videoGrants.getString("room"))
        assertTrue("roomJoin must be true", videoGrants.getBoolean("roomJoin"))
        assertTrue("canPublish must be true", videoGrants.getBoolean("canPublish"))
        assertTrue("canSubscribe must be true", videoGrants.getBoolean("canSubscribe"))
    }
}
