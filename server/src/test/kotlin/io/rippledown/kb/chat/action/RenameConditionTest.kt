package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.kb.chat.ActionComment
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.chat.ChatResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class RenameConditionTest {
    private val service = mockk<RuleService>()
    private val responder = mockk<ModelResponder>()

    @Test
    fun `decodes the action and renames without a case or rule session`() = runTest {
        // Given
        val json = """{"action":"RenameCondition","conditionText":"Glucose is high","newPhrase":"raised glucose"}"""
        val action = Json.decodeFromString<ActionComment>(json).createActionInstance()
        every { service.renameCondition("Glucose is high", "raised glucose") } returns "Renamed."
        every { service.isRuleSessionActive() } returns false

        // When
        val response = (action as ChatAction).doIt(service, null, responder)

        // Then
        action shouldBe RenameCondition("Glucose is high", "raised glucose")
        response shouldBe ChatResponse("Renamed.")
        verify(exactly = 0) { service.sendCornerstoneStatus() }
        verify { responder wasNot Called }
    }

    @Test
    fun `refreshes cornerstone status during an active session`() = runTest {
        // Given
        every { service.renameCondition(any(), any()) } returns "Renamed."
        every { service.isRuleSessionActive() } returns true

        // When
        val response = RenameCondition("old", "new").doIt(service, null, responder)

        // Then
        response shouldBe ChatResponse("Renamed.")
        verify(exactly = 1) { service.sendCornerstoneStatus() }
        verify(exactly = 0) { service.commitCurrentRuleSession() }
        verify(exactly = 0) { service.cancelCurrentRuleSession() }
    }

    @Test
    fun `reports lookup and validation failures without refreshing status`() = runTest {
        // Given
        val errors = listOf(
            IllegalStateException("No condition exists."),
            IllegalArgumentException("A condition phrase cannot be blank."),
            IllegalStateException(),
            IllegalArgumentException()
        )
        errors.forEach { error ->
            every { service.renameCondition(any(), any()) } throws error

            // When
            val response = RenameCondition("old", "new").doIt(service, null, responder)

            // Then
            response shouldBe ChatResponse(error.message ?: "Could not rename the condition.")
        }
        verify(exactly = 0) { service.sendCornerstoneStatus() }
    }
}
