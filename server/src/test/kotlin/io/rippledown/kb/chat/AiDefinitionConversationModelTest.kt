package io.rippledown.kb.chat

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.chat.CHAT_BOT_DONE_MESSAGE
import io.rippledown.constants.chat.caseGreeting
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.ai.AiBooleanClassifier
import io.rippledown.kb.ai.AiClassification
import io.rippledown.kb.chat.RuleConversation.Companion.MORE_REASONS_QUESTION
import io.rippledown.llm.geminiApiCallCount
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.UserId
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.KBEndpoint
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.time.Duration.Companion.seconds

/** Opt-in, headless chat regression using Gemini, made-up notes and a fixed classifier. */
@EnabledIfEnvironmentVariable(named = "RUN_AI_AUTHORING_TEST", matches = "true")
class AiDefinitionConversationModelTest {
    @Test
    fun `accepting a definition continues rule building and declining reasons commits it`() = runTest(timeout = 120.seconds) {
        // Given a real chat conversation on a case with an empty report
        val kb = KB(InMemoryKB(KBInfo("ai_authoring", "AI authoring")))
        val session = KBSession(kb)
        val user = UserId("expert")
        val manager = session.ruleSessionManagerFor(user)
        val case = kb.addProcessedCase(RDRCaseBuilder().apply {
            addValue(kb.attributeManager.getOrCreate("clinical_notes"), defaultDate, "Diabetic")
            addValue(kb.attributeManager.getOrCreate("age"), defaultDate, "58")
        }.build("Example"))
        var classifications = 0
        val classifier = object : AiBooleanClassifier {
            override suspend fun classify(request: AiEvaluationRequest): AiClassification {
                classifications++
                return AiClassification.Classified(true, "Diabetic", "test-model")
            }
        }
        val endpoint = KBEndpoint(session, classifier = classifier)
        val kbService = mockk<KnowledgeBaseService>()
        every { kbService.knowledgeBases() } returns listOf(kb.kbInfo)
        every { kbService.demonstrations() } returns emptyList()
        val view = kb.viewableCaseOf(case)
        val chat = ChatManagerFactory { kbService }.create(user, ChatContext.CaseInKnowledgeBase(endpoint, view))
        chat.startConversation(view, greeting = caseGreeting(emptyList()))
        val proposed = chat.response(
            """Define an AI-derived Boolean attribute named hasDiabetes from clinical_notes.
                |Return true only for a clear personal history of diabetes; exclude family history,
                |suspected diabetes and gestational diabetes alone. Return false otherwise.
                |Examples: "Diabetic" -> true; "FH diab" -> false.
            """.trimMargin()
        )
        requireNotNull(proposed.draft).attributeName shouldBe "hasDiabetes"
        manager.isRuleSessionActive() shouldBe false
        val callsBeforeAcceptance = geminiApiCallCount.get()

        // When the expert accepts the displayed definition
        val accepted = chat.response("yes")

        // Then the server asks for reasons without a model turn and keeps the rule pending
        accepted.text shouldBe "Accepted the definition of \"hasDiabetes\".\n\n$MORE_REASONS_QUESTION"
        accepted.suggestions.shouldNotBeEmpty()
        geminiApiCallCount.get() shouldBe callsBeforeAcceptance
        manager.isRuleSessionActive() shouldBe true
        kb.attributeManager.byName("hasDiabetes").shouldBeNull()

        // When the expert declines further reasons, the model answers the server's actual question
        val completed = chat.response("no")

        // Then it commits the pending assignment rule rather than asking about a new comment or definition
        completed.text shouldBe CHAT_BOT_DONE_MESSAGE
        manager.isRuleSessionActive() shouldBe false
        val attribute = requireNotNull(kb.attributeManager.byName("hasDiabetes"))
        kb.materialise(case).latestValue(attribute) shouldBe "true"
        classifications shouldBe 1
    }
}
