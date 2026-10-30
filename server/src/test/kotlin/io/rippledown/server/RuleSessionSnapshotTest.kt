package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.coEvery
import io.mockk.mockk
import io.rippledown.constants.api.CORNERSTONE_STATUS
import io.rippledown.constants.server.KB_ID
import io.rippledown.constants.server.REFUSAL_HEADER
import io.rippledown.constants.server.REFUSAL_STALE
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.ai.AiBooleanClassifier
import io.rippledown.kb.ai.AiClassification
import io.rippledown.kb.ai.AiEvaluationPolicy
import io.rippledown.model.AttributeKind
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.UserId
import io.rippledown.model.caseview.IncompleteCategory
import io.rippledown.model.caseview.IncompleteInterpretation
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.seconds

class RuleSessionSnapshotTest : OpenRDRServerTestBase() {
    private val classifier = mockk<AiBooleanClassifier>()

    private suspend fun reviewWithChangedAiDefinition(): KBEndpoint {
        val session = KBSession(KB(InMemoryKB(KBInfo(kbId, kbName))))
        val kb = session.kb
        val endpoint = KBEndpoint(session, classifier = classifier)
        every { serverApplication.kbForId(kbId) } returns endpoint
        coEvery { classifier.classify(any()) } returns AiClassification.Classified(true, null, "test-model")
        val notes = kb.attributeManager.getOrCreate("clinical_notes")
        val target = kb.attributeManager.getOrCreate("hasDiabetes", AttributeKind.DERIVED)
        val definition = AiBoolean(notes, "Personal diabetes", revision = "r1",
            evaluationProfile = AiEvaluationPolicy.defaultProfile("gemini-2.5-flash"))
        kb.derivedDefinitionManager.store(target.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(target, ByDefinition), emptySet())
        fun case(name: String) = kb.addProcessedCase(RDRCaseBuilder().apply {
            addValue(notes, defaultDate, name)
        }.build(name))
        val current = case("Current")
        kb.addCornerstoneCase(case("Previous"))
        endpoint.coordinator.withPreparedEvaluations(null) {
            session.locked {
                session.ruleSessionManagerFor(testUser).startRuleSessionToAssignValue(current, "extra", "text")
            }
        }
        session.locked { kb.derivedDefinitionManager.store(target.id, definition.copy(revision = "r2")) }
        return endpoint
    }

    @Test
    fun `reconnect snapshot prepares a changed AI definition`() = testApplication {
        // Given an active cornerstone review whose AI definition was changed by another user
        setupServer()
        val endpoint = reviewWithChangedAiDefinition()

        // When the reconnect snapshot is requested
        val response = httpClient.get(CORNERSTONE_STATUS) { parameter(KB_ID, kbId) }

        // Then the review and the updated evaluation are restored
        response.status shouldBe HttpStatusCode.OK
        val status = response.body<CornerstoneStatus>()
        status.cornerstoneToReview?.name shouldBe "Previous"
        status.cornerstoneToReview?.derivedValues()?.single()?.provenance?.revision shouldBe "r2"
        status.numberOfCornerstones shouldBe 1
        endpoint.session.ruleSessionManagerFor(testUser).isRuleSessionActive() shouldBe true
    }

    @Test
    fun `a failed reconnect evaluation reports an incomplete snapshot without losing the review`() = testApplication {
        // Given a changed definition whose new evaluation cannot be obtained
        setupServer()
        val endpoint = reviewWithChangedAiDefinition()
        coEvery { classifier.classify(any()) } returns AiClassification.Failed("Provider unavailable")

        // When the reconnect snapshot is requested
        val response = httpClient.get(CORNERSTONE_STATUS) { parameter(KB_ID, kbId) }

        // Then it reports a typed failure and preserves the active review
        response.status shouldBe HttpStatusCode.ServiceUnavailable
        val incomplete = response.body<IncompleteInterpretation>()
        incomplete.category shouldBe IncompleteCategory.EVALUATION_FAILED
        incomplete.attributeName shouldBe "hasDiabetes"
        incomplete.reason shouldBe "Provider unavailable"
        endpoint.session.ruleSessionManagerFor(testUser).isRuleSessionActive() shouldBe true
    }

    @Test
    fun `cancelling a review while its snapshot evaluates does not restore the old session`() = testApplication {
        // Given a reconnect evaluation that is waiting for the provider
        setupServer()
        val endpoint = reviewWithChangedAiDefinition()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        coEvery { classifier.classify(any()) } coAnswers {
            entered.complete(Unit)
            release.await()
            AiClassification.Classified(true, null, "test-model")
        }

        coroutineScope {
            val snapshot = async { httpClient.get(CORNERSTONE_STATUS) { parameter(KB_ID, kbId) } }
            try {
                withTimeout(5.seconds) { entered.await() }

                // When the user cancels, the KB lock remains available during evaluation
                val cancellation = async(Dispatchers.Default) { endpoint.cancelRuleSession(testUser) }
                withTimeout(5.seconds) { cancellation.await() }
            } finally {
                release.complete(Unit)
            }

            // Then the waiting request is refused and a subsequent snapshot is inactive
            val response = withTimeout(5.seconds) { snapshot.await() }
            response.status shouldBe HttpStatusCode.Conflict
            response.headers[REFUSAL_HEADER] shouldBe REFUSAL_STALE
            endpoint.session.ruleSessionManagerFor(testUser).isRuleSessionActive() shouldBe false
            httpClient.get(CORNERSTONE_STATUS) { parameter(KB_ID, kbId) }.status shouldBe HttpStatusCode.NoContent
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `snapshot distinguishes an inactive session from an active one without cornerstones`(active: Boolean) =
        testApplication {
            // Given
            setupServer()
            val session = KBSession(KB(InMemoryKB(KBInfo(kbId, kbName))))
            val endpoint = KBEndpoint(session)
            every { serverApplication.kbForId(kbId) } returns endpoint
            val manager = session.ruleSessionManagerFor(testUser)
            if (active) {
                val case = RDRCaseBuilder().build("Case")
                session.kb.interpret(case)
                manager.startRuleSessionToAddComment(case, "Advice.")
            }

            // When
            val response = httpClient.get(CORNERSTONE_STATUS) { parameter(KB_ID, kbId) }

            // Then
            response.status shouldBe if (active) HttpStatusCode.OK else HttpStatusCode.NoContent
            if (active) response.body<CornerstoneStatus>() shouldBe manager.cornerstoneStatus()
            manager.isRuleSessionActive() shouldBe active
            clientFor(UserId("other-user")).get(CORNERSTONE_STATUS) { parameter(KB_ID, kbId) }
                .status shouldBe HttpStatusCode.NoContent
        }
}
