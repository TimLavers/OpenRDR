package io.rippledown.main

import io.kotest.matchers.shouldBe
import io.rippledown.constants.server.LOCAL_USER_ID
import io.rippledown.model.UserId
import kotlin.test.Test

class DefaultUserIdTest {

    @Test
    fun `the system property wins when set`() {
        // Given
        val properties = mapOf(USER_ID_PROPERTY to "alice", "user.name" to "os-user")

        // When / Then
        defaultUserId(properties::get) shouldBe UserId("alice")
    }

    @Test
    fun `falls back to the OS user name`() {
        // Given
        val properties = mapOf("user.name" to "os-user")

        // When / Then
        defaultUserId(properties::get) shouldBe UserId("os-user")
    }

    @Test
    fun `a blank property is ignored`() {
        // Given
        val properties = mapOf(USER_ID_PROPERTY to "  ", "user.name" to "os-user")

        // When / Then
        defaultUserId(properties::get) shouldBe UserId("os-user")
    }

    @Test
    fun `falls back to the local id when nothing is set`() {
        // Given
        val properties = emptyMap<String, String>()

        // When / Then
        defaultUserId(properties::get) shouldBe UserId(LOCAL_USER_ID)
    }

    @Test
    fun `the value is trimmed`() {
        // Given
        val properties = mapOf(USER_ID_PROPERTY to " alice ")

        // When / Then
        defaultUserId(properties::get) shouldBe UserId("alice")
    }
}
