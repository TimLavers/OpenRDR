package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.*
import io.rippledown.chat.ConversationService
import io.rippledown.model.KBInfo
import io.rippledown.model.PresentUser
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import io.rippledown.utils.createCaseWithInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AwarenessChatTest {
    @Test
    fun `both awareness actions run with an open KB and no case or rule service`() = runTest {
        // Given
        val conversation = mockk<ConversationService>()
        val kbService = mockk<KnowledgeBaseService>()
        every { kbService.openKnowledgeBase() } returns KBInfo("clinic", "Clinic")
        every { kbService.recentChanges(2, "BMI") } returns listOf(
            ChangeRecord(1, "Bob", 0, ChangeKind.DEFINITION_EDITED, "Changed BMI.")
        )
        every { kbService.usersPresent() } returns listOf(PresentUser("Bob", true))
        coEvery { conversation.response("Who changed BMI?") } returns
                """{"action":"ListRecentChanges","limit":2,"attribute":"BMI"}"""
        coEvery { conversation.response("Who else is here?") } returns """{"action":"ListUsersPresent"}"""
        val manager = ChatManager(conversation, null, kbService)

        // When
        val changes = manager.response("Who changed BMI?")
        val users = manager.response("Who else is here?")

        // Then
        changes.text shouldContain "Bob: Changed BMI."
        users.text shouldBe "Also working on Clinic: Bob (building a rule)."
        verify(exactly = 1) { kbService.recentChanges(2, "BMI") }
        verify(exactly = 1) { kbService.usersPresent() }
    }

    @Test
    fun `awareness actions remain available during rule building`() = runTest {
        // Given
        val kbService = mockk<KnowledgeBaseService>()
        val ruleService = mockk<RuleService>()
        every { kbService.openKnowledgeBase() } returns KBInfo("clinic", "Clinic")
        every { kbService.recentChanges(10, null) } returns emptyList()
        every { kbService.usersPresent() } returns emptyList()
        every { ruleService.isRuleSessionActive() } returns true
        val manager = ChatManager(mockk(), ruleService, kbService)

        // When
        val changes = manager.processActionComment(ActionComment("ListRecentChanges"))
        val users = manager.processActionComment(ActionComment("ListUsersPresent"))

        // Then
        changes.text shouldBe "No changes have been recorded for this knowledge base."
        users.text shouldBe "Nobody else is working on Clinic right now."
        coVerify(exactly = 0) { ruleService.cancelCurrentRuleSession() }
        coVerify(exactly = 0) { ruleService.commitCurrentRuleSession() }
    }

    @Test
    fun `awareness instructions are present with and without a case`() {
        // Given
        val cases = listOf(null, createCaseWithInterpretation("Case"))

        cases.forEach { case ->
            // When
            val prompt = KBChatService.systemPrompt(case, kbName = "Clinic")

            // Then
            prompt shouldContain "\"action\": \"ListRecentChanges\""
            prompt shouldContain "\"action\": \"ListUsersPresent\""
            prompt shouldContain "\"limit\""
            prompt shouldContain "\"attribute\""
            prompt shouldContain "who changed"
            prompt shouldContain "who else is here"
            prompt shouldNotContain "{{"
        }
    }
}
