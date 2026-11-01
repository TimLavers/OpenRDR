package io.rippledown.kb.ai

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.IncompleteCategory
import io.rippledown.model.caseview.IncompleteInterpretation
import io.rippledown.model.condition.isAbsent
import io.rippledown.model.condition.isCondition
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.model.rule.IncompleteInferenceException
import io.rippledown.model.rule.InferenceOutcome
import io.rippledown.model.rule.Literal
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AiInferenceCoordinatorTest {
    @Test
    fun `an operation has a total deadline while the shared evaluation can finish for other callers`(): Unit = runBlocking {
        // Given a provider that has not yet answered
        installDiabetesRules()
        val case = storedCase("Slow", notes to "T2 Diab")
        val release = CompletableDeferred<Unit>()
        val classifier = FakeClassifier { release.await(); classified(true) }
        val service = AiEvaluationService(persistentKB.aiEvaluationStore(), classifier)
        val coordinator = AiInferenceCoordinator(session, service, operationTimeoutMillis = 100)
        try {
            // When the total operation deadline expires
            val failure = shouldThrow<InterpretationIncompleteException> {
                coordinator.interpretStored(requireNotNull(case.id))
            }
            // Then nothing was published, and the error is explicitly retryable
            failure.details.category shouldBe IncompleteCategory.ABANDONED
            case.interpretation.assignments().shouldBeEmpty()
        } finally {
            release.complete(Unit)
        }
        withTimeout(5_000) { AiInferenceCoordinator(session, service).interpretStored(requireNotNull(case.id)) }
        classifier.calls.get() shouldBe 1
    }
    private lateinit var persistentKB: InMemoryKB
    private lateinit var kb: KB
    private lateinit var session: KBSession
    private lateinit var notes: Attribute
    private lateinit var glucose: Attribute
    private lateinit var hasDiabetes: Attribute
    private lateinit var advice: Attribute
    private lateinit var unknown: Attribute
    private lateinit var definition: AiBoolean

    private var nextRuleId = 100
    private var nextConditionId = 1000

    private class FakeClassifier(private val answer: suspend (AiEvaluationRequest) -> AiClassification) : AiBooleanClassifier {
        val calls = AtomicInteger()
        override suspend fun classify(request: AiEvaluationRequest): AiClassification {
            calls.incrementAndGet()
            return answer(request)
        }
    }

    private fun classified(value: Boolean, evidence: String? = null) =
        AiClassification.Classified(value, evidence, "gemini-2.5-flash-001")

    @BeforeTest
    fun setup() {
        persistentKB = InMemoryKB(KBInfo("id123", "Diabetes"))
        kb = KB(persistentKB)
        session = KBSession(kb)
        notes = kb.attributeManager.getOrCreate("clinical_notes")
        glucose = kb.attributeManager.getOrCreate("Glucose")
        hasDiabetes = kb.attributeManager.getOrCreate("hasDiabetes", AttributeKind.DERIVED)
        advice = kb.attributeManager.getOrCreate("C1", AttributeKind.COMMENT)
        unknown = kb.attributeManager.getOrCreate("C2", AttributeKind.COMMENT)
        definition = AiBoolean(
            sourceAttribute = notes,
            instructions = "True if the notes affirm a personal history of diabetes.",
            revision = "rev-1",
            evaluationProfile = AiEvaluationPolicy.defaultProfile("gemini-2.5-flash")
        )
    }

    private fun coordinator(classifier: AiBooleanClassifier, maxEvaluations: Int = DEFAULT_MAX_EVALUATIONS_PER_OPERATION) =
        AiInferenceCoordinator(session, AiEvaluationService(persistentKB.aiEvaluationStore(), classifier), maxEvaluations)

    private fun addRule(assignment: AssignValue?, vararg conditions: io.rippledown.model.condition.Condition): Rule {
        val rule = Rule(nextRuleId++, null, conditions.toSet(), mutableSetOf(), assignment)
        kb.ruleTree.root.addChild(rule)
        return rule
    }

    private fun installDiabetesRules() {
        kb.derivedDefinitionManager.store(hasDiabetes.id, definition)
        addRule(AssignValue(hasDiabetes, ByDefinition))
        addRule(AssignValue(advice, CommentTemplate("Diabetic diet advice given.")), isCondition(nextConditionId++, hasDiabetes, "true"))
        addRule(AssignValue(unknown, CommentTemplate("Diabetes status unknown.")), isAbsent(hasDiabetes, nextConditionId++))
    }

    private fun storedCase(name: String, vararg attributeToValue: Pair<Attribute, String>): RDRCase {
        val built = with(RDRCaseBuilder()) {
            attributeToValue.forEach { addValue(it.first, defaultDate, it.second) }
            build(name)
        }
        return kb.addProcessedCase(built)
    }

    private fun idOf(case: RDRCase) = requireNotNull(case.id)

    @Test
    fun `a knowledge base without AI definitions is interpreted without any classification`(): Unit = runBlocking {
        // Given a local rule only
        addRule(AssignValue(advice, CommentTemplate("High glucose.")), isCondition(nextConditionId++, glucose, "12.0"))
        val case = storedCase("Fermi", glucose to "12.0")
        val classifier = FakeClassifier { error("not called") }

        // When the case is interpreted
        val prepared = coordinator(classifier).interpretStored(idOf(case))

        // Then the local rule has fired and the classifier was never consulted
        prepared.case.interpretation.assignments().map { it.attribute } shouldBe listOf(advice)
        prepared.provenance shouldBe emptyMap()
        classifier.calls.get() shouldBe 0
    }

    @Test
    fun `an interpretation that needs no classification is completed in a single locked pass`(): Unit = runBlocking {
        // Given a local rule only, and a session that counts its locked sections
        addRule(AssignValue(advice, CommentTemplate("High glucose.")), isCondition(nextConditionId++, glucose, "12.0"))
        val case = storedCase("Fermi", glucose to "12.0")
        val countingSession = mockk<KBSession>()
        every { countingSession.kb } returns kb
        every { countingSession.locked(any<() -> Any?>()) } answers { firstArg<() -> Any?>()() }
        val coordinator = AiInferenceCoordinator(
            countingSession,
            AiEvaluationService(persistentKB.aiEvaluationStore(), FakeClassifier { error("not called") })
        )

        // When the case is interpreted
        coordinator.interpretStored(idOf(case))

        // Then the whole run happened under one lock, so no change could have overtaken it
        verify(exactly = 1) { countingSession.locked(any<() -> Any?>()) }
    }

    @Test
    fun `a classification is obtained outside the lock and drives the interpretation`(): Unit = runBlocking {
        // Given an AI rule and a classifier that answers true
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")
        val classifier = FakeClassifier { classified(true, "T2 Diab") }

        // When the case is interpreted
        val prepared = coordinator(classifier).interpretStored(idOf(case))

        // Then the value is materialised with provenance and only the downstream rule for true fired
        prepared.case.latestValue(hasDiabetes) shouldBe "true"
        prepared.case.interpretation.assignments().map { it.attribute }.toSet() shouldBe setOf(hasDiabetes, advice)
        prepared.provenance.keys shouldBe setOf(hasDiabetes)
        prepared.provenance.getValue(hasDiabetes).evidence shouldBe "T2 Diab"
        classifier.calls.get() shouldBe 1
    }

    @Test
    fun `the stored case is not modified by interpretation`(): Unit = runBlocking {
        // Given an AI rule
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")

        // When the case is interpreted
        coordinator(FakeClassifier { classified(true) }).interpretStored(idOf(case))

        // Then the stored case carries neither the interpretation nor the derived value
        val stored = requireNotNull(kb.getCase(idOf(case)))
        stored.interpretation.assignments().shouldBeEmpty()
        stored.attributes.contains(hasDiabetes) shouldBe false
    }

    @Test
    fun `an obtained classification is kept by the knowledge base for every later interpretation`(): Unit = runBlocking {
        // Given a case classified once
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")
        val classifier = FakeClassifier { classified(false) }
        coordinator(classifier).interpretStored(idOf(case))

        // When the KB interprets the case itself, under the lock, with no coordinator
        val materialised = session.locked { kb.materialise(requireNotNull(kb.getCase(idOf(case)))) }

        // Then the prepared result is read without any further classification
        materialised.latestValue(hasDiabetes) shouldBe "false"
        classifier.calls.get() shouldBe 1
    }

    @Test
    fun `a saved evaluation is reused without consulting the classifier`(): Unit = runBlocking {
        // Given a case already classified once, with the in-memory results lost since
        kb.derivedDefinitionManager.store(hasDiabetes.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(hasDiabetes, ByDefinition), emptySet())
        val case = storedCase("Fermi", notes to "T2 Diab")
        coordinator(FakeClassifier { classified(false) }).interpretStored(idOf(case))
        kb = KB(persistentKB)
        session = KBSession(kb)

        // When it is interpreted again with a classifier that must not be called
        val second = FakeClassifier { error("not called") }
        val prepared = coordinator(second).interpretStored(idOf(case))

        // Then the saved result is used
        prepared.case.latestValue(hasDiabetes) shouldBe "false"
        second.calls.get() shouldBe 0
    }

    @Test
    fun `a case without the source text needs no classification and keeps absence semantics`(): Unit = runBlocking {
        installDiabetesRules()
        val case = storedCase("Fermi", glucose to "5.0")
        val classifier = FakeClassifier { error("not called") }

        val prepared = coordinator(classifier).interpretStored(idOf(case))

        prepared.case.interpretation.assignments().map { it.attribute }.toSet() shouldBe setOf(hasDiabetes, unknown)
        classifier.calls.get() shouldBe 0
    }

    @Test
    fun `a failed classification is reported as incomplete and fires no absence rule`(): Unit = runBlocking {
        // Given a classifier that fails
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")

        // When the case is interpreted
        val thrown = shouldThrow<InterpretationIncompleteException> {
            coordinator(FakeClassifier { AiClassification.Failed("Gemini unavailable") }).interpretStored(idOf(case))
        }

        // Then the details name the case, the attribute and the reason
        thrown.details shouldBe IncompleteInterpretation(
            idOf(case), IncompleteCategory.EVALUATION_FAILED, hasDiabetes.name, "Gemini unavailable"
        )
    }

    @Test
    fun `an unknown case is abandoned`(): Unit = runBlocking {
        val thrown = shouldThrow<InterpretationIncompleteException> {
            coordinator(FakeClassifier { error("not called") }).interpretStored(999)
        }

        thrown.details.category shouldBe IncompleteCategory.ABANDONED
        thrown.details.caseId shouldBe 999
    }

    @Test
    fun `the KB lock is free while a classification is in progress`(): Unit = runBlocking {
        // Given a classifier that blocks until released
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")
        val release = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        val classifier = FakeClassifier {
            entered.complete(Unit)
            release.await()
            classified(true)
        }

        // When interpretation is waiting on the classifier
        val interpretation = async(Dispatchers.Default) { coordinator(classifier).interpretStored(idOf(case)) }
        withTimeout(5_000) { entered.await() }

        // Then another operation on the same KB completes
        val otherOperation = withTimeoutOrNull(2_000) { session.locked { kb.description() } }
        otherOperation.shouldBeInstanceOf<String>()

        release.complete(Unit)
        withTimeout(5_000) { interpretation.await() }.case.latestValue(hasDiabetes) shouldBe "true"
    }

    @Test
    fun `a rule added during classification is seen by the published interpretation`(): Unit = runBlocking {
        // Given a classifier that, while classifying, sees a user add a rule on the AI value
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")
        val extra = kb.attributeManager.getOrCreate("C3", AttributeKind.COMMENT)
        val classifier = FakeClassifier {
            session.locked {
                addRule(AssignValue(extra, CommentTemplate("Review medication.")), isCondition(nextConditionId++, hasDiabetes, "true"))
            }
            classified(true)
        }

        // When the case is interpreted
        val prepared = coordinator(classifier).interpretStored(idOf(case))

        // Then the run was repeated on the new tree, reusing the classification
        prepared.case.interpretation.assignments().map { it.attribute }.toSet() shouldBe setOf(hasDiabetes, advice, extra)
        classifier.calls.get() shouldBe 1
    }

    @Test
    fun `a definition edited during classification is re-evaluated`(): Unit = runBlocking {
        // Given a classifier that, on its first call, sees the definition revised
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")
        val revised = definition.copy(instructions = "True only for type 2 diabetes.", revision = "rev-2")
        val classifier = FakeClassifier { request ->
            if (request.definition.revision == "rev-1") {
                session.locked { kb.derivedDefinitionManager.store(hasDiabetes.id, revised) }
                classified(true)
            } else {
                classified(false)
            }
        }

        // When the case is interpreted
        val prepared = coordinator(classifier).interpretStored(idOf(case))

        // Then the published value comes from the revised definition
        prepared.case.latestValue(hasDiabetes) shouldBe "false"
        classifier.calls.get() shouldBe 2
    }

    @Test
    fun `a case deleted during classification is abandoned without publication`(): Unit = runBlocking {
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")
        val classifier = FakeClassifier {
            session.locked { kb.deletedProcessedCaseWithName("Fermi") }
            classified(true)
        }

        val thrown = shouldThrow<InterpretationIncompleteException> {
            coordinator(classifier).interpretStored(idOf(case))
        }

        thrown.details.category shouldBe IncompleteCategory.ABANDONED
        kb.getCase(idOf(case)).shouldBeNull()
    }

    @Test
    fun `an operation that keeps demanding new evaluations is abandoned at the budget`(): Unit = runBlocking {
        // Given every classification is overtaken by a definition edit, so a new one is always demanded
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")
        var revision = 1
        val classifier = FakeClassifier {
            session.locked {
                revision++
                kb.derivedDefinitionManager.store(hasDiabetes.id, definition.copy(revision = "rev-$revision"))
            }
            classified(true)
        }

        // When the case is interpreted with a budget of one evaluation
        val thrown = shouldThrow<InterpretationIncompleteException> {
            coordinator(classifier, maxEvaluations = 1).interpretStored(idOf(case))
        }

        // Then the run gives up after the budget rather than looping
        thrown.details.category shouldBe IncompleteCategory.ABANDONED
        classifier.calls.get() shouldBe 1
    }

    @Test
    fun `a cancelled caller publishes nothing and leaves the stored case untouched`(): Unit = runBlocking {
        // Given a classifier that blocks until released
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")
        val release = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        val classifier = FakeClassifier {
            entered.complete(Unit)
            release.await()
            classified(true)
        }
        val interpretation = async(Dispatchers.Default) { coordinator(classifier).interpretStored(idOf(case)) }
        withTimeout(5_000) { entered.await() }

        // When the caller is cancelled and the classification later completes
        interpretation.cancel()
        release.complete(Unit)
        delay(100)

        // Then the caller got no result and the stored case is untouched
        interpretation.isCancelled shouldBe true
        requireNotNull(kb.getCase(idOf(case))).interpretation.assignments().shouldBeEmpty()
    }

    @Test
    fun `a case that is not stored can be interpreted against the knowledge base`(): Unit = runBlocking {
        // Given an AI rule and a case that is not in the KB
        installDiabetesRules()
        val unstored = with(RDRCaseBuilder()) {
            addValue(notes, defaultDate, "FH diab")
            build("Preview")
        }

        // When it is interpreted
        val prepared = coordinator(FakeClassifier { classified(false) }).interpret(unstored)

        // Then the result is on the returned copy, and the supplied case is untouched
        prepared.case.latestValue(hasDiabetes) shouldBe "false"
        unstored.interpretation.assignments().shouldBeEmpty()
    }

    @Test
    fun `a literal override needs no classification`(): Unit = runBlocking {
        installDiabetesRules()
        kb.ruleTree.rules().first { it.assignment == AssignValue(hasDiabetes, ByDefinition) }
            .addChild(Rule(nextRuleId++, null, setOf(isCondition(nextConditionId++, glucose, "12.0")), mutableSetOf(), AssignValue(hasDiabetes, Literal("true"))))
        val case = storedCase("Fermi", notes to "T2 Diab", glucose to "12.0")
        val classifier = FakeClassifier { error("not called") }

        val prepared = coordinator(classifier).interpretStored(idOf(case))

        prepared.case.latestValue(hasDiabetes) shouldBe "true"
        classifier.calls.get() shouldBe 0
    }

    @Test
    fun `an operation demanding an evaluation is run again once the result is prepared`(): Unit = runBlocking {
        // Given an operation that interprets a case under the lock
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")
        val runs = AtomicInteger()
        val classifier = FakeClassifier { classified(true) }

        // When it is run through the coordinator
        val result = coordinator(classifier).withPreparedEvaluations(idOf(case)) {
            runs.incrementAndGet()
            session.locked { kb.materialise(requireNotNull(kb.getCase(idOf(case)))).latestValue(hasDiabetes) }
        }

        // Then it was rerun exactly once, after the single classification, and the result is from the rerun
        result shouldBe "true"
        runs.get() shouldBe 2
        classifier.calls.get() shouldBe 1
    }

    @Test
    fun `an operation whose demanded evaluation fails is incomplete and is not run again`(): Unit = runBlocking {
        // Given
        installDiabetesRules()
        val case = storedCase("Fermi", notes to "T2 Diab")
        val runs = AtomicInteger()

        // When
        val thrown = shouldThrow<InterpretationIncompleteException> {
            coordinator(FakeClassifier { AiClassification.Failed("Rate limited") }).withPreparedEvaluations(idOf(case)) {
                runs.incrementAndGet()
                session.locked { kb.materialise(requireNotNull(kb.getCase(idOf(case)))) }
            }
        }

        // Then
        thrown.details shouldBe IncompleteInterpretation(idOf(case), IncompleteCategory.EVALUATION_FAILED, hasDiabetes.name, "Rate limited")
        runs.get() shouldBe 1
    }

    @Test
    fun `an operation that stops on a failure already in its context is incomplete`(): Unit = runBlocking {
        // Given an operation whose engine run reports a failed evaluation rather than demanding one
        val outcome = InferenceOutcome.Incomplete(hasDiabetes, "Refused")

        // When
        val thrown = shouldThrow<InterpretationIncompleteException> {
            coordinator(FakeClassifier { error("not called") }).withPreparedEvaluations(7) {
                throw IncompleteInferenceException(outcome)
            }
        }

        // Then
        thrown.details shouldBe IncompleteInterpretation(7, IncompleteCategory.EVALUATION_FAILED, hasDiabetes.name, "Refused")
    }
}
