package io.rippledown.kb.export

import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test

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
    fun exportImport() {
        // Create a simple KB.
        val kbInfo = KBInfo("Whatever")
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

        val case1 = buildCase("Case1", "4.0", "2.5", "1.8")
        val case2 = buildCase("Case2", "4.1", "2.4", "1.6")
        val case3 = buildCase("Case3", "4.2", "2.3", "1.4")
        kb.addProcessedCase(case2)
        kb.addProcessedCase(case3)

        // Add a rule.
        kb.startRuleSession(case1, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Glucose ok.")))
        kb.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, glucose, 4.1))
        kb.commitCurrentRuleSession()

        // Set up the case view.
        kb.caseViewManager.set(listOf(hdl, ldl, glucose))

        // Export and import.
        KBExporter(tempDir, kb).export()

        val rebuilt = KBImporter(tempDir, persistenceProvider).import()
        rebuilt.kbInfo.name shouldBe kb.kbInfo.name

        rebuilt.allCornerstoneCases().size shouldBe 1
        rebuilt.getCornerstoneCaseByName(case1.name) shouldBeEqualToComparingFields kb.getCornerstoneCaseByName(case1.name)

        rebuilt.allProcessedCases().size shouldBe 2
        rebuilt.getProcessedCaseByName(case2.name) shouldBeEqualToComparingFields kb.getProcessedCaseByName(case2.name)
        rebuilt.getProcessedCaseByName(case3.name) shouldBeEqualToComparingFields kb.getProcessedCaseByName(case3.name)

        rebuilt.caseViewManager.allInOrder() shouldBe kb.caseViewManager.allInOrder()

        rebuilt.ruleTree.size() shouldBe 2
        rebuilt.ruleTree.root.childRules().first()
            .structurallyEqual(kb.ruleTree.root.childRules().first()) shouldBe true

        persistenceProvider.idStore().data() shouldHaveSize 2
    }
}