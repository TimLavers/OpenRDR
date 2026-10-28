package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.kb.ai.*
import io.rippledown.kb.chat.LockedRuleService
import io.rippledown.model.*
import io.rippledown.model.rule.*
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import org.junit.jupiter.api.Test

class AiDefinitionAuthoringTest {
    @Test
    fun `a caseless draft is bound to the case on which its preview is shown`() {
        // Given a draft created before selecting a case
        val draft = service.proposeAiDefinition("hasDiabetes", "clinical_notes", criteria, examples, null)
        val another = kb.addProcessedCase(RDRCaseBuilder().apply {
            addValue(notes, defaultDate, "FH diab")
        }.build("Another"))
        // When it is previewed on the first selected case
        service.previewAiDefinition(draft.id, case)
        // Then it cannot be accepted on a different case without a new offer
        shouldThrow<IllegalStateException> { service.acceptAiDefinition(draft.id, another) }
        manager.isRuleSessionActive() shouldBe false
        service.currentAiDefinitionDraft()?.caseId shouldBe case.id
    }

    @Test
    fun `closing a KB discards a draft even before a rule session has started`() {
        // Given an unaccepted draft
        draft()
        // When the KB is closed
        session.locked { session.cancelRuleSessionOf(UserId("expert")) }
        // Then its name and acceptance offer are released
        service.currentAiDefinitionDraft().shouldBeNull()
        kb.attributeManager.byName("hasDiabetes").shouldBeNull()
    }
    private val kb = KB(InMemoryKB(KBInfo("authoring", "Authoring")))
    private val session = KBSession(kb)
    private val manager = session.ruleSessionManagerFor(UserId("expert"))
    private val notes = kb.attributeManager.getOrCreate("clinical_notes")
    private val case = kb.addProcessedCase(RDRCaseBuilder().apply {
        addValue(notes, defaultDate, "T2 Diab")
    }.build("Current"))
    private var calls = 0
    private val classifier = object : AiBooleanClassifier {
        override suspend fun classify(request: AiEvaluationRequest): AiClassification {
            calls++
            return AiClassification.Classified(true, "T2 Diab", "test-model")
        }
    }
    private fun service(user: String) = LockedRuleService(session, session.ruleSessionManagerFor(UserId(user)),
        AiInferenceCoordinator(session, AiEvaluationService(kb.aiEvaluationStore, classifier)))
    private val service = service("expert")
    private val examples = listOf(
        AiBooleanExample("FH diab", false), AiBooleanExample("?diabetes", false),
        AiBooleanExample("gest diabetes", false), AiBooleanExample("Diabetic", true),
        AiBooleanExample("T2 Diab", true), AiBooleanExample("T1 diab", true)
    )
    private val criteria = "Personal diabetes history; exclude family history, suspected and gestational diabetes alone."

    private fun draft() = service.proposeAiDefinition("hasDiabetes", "clinical_notes", criteria, examples, case.id)

    @Test
    fun `draft preview and acceptance preserve the approved definition and all six labels`() {
        // Given a complete user specification
        val draft = draft()

        // When a preview is displayed and that exact draft is accepted
        service.previewAiDefinition(draft.id, case) shouldContain "Preview: true"
        service.acceptAiDefinition(draft.id, case)

        // Then previewing and starting a rule never create a live attribute
        kb.attributeManager.byName("hasDiabetes").shouldBeNull()
        manager.isRuleSessionActive() shouldBe true
        service.commitCurrentRuleSession()
        val attribute = requireNotNull(kb.attributeManager.byName("hasDiabetes"))
        val saved = kb.derivedDefinitionManager.definitionFor(attribute.id) as AiBoolean
        saved shouldBe draft.definition
        saved.examples shouldBe examples
        calls shouldBe 1
        kb.materialise(case).latestValue(attribute) shouldBe "true"
    }

    @Test
    fun `cancelled AI session releases its name without deleting another users attribute`() {
        // Given two experts drafting the same name independently
        val first = draft()
        val other = service("other")
        val second = other.proposeAiDefinition("hasDiabetes", "clinical_notes", criteria, examples, case.id)
        service.acceptAiDefinition(first.id, case)
        other.acceptAiDefinition(second.id, case)

        // When one commits and the other cancels
        other.commitCurrentRuleSession()
        val committed = requireNotNull(kb.attributeManager.byName("hasDiabetes"))
        service.cancelCurrentRuleSession()

        // Then cancellation leaves the committed definition untouched
        kb.attributeManager.byName("hasDiabetes") shouldBe committed
        kb.derivedDefinitionManager.definitionFor(committed.id) shouldBe second.definition
    }

    @Test
    fun `a competing commit refuses a stale draft name instead of replacing its definition`() {
        // Given two sessions competing for a new name
        val first = draft()
        val other = service("other")
        val second = other.proposeAiDefinition("hasDiabetes", "clinical_notes", criteria, examples, case.id)
        service.acceptAiDefinition(first.id, case)
        other.acceptAiDefinition(second.id, case)
        other.commitCurrentRuleSession()

        // When the earlier session attempts to commit
        shouldThrow<StaleRuleSessionException> { service.commitCurrentRuleSession() }

        // Then only the winner's definition and rule are live
        val attribute = requireNotNull(kb.attributeManager.byName("hasDiabetes"))
        kb.derivedDefinitionManager.definitionFor(attribute.id) shouldBe second.definition
        kb.ruleTree.size() shouldBe 2L
    }

    @Test
    fun `revised drafts cannot be accepted by an old confirmation`() {
        // Given an offer superseded by a corrected definition
        val first = draft()
        val second = service.proposeAiDefinition("hasDiabetes", "clinical_notes", "Include gestational diabetes", examples, case.id)

        // When the old offer is accepted
        shouldThrow<IllegalStateException> { service.acceptAiDefinition(first.id, case) }

        // Then the revision is new and no rule session is started
        (second.definition.revision != first.definition.revision) shouldBe true
        manager.isRuleSessionActive() shouldBe false
    }

    @Test
    fun `a draft for one case cannot be accepted on another case`() {
        // Given a draft preview linked to the current case
        val draft = draft()
        val other = kb.addProcessedCase(RDRCaseBuilder().apply {
            addValue(notes, defaultDate, "FH diab")
        }.build("Other"))

        // When the selection changes before acceptance
        shouldThrow<IllegalStateException> { service.acceptAiDefinition(draft.id, other) }

        // Then no session is started
        manager.isRuleSessionActive() shouldBe false
    }

    @Test
    fun `source names resolve exactly and never create an attribute`() {
        // Given a misspelt source
        // When drafting is attempted
        shouldThrow<IllegalArgumentException> {
            service.proposeAiDefinition("hasDiabetes", "clinical_note", criteria, examples, case.id)
        }.message shouldContain "clinical_notes"

        // Then neither a source nor a target was invented
        kb.attributeManager.byName("clinical_note").shouldBeNull()
        kb.attributeManager.byName("hasDiabetes").shouldBeNull()
        calls shouldBe 0
    }

    @Test
    fun `global AI edit keeps the rules and creates a fresh revision`() {
        // Given a committed AI definition
        service.acceptAiDefinition(draft().id, case)
        service.commitCurrentRuleSession()
        val attribute = requireNotNull(kb.attributeManager.byName("hasDiabetes"))
        val old = kb.derivedDefinitionManager.definitionFor(attribute.id) as AiBoolean
        val rules = kb.ruleTree.rules().map { it.id }

        // When a global definition edit is drafted and accepted
        val edit = service.proposeAiDefinition("hasDiabetes", "clinical_notes", "Include gestational diabetes", examples, case.id, edit = true)
        service.acceptAiDefinition(edit.id, case)

        // Then the profile is retained, the revision changes, and no rule review is opened
        val revised = kb.derivedDefinitionManager.definitionFor(attribute.id) as AiBoolean
        revised shouldBe edit.definition
        revised.evaluationProfile shouldBe old.evaluationProfile
        (revised.revision != old.revision) shouldBe true
        kb.ruleTree.rules().map { it.id } shouldBe rules
        manager.isRuleSessionActive() shouldBe false
    }
}
