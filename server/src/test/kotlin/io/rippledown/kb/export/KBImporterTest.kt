package io.rippledown.kb.export

import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.kb.export.util.Unzipper
import io.rippledown.kb.export.util.Zipper
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import java.io.File
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val case1 = "Case1"
private const val case2 = "Case2"
private const val case3 = "Case3"
private const val userExpression = "Glucose is no more than 4.1"


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
    }

    @Test
    fun `should export then import a dummy KB`() {
        // Given a KB with some cases, a rule, and a case view.
        val kb = buildDummyKB("Whatever")
        KBExporter(tempDir, kb).export()

        // When the KB is imported.
        val rebuilt = KBImporter(tempDir, persistenceProvider).import()
        rebuilt.kbInfo.name shouldBe kb.kbInfo.name

        rebuilt.allCornerstoneCases().size shouldBe 1
        rebuilt.getCornerstoneCaseByName(case1) shouldBeEqualToComparingFields kb.getCornerstoneCaseByName(case1)

        rebuilt.allProcessedCases().size shouldBe 2
        rebuilt.getProcessedCaseByName(case2) shouldBeEqualToComparingFields kb.getProcessedCaseByName(case2)
        rebuilt.getProcessedCaseByName(case3) shouldBeEqualToComparingFields kb.getProcessedCaseByName(case3)

        rebuilt.caseViewManager.allInOrder() shouldBe kb.caseViewManager.allInOrder()

        rebuilt.ruleTree.size() shouldBe 2
        val rebuiltFirstRule = rebuilt.ruleTree.root.childRules().first()
        rebuiltFirstRule
            .structurallyEqual(kb.ruleTree.root.childRules().first()) shouldBe true
        rebuiltFirstRule.conditions.iterator().next().userExpression() shouldBe userExpression

        persistenceProvider.idStore().data() shouldHaveSize 2
    }

    fun buildDummyKB(kbName: String): KB {
        // Create a simple KB.
        val kbInfo = KBInfo(kbName)
        val pKB = persistenceProvider.createKBPersistence(kbInfo)
        val kb = KB(pKB)
        // Attributes.
        val glucose = kb.attributeManager.getOrCreate("Glucose")
        val ldl = kb.attributeManager.getOrCreate("LDL")
        val hdl = kb.attributeManager.getOrCreate("HDL")
        // Build some cases.
        val episodeDate = Instant.now().toEpochMilli()
        fun buildCase(name: String, glucoseValue: String, ldlValue: String, hdlValue: String): RDRCase {
            val rdrCaseBuilder = RDRCaseBuilder()
            rdrCaseBuilder.addResult(glucose, episodeDate, TestResult(glucoseValue))
            rdrCaseBuilder.addResult(ldl, episodeDate, TestResult(ldlValue))
            rdrCaseBuilder.addResult(hdl, episodeDate, TestResult(hdlValue))
            return rdrCaseBuilder.build(name)
        }

        val case1 = buildCase(case1, "4.0", "2.5", "1.8")
        val case2 = buildCase(case2, "4.1", "2.4", "1.6")
        val case3 = buildCase(case3, "4.2", "2.3", "1.4")
        kb.addProcessedCase(case2)
        kb.addProcessedCase(case3)

        // Add a rule.
        kb.startRuleSession(case1, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Glucose ok.")))
        val condition = EpisodicCondition(null, glucose, LessThanOrEquals(4.1), Current, userExpression)
        kb.addConditionToCurrentRuleSession(condition)
        kb.commitCurrentRuleSession()

        // Set up the case view.
        kb.caseViewManager.set(listOf(hdl, ldl, glucose))
        return kb
    }

    @Test
    fun `should import a KB from a newly created zip file`() {
        // Given a zipped KB
        val kbName = "Whatever"
        val kb = buildDummyKB(kbName)
        println("tempDir1 = ${tempDir}")
        KBExporter(tempDir, kb).export()
        val bytes = Zipper(tempDir).zip()

        //When the file is upzipped
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
