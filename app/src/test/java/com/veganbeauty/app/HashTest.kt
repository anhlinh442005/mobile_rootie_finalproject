package com.veganbeauty.app

import org.junit.Test
import org.junit.Assert.assertEquals
import java.security.MessageDigest

class HashTest {
    @Test
    fun testHash() {
        val password = "password123"
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        val hash = bytes.joinToString("") { "%02x".format(it) }
        println("Hash is: $hash")
        assertEquals("ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f", hash)
    }
}
