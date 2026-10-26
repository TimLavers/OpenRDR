package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.ai.AiBooleanClassifier
import io.rippledown.kb.ai.AiClassification
import io.rippledown.kb.ai.AiEvaluationPolicy
import io.rippledown.kb.ai.InterpretationIncompleteException
import io.rippledown.kb.report.ReportService
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.KBInfo
import io.rippledown.model.Result
import io.rippledown.model.caseview.IncompleteCategory
import io.rippledown.model.condition.isCondition
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import io.rippledown.model.report.CaseReport
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class KBEndpointAiInterpretationTest {
    private lateinit var kb: KB
    private lateinit var session: KBSession
    private lateinit var notes: Attribute
    private lateinit var hasDiabetes: Attribute
    private lateinit var advice: Attribute

    private class ScriptedClassifier : AiBooleanClassifier {
        var answer: () -> AiClassification = { AiClassification.Classified(true, "T2 Diab", "gemini-2.5-flash-001") }
        var calls = 0
        override suspend fun classify(request: AiEvaluationRequest): AiClassification {
            calls++
            return answer()
        }
    }

    private val classifier = ScriptedClassifier()

    @BeforeTest
    fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "Diabetes")))
        session = KBSession(kb)
        notes = kb.attributeManager.getOrCreate("clinical_notes")
        hasDiabetes = kb.attributeManager.getOrCreate("hasDiabetes", AttributeKind.DERIVED)
        advice = kb.attributeManager.getOrCreate("C1", AttributeKind.COMMENT)
        val definition = AiBoolean(
            sourceAttribute = notes,
            instructions = "True if the notes affirm a personal history of diabetes.",
            revision = "rev-1",
            evaluationProfile = AiEvaluationPolicy.defaultProfile("gemini-2.5-flash")
        )
        kb.derivedDefinitionManager.store(hasDiabetes.id, definition)
        kb.ruleTree.root.addChild(Rule(100, null, emptySet(), mutableSetOf(), AssignValue(hasDiabetes, ByDefinition)))
        kb.ruleTree.root.addChild(
            Rule(
                101, null, setOf(isCondition(1000, hasDiabetes, "true")), mutableSetOf(),
                AssignValue(advice, CommentTemplate("Diabetic diet advice given."))
            )
        )
    }

    private fun endpoint(reportService: ReportService = ReportService()) = KBEndpoint(session, reportService, classifier)

    private fun externalCase(name: String, vararg attributeToValue: Pair<String, String>) =
        ExternalCase(name, attributeToValue.associate { MeasurementEvent(it.first, defaultDate) to Result(it.second) })

    @Test
    fun `an ingested case is interpreted with its classification and carries no derived data`() = runTest {
        // When
        val case = endpoint().processCase(externalCase("Fermi", "clinical_notes" to "T2 Diab"))

        // Then the interpretation is on the returned case, with the definition resolved for the caller
        case.interpretation.assignments().map { it.attribute }.toSet() shouldBe setOf(hasDiabetes, advice)
        case.interpretation.assignments().first { it.attribute == hasDiabetes }.expression shouldBe
                kb.derivedDefinitionManager.definitionFor(hasDiabetes.id)
        // and the laboratory data feed carries no derived values
        case.attributes.toSet() shouldBe setOf(notes)
        kb.allProcessedCases() shouldHaveSize 1
    }

    @Test
    fun `a failed evaluation during ingestion keeps the case and reports the retained id`() = runTest {
        // Given the classifier fails
        classifier.answer = { AiClassification.Failed("Gemini unavailable") }

        // When
        val thrown = shouldThrow<InterpretationIncompleteException> {
            endpoint().processCase(externalCase("Fermi", "clinical_notes" to "T2 Diab"))
        }

        // Then the primary data is retained and the failure names the stored case
        val stored = kb.allProcessedCases().single()
        thrown.details.caseId shouldBe stored.id
        thrown.details.category shouldBe IncompleteCategory.EVALUATION_FAILED
        thrown.details.attributeName shouldBe "hasDiabetes"
        thrown.details.reason shouldBe "Gemini unavailable"
    }

    @Test
    fun `the retained case can be interpreted later without being ingested again`() = runTest {
        // Given an ingestion whose evaluation failed
        classifier.answer = { AiClassification.Failed("Gemini unavailable") }
        val thrown = shouldThrow<InterpretationIncompleteException> {
            endpoint().processCase(externalCase("Fermi", "clinical_notes" to "T2 Diab"))
        }
        val id = requireNotNull(thrown.details.caseId)

        // When the provider recovers and the stored case is requested
        classifier.answer = { AiClassification.Classified(false, null, "gemini-2.5-flash-001") }
        val case = endpoint().interpretedCase(id)

        // Then it is interpreted with the fresh classification and there is still one case
        case.interpretation.assignments().map { it.attribute } shouldBe listOf(hasDiabetes)
        kb.allProcessedCases() shouldHaveSize 1
    }

    @Test
    fun `a case without the source text is interpreted without any classification`() = runTest {
        // When
        val case = endpoint().processCase(externalCase("Fermi", "Glucose" to "5.0"))

        // Then
        case.interpretation.assignments().map { it.attribute } shouldBe listOf(hasDiabetes)
        classifier.calls shouldBe 0
    }

    @Test
    fun `the viewable case shows the classification as a derived value with its comment`() = runTest {
        // Given
        val stored = endpoint().processCase(externalCase("Fermi", "clinical_notes" to "T2 Diab"))

        // When
        val viewable = endpoint().viewableCase(requireNotNull(stored.id))

        // Then
        viewable.case.latestValue(hasDiabetes) shouldBe "true"
        viewable.viewableInterpretation.renderedComments.map { it.text } shouldBe listOf("Diabetic diet advice given.")
        classifier.calls shouldBe 1
    }

    @Test
    fun `a view of a case whose evaluation fails is refused with the diagnosis`() = runTest {
        // Given a stored case and a failing classifier
        val stored = session.locked { kb.storeCase(externalCase("Fermi", "clinical_notes" to "T2 Diab")) }
        classifier.answer = { AiClassification.Failed("Gemini unavailable") }

        // When
        val thrown = shouldThrow<InterpretationIncompleteException> { endpoint().viewableCase(requireNotNull(stored.id)) }

        // Then
        thrown.details.caseId shouldBe stored.id
        thrown.details.attributeName shouldBe "hasDiabetes"
    }

    @Test
    fun `no report is generated from an incomplete interpretation`() = runTest {
        // Given
        val reportService = mockk<ReportService>()
        val stored = session.locked { kb.storeCase(externalCase("Fermi", "clinical_notes" to "T2 Diab")) }
        classifier.answer = { AiClassification.Failed("Gemini unavailable") }

        // When
        shouldThrow<InterpretationIncompleteException> { endpoint(reportService).caseReport(requireNotNull(stored.id)) }

        // Then
        coVerify(exactly = 0) { reportService.generate(any(), any()) }
    }

    @Test
    fun `an earlier report is not returned as current after the interpretation fails`() = runTest {
        // Given a report generated while evaluation succeeded
        val reportService = mockk<ReportService>()
        coEvery { reportService.generate(any(), any()) } returns CaseReport(markdown = "Diabetic.", generated = true)
        val endpoint = endpoint(reportService)
        val stored = endpoint.processCase(externalCase("Fermi", "clinical_notes" to "T2 Diab"))
        val id = requireNotNull(stored.id)
        endpoint.caseReport(id).markdown shouldBe "Diabetic."

        // When the definition is revised and its evaluation fails
        val revised = requireNotNull(kb.derivedDefinitionManager.definitionFor(hasDiabetes.id) as AiBoolean)
            .copy(revision = "rev-2")
        session.locked { kb.derivedDefinitionManager.store(hasDiabetes.id, revised) }
        classifier.answer = { AiClassification.Failed("Gemini unavailable") }

        // Then the stale report is not served
        shouldThrow<InterpretationIncompleteException> { endpoint.caseReport(id) }
    }

    @Test
    fun `the stored case is left uninterpreted by ingestion`() = runTest {
        // When
        val returned = endpoint().processCase(externalCase("Fermi", "clinical_notes" to "T2 Diab"))

        // Then the copy handed back carries the interpretation; the stored object does not
        returned.interpretation.assignments() shouldHaveSize 2
        requireNotNull(kb.getCase(requireNotNull(returned.id))).interpretation.assignments().shouldBeEmpty()
    }
}
