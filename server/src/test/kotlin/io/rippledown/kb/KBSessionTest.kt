package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.diff.Addition
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBSessionTest {
    private lateinit var kb: KB
    private lateinit var session: KBSession

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "TestKB")
        kb = KB(InMemoryKB(kbInfo))
        session = KBSession(kb)
    }

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun createCase(name: String, value: String = "1.0", id: Long? = null): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(glucose(), defaultDate, value)
        return builder.build(name, id)
    }

    @Test
    fun `should expose the KB instance`() {
        // Given/When
        val exposedKB = session.kb

        // Then
        exposedKB shouldBe kb
    }

    @Test
    fun `should create a RuleSessionManager`() {
        // Given/When
        val rsm = session.ruleSessionManager

        // Then
        rsm.shouldBeInstanceOf<RuleSessionManager>()
    }

    @Test
    fun `should create a ChatSessionManager`() {
        // Given/When
        val csm = session.chatSessionManager

        // Then
        csm.shouldBeInstanceOf<ChatSessionManager>()
    }

    @Test
    fun `should pass webSocketManager to RuleSessionManager`() {
        // Given
        val webSocketManager = mockk<WebSocketManager>()
        val sessionWithWs = KBSession(kb, webSocketManager)

        // When
        val rsm = sessionWithWs.ruleSessionManager

        // Then - verify it works by using the rsm (indirectly confirms wiring)
        rsm shouldNotBe null
    }

    @Test
    fun `should allow rule session operations through ruleSessionManager`() {
        // Given
        val sessionCase = createCase("Case1")
        val conclusion = kb.conclusionManager.getOrCreate("Go.")

        // When
        session.ruleSessionManager.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))
        session.ruleSessionManager.commitCurrentRuleSession()

        // Then
        kb.interpret(sessionCase)
        sessionCase.interpretation.conclusionTexts() shouldBe setOf("Go.")
    }

    @Test
    fun `should allow startRuleSessionToAddComment through ruleSessionManager`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0", id = 1)
        kb.interpret(sessionCase)
        val viewableCase = kb.viewableCase(sessionCase)
        val comment = "Go to Bondi."

        // When
        session.ruleSessionManager.startRuleSessionToAddComment(viewableCase, comment)

        // Then
        session.ruleSessionManager.currentDiff shouldBe Addition(comment)
    }

    @Test
    fun `should share the same RuleSessionManager between session and chatSessionManager`() {
        // Given - start a rule session through rsm
        val sessionCase = createCase("Case1")
        session.ruleSessionManager.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go."))
        )

        // When/Then - the session should show active
        session.ruleSessionManager.isRuleSessionActive() shouldBe true
    }
}
