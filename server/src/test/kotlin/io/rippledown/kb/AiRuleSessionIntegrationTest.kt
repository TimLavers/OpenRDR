package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.kb.ai.*
import io.rippledown.model.*
import io.rippledown.model.rule.*
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.model.condition.isCondition
import io.rippledown.kb.chat.LockedRuleService
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test

class AiRuleSessionIntegrationTest {
    @Test
    fun `changed source notes cannot be committed using the previous review`() = runTest {
        // Given an AI assignment reviewed against the original notes
        val original = case("Current", "T2 Diab")
        prepared { manager.startRuleSessionToAssignValue(original, "hasDiabetes", definition) }
        // When the primary case is replaced while review is in progress
        kb.deletedProcessedCaseWithName("Current")
        case("Current", "FH diab")
        // Then commitment based on the earlier case is refused
        shouldThrow<StaleRuleSessionException> { prepared { manager.commitCurrentRuleSession() } }
        kb.ruleSessionRecorder.allRuleSessionHistories().shouldBeEmpty()
        kb.ruleTree.size() shouldBe 1L
    }
    @Test
    fun `a failed commit request does not install the requested conditions`() = runTest {
        // Given a prepared session, a revised definition, and one failing cornerstone
        kb.derivedDefinitionManager.store(target.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(target, ByDefinition), emptySet())
        val case = case("Current", "T2 Diab")
        kb.addCornerstoneCase(case("Previous", "FH diab"))
        prepared { manager.startRuleSessionToAssignValue(case, "extra", "text") }
        kb.derivedDefinitionManager.store(target.id, definition.copy(revision = "revision-2"))
        answer = { if (it.input == "FH diab") AiClassification.Failed("Unavailable")
            else AiClassification.Classified(true, null, "test-model") }
        val condition = kb.conditionManager.getOrCreate(isCondition(null, notes, "T2 Diab"))
        // When revalidation cannot interpret the cornerstone
        shouldThrow<InterpretationIncompleteException> {
            prepared { manager.commitRuleSession(RuleRequest(requireNotNull(case.id),
                io.rippledown.model.condition.RuleConditionList(listOf(condition)))) }
        }
        // Then the request did not partially alter the review or commit a rule
        manager.currentRuleSessionConditionTexts().shouldBeEmpty()
        manager.isRuleSessionActive() shouldBe true
        kb.ruleSessionRecorder.allRuleSessionHistories().shouldBeEmpty()
    }
    @Test
    fun `failed removal of a condition preserves the review conditions`() = runTest {
        // Given a condition excluding the cornerstone, followed by a new definition revision
        kb.derivedDefinitionManager.store(target.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(target, ByDefinition), emptySet())
        val case = case("Current", "T2 Diab")
        kb.addCornerstoneCase(case("Previous", "FH diab"))
        prepared { manager.startRuleSessionToAssignValue(case, "extra", "text") }
        val condition = kb.conditionManager.getOrCreate(isCondition(null, notes, "T2 Diab"))
        manager.addConditionToCurrentRuleSession(condition)
        kb.derivedDefinitionManager.store(target.id, definition.copy(revision = "revision-2"))
        answer = { AiClassification.Failed("Unavailable") }
        // When removing the condition would show a cornerstone that cannot be interpreted
        shouldThrow<InterpretationIncompleteException> {
            prepared { manager.removeConditionByText(condition.asText()) }
        }
        // Then the attempted change is not applied
        manager.currentRuleSessionConditionTexts() shouldBe setOf(condition.asText())
        manager.conflictingCasesInCurrentRuleSession().shouldBeEmpty()
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = [false, true])
    fun `closing a KB during evaluation prevents a late endpoint rule operation`(allInOne: Boolean): Unit = runBlocking {
        // Given a start awaiting classification
        kb.derivedDefinitionManager.store(target.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(target, ByDefinition), emptySet())
        val case = case("Current", "T2 Diab")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val endpoint = io.rippledown.server.KBEndpoint(session, classifier = object : AiBooleanClassifier {
            override suspend fun classify(request: AiEvaluationRequest): AiClassification {
                entered.complete(Unit)
                release.await()
                return AiClassification.Classified(true, null, "test-model")
            }
        })
        val starting = async(Dispatchers.Default) {
            runCatching {
                if (allInOne) endpoint.buildRule(BuildRuleRequest(case.name,
                    io.rippledown.model.diff.Addition("Advice"), emptyList()), UserId("expert"))
                else endpoint.startRuleSession(SessionStartRequest(requireNotNull(case.id),
                    io.rippledown.model.diff.Addition("Advice")), UserId("expert"))
            }
        }
        try {
            withTimeout(5_000) { entered.await() }
            // When the context is closed before the response arrives
            session.locked { session.cancelRuleSessionOf(UserId("expert")) }
        } finally {
            release.complete(Unit)
        }
        // Then the operation cannot create a session in the closed context
        (withTimeout(5_000) { starting.await() }.exceptionOrNull() is StaleRuleSessionException) shouldBe true
        manager.isRuleSessionActive() shouldBe false
        kb.ruleTree.size() shouldBe 2L
    }

    @Test
    fun `condition translation cannot return a condition after the user context changes`() {
        // Given translation that is still in progress when the context is closed
        val case = case("Current", "T2 Diab")
        manager.setConditionParser(object : ConditionParser {
            override fun parse(expression: String, attributeFor: (String) -> Attribute): io.rippledown.model.condition.Condition {
                session.cancelRuleSessionOf(UserId("expert"))
                return isCondition(null, notes, "T2 Diab")
            }
        })
        // When the translation returns
        // Then its result is not applied to the changed context
        shouldThrow<StaleRuleSessionException> { manager.conditionForExpression(case, "notes show diabetes") }
    }
    private val kb = KB(InMemoryKB(KBInfo("aisessions", "AI sessions")))
    private val session = KBSession(kb)
    private val manager = session.ruleSessionManagerFor(UserId("expert"))
    private val notes = kb.attributeManager.getOrCreate("clinical_notes")
    private val target = kb.attributeManager.getOrCreate("hasDiabetes", AttributeKind.DERIVED)
    private val definition = AiBoolean(
        notes, "Affirmative personal diabetes history", revision = "revision-1",
        evaluationProfile = AiEvaluationPolicy.defaultProfile("gemini-2.5-flash")
    )
    private val requests = mutableListOf<AiEvaluationRequest>()
    private var answer: (AiEvaluationRequest) -> AiClassification = {
        AiClassification.Classified(it.input == "T2 Diab", null, "test-model")
    }
    private val classifier = object : AiBooleanClassifier {
        override suspend fun classify(request: AiEvaluationRequest): AiClassification {
            requests.add(request)
            return answer(request)
        }
    }
    private val coordinator = AiInferenceCoordinator(session, AiEvaluationService(kb.aiEvaluationStore, classifier))

    private fun case(name: String, text: String) = kb.addProcessedCase(RDRCaseBuilder().apply {
        addValue(notes, defaultDate, text)
    }.build(name))

    private suspend fun <T> prepared(block: () -> T): T =
        coordinator.withPreparedEvaluations(null) { session.locked(block) }

    @Test
    fun `session start prepares its candidate case and leaves the stored case untouched`() = runTest {
        // Given a definition that has no assignment rule yet
        kb.derivedDefinitionManager.store(target.id, definition)
        val case = case("Current", "T2 Diab")

        // When the assignment is previewed
        prepared { manager.startRuleSession(case, ChangeTreeToAddAssignment(AssignValue(target, ByDefinition))) }

        // Then the preview was classified without publishing a rule or interpretation
        requests.map { it.input } shouldBe listOf("T2 Diab")
        manager.isRuleSessionActive() shouldBe true
        kb.ruleTree.size() shouldBe 1L
        case.interpretation.assignments().shouldBeEmpty()
        case.latestValue(target).shouldBeNull()
    }

    @Test
    fun `session preparation evaluates candidate cornerstones and reuses results on commit`() = runTest {
        // Given different notes on the current case and a cornerstone
        kb.derivedDefinitionManager.store(target.id, definition)
        val case = case("Current", "T2 Diab")
        val cornerstone = kb.addCornerstoneCase(case("Previous", "FH diab"))

        // When the assignment is reviewed and committed
        val status = prepared {
            manager.startRuleSession(case, ChangeTreeToAddAssignment(AssignValue(target, ByDefinition)))
        }
        prepared { manager.exemptCornerstoneCase() }
        prepared { manager.commitCurrentRuleSession() }

        // Then both inputs were evaluated once, and previewing never changed the cornerstone
        status.numberOfCornerstones shouldBe 1
        requests.map { it.input }.toSet() shouldBe setOf("T2 Diab", "FH diab")
        requests.size shouldBe 2
        cornerstone.interpretation.assignments().shouldBeEmpty()
        kb.materialise(case).latestValue(target) shouldBe "true"
        manager.isRuleSessionActive() shouldBe false
    }

    @Test
    fun `a failed candidate evaluation cannot install a session`() = runTest {
        // Given an unevaluable candidate
        kb.derivedDefinitionManager.store(target.id, definition)
        val case = case("Current", "T2 Diab")
        answer = { AiClassification.Failed("Unavailable") }

        // When the preview fails
        shouldThrow<InterpretationIncompleteException> {
            prepared { manager.startRuleSession(case, ChangeTreeToAddAssignment(AssignValue(target, ByDefinition))) }
        }

        // Then there is no session or rule to commit
        manager.isRuleSessionActive() shouldBe false
        manager.pendingChange.shouldBeNull()
        kb.ruleTree.size() shouldBe 1L
    }

    @Test
    fun `commit revalidates a revised definition and a failed evaluation leaves the session open`() = runTest {
        // Given a successfully previewed assignment
        kb.derivedDefinitionManager.store(target.id, definition)
        val case = case("Current", "T2 Diab")
        prepared { manager.startRuleSession(case, ChangeTreeToAddAssignment(AssignValue(target, ByDefinition))) }
        kb.derivedDefinitionManager.store(target.id, definition.copy(revision = "revision-2"))
        answer = { AiClassification.Failed("Unavailable") }

        // When a commit needs the revised classification
        shouldThrow<InterpretationIncompleteException> { prepared { manager.commitCurrentRuleSession() } }

        // Then the new definition was evaluated and nothing was committed
        requests.map { it.definition.revision } shouldBe listOf("revision-1", "revision-2")
        manager.isRuleSessionActive() shouldBe true
        kb.ruleTree.size() shouldBe 1L
        kb.ruleSessionRecorder.allRuleSessionHistories().shouldBeEmpty()
    }

    @Test
    fun `a typed AI definition is pending until commit with the same revision`() = runTest {
        // Given a definition supplied without passing through the formula parser
        val case = case("Current", "T2 Diab")

        // When it is previewed
        prepared { manager.startRuleSessionToAssignValue(case, "hasDiabetes", definition) }

        // Then the preview is evaluated, but no definition is live yet
        requests.single().definition shouldBe definition
        kb.derivedDefinitionManager.definitionFor(target.id).shouldBeNull()
        manager.currentDerivedValueChange?.attributeName shouldBe "hasDiabetes"

        // When committed, the approved revision and its result are reused
        prepared { manager.commitCurrentRuleSession() }

        // Then the stored definition and rule match that preview
        kb.derivedDefinitionManager.definitionFor(target.id) shouldBe definition
        kb.ruleTree.rulesMatching { it.assignment?.attribute == target }.single().assignment?.expression shouldBe ByDefinition
        requests.size shouldBe 1
    }

    @Test
    fun `a failed typed definition preview leaves no pending definition or session`() = runTest {
        // Given a new definition and an unavailable classifier
        val case = case("Current", "T2 Diab")
        answer = { AiClassification.Failed("Unavailable") }

        // When its preview fails
        shouldThrow<InterpretationIncompleteException> {
            prepared { manager.startRuleSessionToAssignValue(case, "hasDiabetes", definition) }
        }

        // Then the definition cannot leak into a subsequent session
        kb.derivedDefinitionManager.definitionFor(target.id).shouldBeNull()
        manager.pendingChange.shouldBeNull()
        manager.isRuleSessionActive() shouldBe false
        kb.ruleTree.size() shouldBe 1L
    }

    @Test
    fun `typed definitions reject invalid criteria before creating an attribute`() = runTest {
        // Given blank criteria
        val case = case("Current", "T2 Diab")

        // When a new attribute is requested
        shouldThrow<IllegalArgumentException> {
            prepared { manager.startRuleSessionToAssignValue(case, "invalid", definition.copy(instructions = " ")) }
        }

        // Then validation has no side effects
        kb.attributeManager.byName("invalid").shouldBeNull()
        requests.shouldBeEmpty()
        manager.isRuleSessionActive() shouldBe false
    }

    @Test
    fun `a second start cannot discard the active session pending definition`() = runTest {
        // Given a previewed AI definition
        val case = case("Current", "T2 Diab")
        prepared { manager.startRuleSessionToAssignValue(case, "hasDiabetes", definition) }

        // When a second start is refused
        shouldThrow<IllegalStateException> {
            prepared { manager.startRuleSessionToAssignValue(case, "another", Literal("true")) }
        }
        prepared { manager.commitCurrentRuleSession() }

        // Then the original definition is still committed and no second attribute was created
        kb.derivedDefinitionManager.definitionFor(target.id) shouldBe definition
        kb.attributeManager.byName("another").shouldBeNull()
    }

    @Test
    fun `blank source permits an AI assignment with no classification`() = runTest {
        // Given a selected case with no usable input
        val case = case("Current", "  ")

        // When the typed definition is assigned and committed
        prepared { manager.startRuleSessionToAssignValue(case, "hasDiabetes", definition) }
        prepared { manager.commitCurrentRuleSession() }

        // Then absence remains distinct from false
        requests.shouldBeEmpty()
        kb.materialise(case).latestValue(target).shouldBeNull()
        kb.derivedDefinitionManager.definitionFor(target.id) shouldBe definition
    }

    @Test
    fun `an assignment depending on an AI value can be removed through rule building`() = runTest {
        // Given a comment selected by an AI-derived condition
        kb.derivedDefinitionManager.store(target.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(target, ByDefinition), emptySet())
        val comment = kb.attributeManager.createCommentAttribute()
        kb.derivedDefinitionManager.store(comment.id, CommentTemplate("Advice"))
        val condition = kb.conditionManager.getOrCreate(isCondition(null, target, "true"))
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(comment, ByDefinition), setOf(condition))
        val case = case("Current", "T2 Diab")

        // When the downstream comment is removed
        prepared { manager.startRuleSessionToRemoveComment(case, "Advice") }
        prepared { manager.commitCurrentRuleSession() }

        // Then the removal is attached to the rule that actually fired
        kb.materialise(case).interpretation.assignments().map { it.attribute } shouldBe listOf(target)
        requests.size shouldBe 1
    }

    @Test
    fun `chat suggestions evaluate with the KB lock free`(): Unit = runBlocking {
        // Given a classifier waiting until another operation has acquired the same KB lock
        kb.derivedDefinitionManager.store(target.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(target, ByDefinition), emptySet())
        val case = case("Current", "T2 Diab")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val blockingClassifier = object : AiBooleanClassifier {
            override suspend fun classify(request: AiEvaluationRequest): AiClassification {
                entered.complete(Unit)
                release.await()
                return AiClassification.Classified(true, null, "test-model")
            }
        }
        val service = LockedRuleService(session, manager, AiInferenceCoordinator(
            session, AiEvaluationService(kb.aiEvaluationStore, blockingClassifier)
        ))

        // When a synchronous chat call needs the evaluation
        val hints = async(Dispatchers.Default) { service.conditionHintsForCase(case) }
        try {
            withTimeout(5_000) { entered.await() }

            // Then a concurrent operation completes before classification is released
            withTimeout(5_000) { async(Dispatchers.Default) { session.locked { kb.ruleTree.size() } }.await() } shouldBe 2L
        } finally {
            release.complete(Unit)
        }
        withTimeout(5_000) { hints.await() }.suggestions.any { it.asText().contains("hasDiabetes") } shouldBe true
    }

    @Test
    fun `closing a KB during evaluation prevents a late chat session start`(): Unit = runBlocking {
        // Given a chat session start paused at its first classification
        kb.derivedDefinitionManager.store(target.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(target, ByDefinition), emptySet())
        val case = case("Current", "T2 Diab")
        val view = kb.viewableCaseOf(case)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val blockingClassifier = object : AiBooleanClassifier {
            override suspend fun classify(request: AiEvaluationRequest): AiClassification {
                entered.complete(Unit)
                release.await()
                return AiClassification.Classified(true, null, "test-model")
            }
        }
        val service = LockedRuleService(session, manager, AiInferenceCoordinator(
            session, AiEvaluationService(kb.aiEvaluationStore, blockingClassifier)
        ))
        val starting = async(Dispatchers.Default) {
            runCatching { service.startRuleSessionToAddComment(view, "Advice") }
        }
        try {
            withTimeout(5_000) { entered.await() }

            // When closing the KB invalidates even a start that has not installed its session yet
            session.locked { session.cancelRuleSessionOf(UserId("expert")) }
        } finally {
            release.complete(Unit)
        }

        // Then the late classification cannot start a session in the closed context
        val result = withTimeout(5_000) { starting.await() }
        (result.exceptionOrNull() is StaleRuleSessionException) shouldBe true
        manager.isRuleSessionActive() shouldBe false
        kb.ruleTree.size() shouldBe 2L
    }

    @Test
    fun `a failed next cornerstone view does not exempt the current cornerstone`() = runTest {
        // Given a review with two cornerstones and previously prepared classifications
        kb.derivedDefinitionManager.store(target.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(target, ByDefinition), emptySet())
        val case = case("Current", "T2 Diab")
        kb.addCornerstoneCase(case("First", "FH diab"))
        kb.addCornerstoneCase(case("Second", "?diabetes"))
        prepared { manager.startRuleSessionToAssignValue(case, "extra", "text") }
        kb.derivedDefinitionManager.store(target.id, definition.copy(revision = "revision-2"))
        answer = { AiClassification.Failed("Unavailable") }

        // When moving to the next cornerstone cannot obtain its classification
        shouldThrow<InterpretationIncompleteException> { prepared { manager.exemptCornerstone(0) } }

        // Then the current cornerstone remains to be reviewed
        manager.conflictingCasesInCurrentRuleSession().map { it.name } shouldBe listOf("First", "Second")
    }
}
