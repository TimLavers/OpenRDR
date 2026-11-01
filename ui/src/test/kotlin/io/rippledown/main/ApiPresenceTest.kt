package io.rippledown.main

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.constants.server.USER_ID_HEADER
import io.rippledown.mocks.config
import io.rippledown.mocks.mock
import io.rippledown.model.PresentUser
import io.rippledown.model.UserId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ApiPresenceTest {
    @Test
    fun `presence requests the selected KB and preserves user identities and editing flags`() = runTest {
        // Given
        val users = listOf(PresentUser("Alice", false), PresentUser("Bob", true))
        val config = config { returnPresence = users }
        val api = Api(mock(config), userId = UserId("Alice"))
        try {
            val kb = api.createKB("Clinic")
            config.expectedKbId = kb.id

            // When
            val result = api.presence()

            // Then
            result shouldBe users
            config.lastRequestHeaders?.get(USER_ID_HEADER) shouldBe "Alice"
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `presence can be empty`() = runTest {
        // Given
        val api = Api(mock(config {}))
        try {
            api.createKB("Clinic")

            // When
            val result = api.presence()

            // Then
            result shouldBe emptyList()
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `presence requires an open KB`() = runTest {
        // Given
        val api = Api(mock(config {}))
        try {
            // When
            val failure = shouldThrow<IllegalStateException> { api.presence() }

            // Then
            failure.message shouldBe "No knowledge base is open."
        } finally {
            api.client.close()
        }
    }
}
