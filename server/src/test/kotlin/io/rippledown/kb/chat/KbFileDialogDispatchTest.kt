package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.ConversationService
import io.rippledown.constants.chat.*
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.KbFileDialogRequest
import io.rippledown.toJsonString
import io.rippledown.utils.createCaseWithInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class KbFileDialogDispatchTest {
    private val model = mockk<ConversationService>()
    private val service = mockk<KnowledgeBaseService>()
    private val rules = mockk<RuleService>()
    private val thyroids = KBInfo("thyroids_1", "Thyroids")

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `import is dispatched for an empty KB or one with a case`(hasCase: Boolean) = runTest {
        // Given
        every { service.openKnowledgeBase() } returns thyroids
        every { rules.isRuleSessionActive() } returns false
        every { rules.currentRuleSessionConditionTexts() } returns emptySet()
        coEvery { model.startConversation() } returns "Welcome"
        coEvery { model.response("Import a KB") } returns ActionComment(IMPORT_KNOWLEDGE_BASE).toJsonString()
        val manager = ChatManager(model, if (hasCase) rules else null, service)
        manager.startConversation(if (hasCase) createCaseWithInterpretation("Case1") else null)

        // When
        val response = manager.response("Import a KB")

        // Then
        response.kbFileDialogRequest.shouldBeInstanceOf<KbFileDialogRequest.Import>()
        response.text shouldBe "Choose a knowledge base ZIP file to import."
        coVerify(exactly = 0) { service.create(any()) }
        coVerify(exactly = 0) { service.open(any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `export is dispatched for an empty KB or one with a case`(hasCase: Boolean) = runTest {
        // Given
        every { service.openKnowledgeBase() } returns thyroids
        every { rules.isRuleSessionActive() } returns false
        every { rules.currentRuleSessionConditionTexts() } returns emptySet()
        coEvery { model.startConversation() } returns "Welcome"
        coEvery { model.response("Export this KB") } returns ActionComment(EXPORT_KNOWLEDGE_BASE).toJsonString()
        val manager = ChatManager(model, if (hasCase) rules else null, service)
        manager.startConversation(if (hasCase) createCaseWithInterpretation("Case1") else null)

        // When
        val response = manager.response("Export this KB")

        // Then
        val request = response.kbFileDialogRequest.shouldBeInstanceOf<KbFileDialogRequest.Export>()
        request.kbInfo.id shouldBe thyroids.id
        request.kbInfo.name shouldBe thyroids.name
        response.text shouldBe "Choose where to save \"Thyroids\" as a ZIP file."
        coVerify(exactly = 0) { service.open(any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["ImportKnowledgeBase", "ExportKnowledgeBase"])
    fun `file requests leave the no KB greeting creation offer`(action: String) = runTest {
        // Given
        every { service.openKnowledgeBase() } returns null
        every { service.knowledgeBases() } returns emptyList()
        coEvery { model.startConversation() } returns ""
        coEvery { model.response(any()) } returns """{"intent":"OTHER_REQUEST"}"""
        coEvery { model.response(action) } returns ActionComment(action).toJsonString()
        val manager = ChatManager(model, null, service)
        manager.startConversation(null, noKbGreeting(emptyList()))

        // When
        val response = manager.response(action)

        // Then
        if (action == IMPORT_KNOWLEDGE_BASE) {
            response.kbFileDialogRequest.shouldBeInstanceOf<KbFileDialogRequest.Import>()
        } else {
            response.text shouldBe NO_KB_OPEN_MESSAGE
            response.kbFileDialogRequest shouldBe null
        }
        coVerify(exactly = 1) { model.response(match { it.contains("OFFER_CREATION") }) }
        // Markdown line wrapping must not change whether the instruction is present.
        coVerify(exactly = 1) {
            model.response(match { it.replace(Regex("\\s+"), " ").contains("importing or exporting a KB") })
        }
        coVerify(exactly = 1) { model.response(action) }
        coVerify(exactly = 0) { service.create(any()) }

        // Given
        coEvery { model.response("yes") } returns "What would you like to do?"

        // When
        val next = manager.response("yes")

        // Then
        next.text shouldBe "What would you like to do?"
        coVerify(exactly = 1) { model.response("yes") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["ImportKnowledgeBase", "ExportKnowledgeBase"])
    fun `file requests are refused while building a rule`(action: String) = runTest {
        // Given
        every { rules.isRuleSessionActive() } returns true
        val manager = ChatManager(model, rules, service)

        // When
        val response = manager.processActionComment(ActionComment(action))

        // Then
        response.text shouldBe KB_ACTION_DURING_RULE_MESSAGE
        response.text shouldContain if (action == IMPORT_KNOWLEDGE_BASE) "importing" else "exporting"
        response.kbFileDialogRequest shouldBe null
        coVerify(exactly = 0) { service.openKnowledgeBase() }
        coVerify(exactly = 0) { model.response(any()) }
    }
}
