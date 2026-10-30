package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.ai.AiBooleanClassifier
import io.rippledown.kb.ai.AiClassification
import io.rippledown.kb.ai.AiEvaluationPolicy
import io.rippledown.kb.export.KBImporter
import io.rippledown.kb.export.util.Unzipper
import io.rippledown.model.AttributeKind
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.evaluation.AiEvaluationRecord
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.model.rule.evaluation.key
import io.rippledown.persistence.AiEvaluationStore
import io.rippledown.persistence.PersistentKB
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

class KBEndpointAiExportTest {
    @TempDir lateinit var directory: File
    private var beforeEvaluationSnapshot: () -> Unit = {}
    private var afterEvaluationSnapshot: () -> Unit = {}
    private val base = InMemoryKB(KBInfo("aiexport", "AI export"))
    private val store = object : AiEvaluationStore by base.aiEvaluationStore() {
        override fun all(): List<AiEvaluationRecord> {
            beforeEvaluationSnapshot()
            val snapshot = base.aiEvaluationStore().all()
            afterEvaluationSnapshot()
            return snapshot
        }
    }
    private val kb = KB(object : PersistentKB by base {
        override fun aiEvaluationStore() = store
    })
    private val session = KBSession(kb)
    private val endpoint = KBEndpoint(session)
    private val notes = kb.attributeManager.getOrCreate("clinical_notes")
    private val target = kb.attributeManager.getOrCreate("hasDiabetes", AttributeKind.DERIVED)
    private val definition = AiBoolean(notes, "Personal diabetes", revision = "r1",
        evaluationProfile = AiEvaluationPolicy.defaultProfile("gemini-2.5-flash"))
    private val request = AiEvaluationRequest(definition, "T2 Diab")
    private val record = AiEvaluationRecord(request.key(), true, "T2 Diab", definition, 1L,
        definition.evaluationProfile.model, "test-model")

    private fun commitAiRule() = session.locked {
        store.insertIfAbsent(record)
        kb.derivedDefinitionManager.store(target.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(target, ByDefinition), emptySet())
        kb.addProcessedCase(RDRCaseBuilder().apply {
            addValue(notes, defaultDate, request.input)
        }.build("Current"))
    }

    private fun exportAndImport(): KB {
        val zip = endpoint.exportKBToZip()
        Unzipper(zip.readBytes(), directory).unzip()
        return KBImporter(requireNotNull(directory.listFiles()).single(), InMemoryPersistenceProvider()).import()
    }

    @Test
    fun `a commit during export cannot produce a rule without its saved evaluation`() {
        // Given a commit interleaved after the evaluation read but before the old KB snapshot
        afterEvaluationSnapshot = ::commitAiRule

        // When the archive is exported and imported
        val imported = exportAndImport()

        // Then the archive may precede the commit, but must never include it without its result
        kb.ruleTree.size() shouldBe 2L
        if (imported.ruleTree.size() == 2L) imported.aiEvaluationStore.recordFor(request.key()) shouldBe record
        else imported.ruleTree.size() shouldBe 1L
    }

    @Test
    fun `a committed AI rule is imported with its authoritative result without reclassification`(): Unit = runBlocking {
        // Given a committed rule, its case, and a saved classification
        commitAiRule()

        // When the archive is imported and interpreted with a classifier that must not be called
        val imported = exportAndImport()
        val importedEndpoint = KBEndpoint(KBSession(imported), classifier = object : AiBooleanClassifier {
            override suspend fun classify(request: AiEvaluationRequest): AiClassification =
                error("The saved classification must be reused")
        })
        val view = importedEndpoint.viewableCase(requireNotNull(imported.allProcessedCases().single().id))

        // Then the original value and provenance survive the round trip
        imported.ruleTree.size() shouldBe 2L
        imported.aiEvaluationStore.recordFor(request.key()) shouldBe record
        view.derivedValues().single().displayValue() shouldBe "true"
        view.derivedValues().single().provenance?.model shouldBe "test-model"
    }

    @Test
    fun `evaluations arriving for a new source attribute do not make the snapshot unimportable`() {
        // Given a committed rule and a new source/evaluation arriving during the store read
        commitAiRule()
        beforeEvaluationSnapshot = {
            val laterSource = session.locked { kb.attributeManager.getOrCreate("later_notes") }
            val laterDefinition = definition.copy(sourceAttribute = laterSource, revision = "r2")
            val laterRequest = AiEvaluationRequest(laterDefinition, "Later input")
            store.insertIfAbsent(record.copy(key = laterRequest.key(), definition = laterDefinition))
        }

        // When exporting and importing the snapshot
        val imported = exportAndImport()

        // Then every included evaluation refers to an exported source and the committed result survives
        val sourceIds = imported.attributeManager.all().map { it.id }.toSet()
        imported.attributeManager.all().any { it.name == "later_notes" } shouldBe false
        imported.aiEvaluationStore.all().all { it.definition.sourceAttribute.id in sourceIds } shouldBe true
        imported.aiEvaluationStore.recordFor(request.key()) shouldBe record
    }

    @Test
    fun `reading evaluations leaves the KB lock available to other requests`() {
        // Given a store read that waits for a concurrent request to acquire the KB lock
        commitAiRule()
        Executors.newSingleThreadExecutor().use { executor ->
            beforeEvaluationSnapshot = {
                executor.submit<Boolean> { session.locked { true } }.get(5, SECONDS) shouldBe true
            }

            // When exporting and importing
            val imported = exportAndImport()

            // Then the store read completed without blocking that request and retained the evaluation
            imported.aiEvaluationStore.recordFor(request.key()) shouldBe record
        }
    }

    @Test
    fun `saved previews are retained even without an active assignment rule`() {
        // Given a saved preview for an existing source with no committed definition or rule
        store.insertIfAbsent(record)

        // When the snapshot is exported and imported
        val imported = exportAndImport()

        // Then provenance is preserved without activating the draft
        imported.aiEvaluationStore.recordFor(request.key()) shouldBe record
        imported.derivedDefinitionManager.all() shouldBe emptyMap()
        imported.ruleTree.size() shouldBe 1L
    }

    @Test
    fun `a failed evaluation store read fails export rather than returning an incomplete archive`() {
        // Given a committed rule whose evaluation store cannot be read
        commitAiRule()
        beforeEvaluationSnapshot = { throw IOException("Evaluation store unavailable") }

        // When exporting, then no apparently successful archive is returned
        shouldThrow<IOException> { endpoint.exportKBToZip() }.message shouldBe "Evaluation store unavailable"
    }
}
