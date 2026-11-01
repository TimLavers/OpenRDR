package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.ai.*
import io.rippledown.model.*
import io.rippledown.model.rule.*
import io.rippledown.model.rule.evaluation.*
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AiEvaluationExportImportTest {
    @TempDir lateinit var directory: File
    private val kb = KB(InMemoryKB(KBInfo("aiexport", "AI export")))
    private val notes = kb.attributeManager.getOrCreate("clinical_notes")
    private val target = kb.attributeManager.getOrCreate("hasDiabetes", AttributeKind.DERIVED)
    private val definition = AiBoolean(notes, "Clear personal diabetes", revision = "r1",
        evaluationProfile = AiEvaluationPolicy.defaultProfile("gemini-2.5-flash"))
    private val request = AiEvaluationRequest(definition, "T2 Diab")
    private val record = AiEvaluationRecord(request.key(), true, "T2 Diab", definition, 123L,
        definition.evaluationProfile.model, "model-version")
    private val provider = InMemoryPersistenceProvider()

    private fun export(withRecord: Boolean = true) {
        kb.derivedDefinitionManager.store(target.id, definition)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(target, ByDefinition), emptySet())
        kb.addProcessedCase(RDRCaseBuilder().apply { addValue(notes, defaultDate, "T2 Diab") }.build("Demo"))
        if (withRecord) kb.aiEvaluationStore.insertIfAbsent(record)
        KBExporter(directory, kb).export()
    }

    private fun evaluationFile() = requireNotNull(File(directory, "AiEvaluations").listFiles()).single()
    private fun changeRecord(change: (JsonObject) -> JsonObject) {
        val file = evaluationFile()
        val envelope = Json.parseToJsonElement(file.readText()).jsonObject
        val edited = change(envelope.getValue("record").jsonObject)
        file.writeText(JsonObject(envelope + ("record" to edited)).toString())
    }

    @Test
    fun `export and import preserve classification provenance without a provider request`(): Unit = runBlocking {
        // Given a saved classification and rule
        export()
        // When imported into a fresh KB and interpreted
        val imported = KBImporter(directory, provider).import()
        val classifier = object : AiBooleanClassifier {
            override suspend fun classify(request: AiEvaluationRequest): AiClassification = error("Must reuse the imported record")
        }
        val case = imported.allProcessedCases().single()
        val result = AiInferenceCoordinator(KBSession(imported), AiEvaluationService(imported.aiEvaluationStore, classifier))
            .interpretStored(requireNotNull(case.id))
        // Then the value, evidence, timestamp and model survive
        result.case.latestValue(imported.attributeManager.getById(target.id)) shouldBe "true"
        imported.aiEvaluationStore.recordFor(request.key()) shouldBe record
        result.provenance.values.single().evaluatedAt shouldBe 123L
        result.provenance.values.single().model shouldBe "model-version"
    }

    @Test
    fun `preview records survive without activating an orphaned draft`() {
        // Given a result for a draft with no committed definition or rule
        kb.aiEvaluationStore.insertIfAbsent(record)
        KBExporter(directory, kb).export()
        // When imported
        val imported = KBImporter(directory, provider).import()
        // Then the result is retained but no definition or assignment is activated
        imported.aiEvaluationStore.recordFor(request.key()) shouldBe record
        imported.derivedDefinitionManager.all() shouldBe emptyMap()
        imported.ruleTree.size() shouldBe 1L
    }

    @Test
    fun `older exports without evaluation records evaluate only when subsequently requested`(): Unit = runBlocking {
        // Given an export without its optional evaluations directory
        export(withRecord = false)
        File(directory, "AiEvaluations").delete()
        var calls = 0
        val classifier = object : AiBooleanClassifier {
            override suspend fun classify(request: AiEvaluationRequest): AiClassification {
                calls++
                return AiClassification.Classified(false, null, "new-model")
            }
        }
        // When imported and subsequently requested
        val imported = KBImporter(directory, provider).import()
        calls shouldBe 0
        val case = imported.allProcessedCases().single()
        AiInferenceCoordinator(KBSession(imported), AiEvaluationService(imported.aiEvaluationStore, classifier))
            .interpretStored(requireNotNull(case.id)).case.latestValue(imported.attributeManager.getById(target.id)) shouldBe "false"
        // Then the new result belongs only to the destination KB
        calls shouldBe 1
        kb.aiEvaluationStore.recordFor(request.key()) shouldBe null
    }

    @Test
    fun `a corrupt definition fingerprint is rejected before persistence is created`() {
        // Given a tampered key
        export()
        changeRecord { row -> JsonObject(row + ("key" to JsonObject(row.getValue("key").jsonObject +
            ("definitionFingerprint" to JsonPrimitive("0".repeat(64)))))) }
        // When importing
        shouldThrow<IllegalArgumentException> { KBImporter(directory, provider).import() }
        // Then there is no partially created KB
        provider.idStore().data() shouldBe emptyMap()
    }

    @Test
    fun `a malformed input hash is rejected before persistence is created`() {
        // Given a malformed digest
        export()
        changeRecord { row -> JsonObject(row + ("key" to JsonObject(row.getValue("key").jsonObject +
            ("inputHash" to JsonPrimitive("not-a-sha256"))))) }
        // When importing
        shouldThrow<IllegalArgumentException> { KBImporter(directory, provider).import() }
        // Then nothing was persisted
        provider.idStore().data() shouldBe emptyMap()
    }

    @Test
    fun `conflicting duplicates are rejected while exact duplicates are accepted`() {
        // Given an exact duplicate export record
        export()
        val duplicate = File(evaluationFile().parentFile, "duplicate.json")
        duplicate.writeText(evaluationFile().readText())
        KBImporter(directory, InMemoryPersistenceProvider()).import().aiEvaluationStore.recordFor(request.key()) shouldBe record
        // When the duplicate disagrees on the result
        duplicate.writeText(duplicate.readText().replace("\"value\":true", "\"value\":false"))
        shouldThrow<IllegalArgumentException> { KBImporter(directory, provider).import() }
        // Then the conflicting archive was rejected before persistence
        provider.idStore().data() shouldBe emptyMap()
    }

    @Test
    fun `unsupported definition policy is rejected before persistence is created`() {
        // Given an unsupported saved definition even with no evaluation records
        export(withRecord = false)
        val file = requireNotNull(File(directory, "Definitions").listFiles()).single()
        file.writeText(file.readText().replace("\"promptVersion\":1", "\"promptVersion\":999"))
        // When importing
        shouldThrow<IllegalArgumentException> { KBImporter(directory, provider).import() }
        // Then the model policy was not silently changed
        provider.idStore().data() shouldBe emptyMap()
    }
}
