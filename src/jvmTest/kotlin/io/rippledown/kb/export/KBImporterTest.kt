package io.rippledown.kb.export

import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.model.*
import io.rippledown.model.condition.LessThanOrEqualTo
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.persistence.InMemoryPersistenceProvider
import io.rippledown.persistence.PersistenceProvider
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
        rebuilt.kbInfo shouldBe original.kbInfo
        rebuilt.allCases().size shouldBe 0
        rebuilt.caseViewManager.allAttributesInOrder().size shouldBe 0
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
        kb.addCase(case1)
        kb.addCase(case2)
        kb.addCase(case3)

        // Add a rule.
        val sessionCase = kb.getCaseByName(case1.name)
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(Conclusion(33, "Glucose ok.")))
        kb.addConditionToCurrentRuleSession(LessThanOrEqualTo(glucose, 4.1))
        kb.commitCurrentRuleSession()

        // Set up the case view.
        kb.caseViewManager.setAttributes(listOf(hdl, ldl, glucose))

        // Export and import.
        // Rebuild the persistence provider so that we're not attempting to have
        // two copies of the same KB.
        persistenceProvider = InMemoryPersistenceProvider()
        KBExporter(tempDir, kb).export()
        val rebuilt = KBImporter(tempDir, persistenceProvider).import()
        rebuilt.kbInfo shouldBe kb.kbInfo
        rebuilt.allCases().size shouldBe 3
        rebuilt.getCaseByName(case1.name) shouldBeEqualToComparingFields kb.getCaseByName(case1.name)

        rebuilt.caseViewManager.allAttributesInOrder() shouldBe kb.caseViewManager.allAttributesInOrder()

        rebuilt.ruleTree.size() shouldBe 2
        rebuilt.ruleTree.root.childRules().first().structurallyEqual(kb.ruleTree.root.childRules().first()) shouldBe true
    }
}