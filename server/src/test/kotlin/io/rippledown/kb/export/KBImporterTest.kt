package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.export.util.Unzipper
import io.rippledown.kb.export.util.Zipper
import io.rippledown.model.*
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.model.rule.Literal
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import java.io.File
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val case1 = "Case1"
private const val case2 = "Case2"
private const val case3 = "Case3"
private const val userExpression = "Glucose is no more than 4.1"
private const val description = """
    # KB Export test
    This is a multi-line description.
    
    It includes a [link](https://en.wikipedia.org/wiki/Markdown).
    
    Awesome!!
"""

class KBImporterTest : ExporterTestBase() {

    private lateinit var persistenceProvider: PersistenceProvider

    @BeforeTest
    fun setup() {
        persistenceProvider = InMemoryPersistenceProvider()
    }

    @Test
    fun exportImportEmpty() {
        val kbInfo = KBInfo("Empty")
        val emptyKB = persistenceProvider.createKBPersistence(kbInfo)
        val original = KB(emptyKB)
        KBExporter(tempDir, original).export()
        val rebuilt = KBImporter(tempDir, persistenceProvider).import()
        rebuilt.kbInfo.name shouldBe original.kbInfo.name
        rebuilt.allCornerstoneCases().size shouldBe 0
        rebuilt.caseViewManager.allInOrder().size shouldBe 0
        rebuilt.ruleTree.size() shouldBe 1
        rebuilt.ruleSessionRecorder.allRuleSessionHistories().size shouldBe 0
    }

    @Test
    fun `an export containing conclusions is rejected before a persistent KB is created`() {
        // Given an otherwise valid export containing a legacy conclusion
        KBExporter(tempDir, KB(InMemoryKB(KBInfo("Rejected")))).export()
        val conclusionsDirectory = KBExportImport(tempDir).conclusionsDirectory
        conclusionsDirectory.mkdirs()
        writeFileInDirectory(conclusionsDirectory)

        // When the export is imported
        val error = shouldThrow<IllegalStateException> {
            KBImporter(tempDir, persistenceProvider).import()
        }

        // Then it is rejected without leaving a half-built persistent KB
        error.message shouldBe "This knowledge base was exported with conclusions, which are no longer supported."
        persistenceProvider.idStore().data() shouldBe emptyMap()
    }

    @Test
    fun `should export then import a dummy KB`() {
        // Given a KB with some cases, a rule, and a case view.
        val kb = buildDummyKB("Whatever")
        KBExporter(tempDir, kb).export()

        // When the KB is imported.
        val rebuilt = KBImporter(tempDir, persistenceProvider).import()
        rebuilt.kbInfo.name shouldBe kb.kbInfo.name

        rebuilt.metaInfo.getDescription() shouldBe description

        rebuilt.allCornerstoneCases().size shouldBe 1
        rebuilt.getCornerstoneCaseByName(case1) shouldBeEqualToComparingFields kb.getCornerstoneCaseByName(case1)

        rebuilt.allProcessedCases().size shouldBe 2
        rebuilt.getProcessedCaseByName(case2) shouldBeEqualToComparingFields kb.getProcessedCaseByName(case2)
        rebuilt.getProcessedCaseByName(case3) shouldBeEqualToComparingFields kb.getProcessedCaseByName(case3)

        rebuilt.caseViewManager.allInOrder() shouldBe kb.caseViewManager.allInOrder()

        rebuilt.ruleTree.size() shouldBe 2
        val rebuiltFirstRule = rebuilt.ruleTree.root.childRules().first()
        rebuiltFirstRule.structurallyEqual(kb.ruleTree.root.childRules().first()) shouldBe true
        rebuiltFirstRule.conditions.iterator().next().userExpression() shouldBe userExpression

        with(rebuilt.ruleSessionRecorder.allRuleSessionHistories()) {
            size shouldBe 1
            first().idsOfRulesAddedInSession shouldContain rebuiltFirstRule.id
        }

        persistenceProvider.idStore().data() shouldHaveSize 2
    }

    private fun buildDummyKB(kbName: String): KB {
        // Create a simple KB.
        val kbInfo = KBInfo(kbName)
        val pKB = persistenceProvider.createKBPersistence(kbInfo)
        val kb = KB(pKB)
        // MetaInfo
        kb.metaInfo.setDescription(description)
        // Attributes.
        val glucose = kb.attributeManager.getOrCreate("Glucose")
        val ldl = kb.attributeManager.getOrCreate("LDL")
        val hdl = kb.attributeManager.getOrCreate("HDL")
        // Build some cases.
        val episodeDate = Instant.now().toEpochMilli()
        fun buildCase(name: String, glucoseValue: String, ldlValue: String, hdlValue: String): RDRCase {
            val rdrCaseBuilder = RDRCaseBuilder()
            rdrCaseBuilder.addResult(glucose, episodeDate, Result(glucoseValue))
            rdrCaseBuilder.addResult(ldl, episodeDate, Result(ldlValue))
            rdrCaseBuilder.addResult(hdl, episodeDate, Result(hdlValue))
            return rdrCaseBuilder.build(name)
        }

        val case1 = buildCase(case1, "4.0", "2.5", "1.8")
        val case2 = buildCase(case2, "4.1", "2.4", "1.6")
        val case3 = buildCase(case3, "4.2", "2.3", "1.4")
        kb.addProcessedCase(case2)
        kb.addProcessedCase(case3)

        // Add a rule.
        val rsm = KBSession(kb).ruleSessionManager
        rsm.startRuleSessionToAddComment(case1, "Glucose ok.")
        val condition = EpisodicCondition(null, glucose, LessThanOrEquals(4.1), Current, userExpression)
        rsm.addConditionToCurrentRuleSession(condition)
        rsm.commitCurrentRuleSession()

        // Set up the case view.
        kb.caseViewManager.set(listOf(hdl, ldl, glucose))
        return kb
    }

    @Test
    fun `should export then import the definitions of comment and derived attributes`() {
        // Given a KB whose comment is a comment attribute and which has a
        // derived attribute, so that all the text is held in definitions
        val kbInfo = KBInfo("Definitions")
        val kb = KB(persistenceProvider.createKBPersistence(kbInfo))
        val glucose = kb.attributeManager.getOrCreate("Glucose")
        val comment = kb.attributeManager.createCommentAttribute()
        kb.derivedDefinitionManager.store(comment.id, CommentTemplate("Glucose is high."))
        val ratio = kb.attributeManager.getOrCreate("Ratio", AttributeKind.DERIVED)
        kb.derivedDefinitionManager.store(ratio.id, Literal("1.5"))

        // When it is exported and imported
        KBExporter(tempDir, kb).export()
        val rebuilt = KBImporter(tempDir, persistenceProvider).import()

        // Then the definitions are those of the original, so no comment is lost
        rebuilt.derivedDefinitionManager.definitionFor(comment.id) shouldBe CommentTemplate("Glucose is high.")
        rebuilt.derivedDefinitionManager.definitionFor(ratio.id) shouldBe Literal("1.5")
        rebuilt.attributeManager.getById(glucose.id).name shouldBe "Glucose"
    }

    @Test
    fun `should import a KB from a newly created zip file`() {
        // Given a zipped KB
        val kbName = "Whatever"
        val kb = buildDummyKB(kbName)
        KBExporter(tempDir, kb).export()
        val bytes = Zipper(tempDir).zip()

        //When the file is unzipped
        Unzipper(bytes, tempDir).unzip()

        //Then the KB can be imported
        val rebuilt = KBImporter(tempDir, persistenceProvider).import()
        rebuilt.kbInfo.name shouldBe kbName
    }

    @Test
    fun `should import from a configured KB zip`() {
        // Given a configured zipped KB
        val kbName = "Whatever"
        val file = File("src/test/resources/export/Whatever.zip")
        val bytes = file.readBytes()

        //When the file is upzipped
        Unzipper(bytes, tempDir).unzip()
        val subDirectories = tempDir.listFiles()
        require(subDirectories != null && subDirectories.size == 1) {
            "Invalid zip for KB import."
        }

        //Then the KB can be imported
        val rootDir = subDirectories[0]
        val rebuilt = KBImporter(rootDir, persistenceProvider).import()
        rebuilt.kbInfo.name shouldBe kbName
    }
}
