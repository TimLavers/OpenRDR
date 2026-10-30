package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.KBInfo
import io.rippledown.model.PresentUser
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ListUsersPresentTest {
    private val service = mockk<KnowledgeBaseService>()

    @Test
    fun `lists the other users and identifies those building rules`() = runTest {
        // Given
        every { service.openKnowledgeBase() } returns KBInfo("clinic", "Clinic")
        every { service.usersPresent() } returns listOf(PresentUser("Bob", true), PresentUser("Carol", false))

        // When
        val outcome = ListUsersPresent().doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe
                "Also working on Clinic: Bob (building a rule), Carol."
    }

    @Test
    fun `reports when nobody else is working on the KB`() = runTest {
        // Given
        every { service.openKnowledgeBase() } returns KBInfo("clinic", "Clinic")
        every { service.usersPresent() } returns emptyList()

        // When
        val outcome = ListUsersPresent().doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe
                "Nobody else is working on Clinic right now."
    }

    @Test
    fun `without an open KB presence is not queried`() = runTest {
        // Given
        every { service.openKnowledgeBase() } returns null

        // When
        val outcome = ListUsersPresent().doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe NO_KB_OPEN_MESSAGE
        verify(exactly = 0) { service.usersPresent() }
    }
}
