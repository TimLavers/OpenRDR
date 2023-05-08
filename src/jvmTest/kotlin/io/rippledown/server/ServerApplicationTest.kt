package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.CaseTestUtils
import io.rippledown.model.*
import io.rippledown.model.condition.GreaterThanOrEqualTo
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Unchanged
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ServerApplicationTest {
    private lateinit var app: ServerApplication

    @BeforeTest
    fun setup() {
        app = ServerApplication()
        FileUtils.cleanDirectory(app.casesDir)
        FileUtils.cleanDirectory(app.interpretationsDir)
    }

    @Test
    fun casesDir() {
        assertEquals(app.casesDir, File("cases"))
        assertTrue(app.casesDir.exists())
    }

    @Test
    fun interpretationsDir() {
        assertEquals(app.interpretationsDir, File("interpretations"))
        assertTrue(app.interpretationsDir.exists())
    }

    @Test
    fun case() {
        setUpCaseFromFile("Case1", app)
        val retrieved = app.case("Case1")
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.get("TSH")!!.value.text, "0.667")
        assertEquals(retrieved.get("ABC")!!.value.text, "6.7")
        assertEquals(2, retrieved.data.size)
        // No rules added.
        retrieved.interpretation.conclusions().size shouldBe 0
        // Add a rule.
        val conclusion = Conclusion("ABC ok.")
        app.kb.startRuleSession(retrieved, ChangeTreeToAddConclusion(conclusion))
        app.kb.addConditionToCurrentRuleSession(GreaterThanOrEqualTo(Attribute("ABC"), 5.0))
        app.kb.commitCurrentRuleSession()
        val retrievedAgain = app.case("Case1")
        retrievedAgain.interpretation.conclusions() shouldContainExactly setOf(conclusion)
    }

    @Test
    fun `should retrieve cached case`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)
        val retrieved = app.case(id)
        val retrievedAgain = app.case(id)
        retrievedAgain shouldBeSameInstanceAs retrieved
    }

    @Test
    fun `should set the interpretation's DiffList to be empty when retrieving a case with a blank interpretation`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)

        val case = app.case(id)
        case.interpretation.verifiedText shouldBe null
        case.interpretation.textGivenByRules() shouldBe ""
        case.interpretation.diffList shouldBe DiffList(emptyList())
    }

    @Test
    fun `the DiffList should have an unchanged fragment when retrieving a case with a non-blank interpretation`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)

        val case = app.case(id)
        val text = "ABC ok."
        val conclusion = Conclusion(text)
        app.kb.startRuleSession(case, ChangeTreeToAddConclusion(conclusion))
        app.kb.commitCurrentRuleSession()

        with(app.viewableCase(id)) {
            interpretation.verifiedText shouldBe null
            interpretation.textGivenByRules() shouldBe text
            interpretation.diffList shouldBe DiffList(listOf(Unchanged(text)))
        }
    }

    @Test
    fun `should update a case's verified interpretation and return an interpretation containing a DiffList`() {
        val id = "Case1"
        val caseId = CaseId(id, id)
        setUpCaseFromFile(id, app)

        val original = app.case(id)
        original.interpretation.verifiedText shouldBe null
        original.interpretation.textGivenByRules() shouldBe ""

        val verifiedInterpretation = Interpretation(caseId, "Verified.")
        val returnedInterpretation = app.saveInterpretation(verifiedInterpretation)
        val updated = app.case(id)
        updated.interpretation shouldBe returnedInterpretation
        returnedInterpretation.diffList shouldBe DiffList(listOf(Addition("Verified.")))
    }

    @Test
    fun viewableCase() {
        setUpCaseFromFile("Case1", app)
        val retrieved = app.viewableCase("Case1")
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.rdrCase.get("ABC")!!.value.text, "6.7")
        assertEquals(2, retrieved.attributes().size)
        // No rules added.
        retrieved.interpretation.conclusions().size shouldBe 0
        // Add a rule.
        val conclusion = Conclusion("ABC ok.")
        app.kb.startRuleSession(retrieved.rdrCase, ChangeTreeToAddConclusion(conclusion))
        app.kb.addConditionToCurrentRuleSession(GreaterThanOrEqualTo(Attribute("ABC"), 5.0))
        app.kb.commitCurrentRuleSession()
        val retrievedAgain = app.viewableCase("Case1")
        retrievedAgain.interpretation.conclusions() shouldContainExactly setOf(conclusion)
    }

    @Test
    fun moveAttributeJustBelow() {
        setUpCaseFromFile("Case1", app)
        val retrieved = app.viewableCase("Case1")
        val attributesBefore = retrieved.attributes()
        attributesBefore.size shouldBe 2 // sanity
        app.moveAttributeJustBelow(attributesBefore[0], attributesBefore[1])
        // Get the case again and check that the order has been applied.
        val retrievedAfter = app.viewableCase("Case1")
        retrievedAfter.attributes()[0] shouldBe attributesBefore[1]
        retrievedAfter.attributes()[1] shouldBe attributesBefore[0]
    }

    @Test
    fun waitingCasesInfo() {
        FileUtils.cleanDirectory(app.casesDir)
        assertEquals(app.waitingCasesInfo().resourcePath, File("cases").absolutePath)
        assertEquals(app.waitingCasesInfo().count, 0)

        // Move some cases into the directory.
        setUpCaseFromFile("Case3", app)
        val ci1 = app.waitingCasesInfo()
        assertEquals(ci1.count, 1)
        assertEquals(ci1.caseIds[0].name, "Case3")

        setUpCaseFromFile("Case2", app)
        val ci2 = app.waitingCasesInfo()
        assertEquals(ci2.count, 2)
        assertEquals(ci2.caseIds[0].name, "Case2")
        assertEquals(ci2.caseIds[1].name, "Case3")
    }

    @Test
    fun createKB() {
        app.kb.name shouldBe "Thyroids"
        app.kb.containsCaseWithName("Case1") shouldBe false //sanity
        app.kb.addCase(createCase("Case1"))
        app.kb.containsCaseWithName("Case1") shouldBe true

        app.createKB()
        app.kb.name shouldBe "Thyroids"
        app.kb.containsCaseWithName("Case1") shouldBe false //kb rebuilt
    }

    @Test
    fun kbName() {
        app.kbName() shouldBe KBInfo("Thyroids")
    }

    @Test
    fun startRuleSessionToAddConclusion() {
        val id = "Case1"
        setUpCaseFromFile(id, app)
        app.kb.addCase(createCase(id))
        app.case(id).interpretation.conclusions() shouldBe emptySet()
        app.startRuleSessionToAddConclusion(id, Conclusion("Whatever"))
        app.commitCurrentRuleSession()
        app.case(id).interpretation.textGivenByRules() shouldBe "Whatever"
    }

    @Test
    fun exportKBToZip() {
        // Add a case and a rule to the KB.
        val id = "Case1"
        setUpCaseFromFile(id, app)
        app.kb.addCase(createCase(id))
        val conclusion1 = Conclusion("Whatever")
        app.startRuleSessionToAddConclusion(id, conclusion1)
        app.commitCurrentRuleSession()
        app.case(id).interpretation.textGivenByRules() shouldBe conclusion1.text

        // Get the exported KB.
        val exported = app.exportKBToZip()
        exported.name shouldBe "${app.kb.name}.zip"

        // Clear the KB
        app.createKB()
        app.kb.allCases() shouldBe emptySet()
        app.kb.ruleTree.size() shouldBe 1

        // Import the exported KB.
        app.importKBFromZip(exported.readBytes())
        app.kb.allCases().size shouldBe 1
        app.kb.ruleTree.size() shouldBe 2
        app.case(id).interpretation.textGivenByRules() shouldBe conclusion1.text
    }

    @Test
    fun importKBFromZip() {
        val zipFile = File("src/jvmTest/resources/export/KBExported.zip").toPath()
        app.kb.name shouldBe "Thyroids"
        app.kb.allCases().size shouldBe 0
        app.importKBFromZip(Files.readAllBytes(zipFile))

        app.kb.name shouldBe "Whatever"
        app.kb.allCases().size shouldBe 3
        app.kb.ruleTree.size() shouldBe 2
        val case = app.kb.getCaseByName("Case1")
        val interpretedCase = app.kb.viewableInterpretedCase(case)
        interpretedCase.interpretation.textGivenByRules() shouldBe "Glucose ok."
        app.kbName() shouldBe KBInfo("Whatever")
    }

    @Test
    fun `handle zip in bad format`() {
        val zipFile = File("src/jvmTest/resources/export/NoRootDir.zip").toPath()
        shouldThrow<IllegalArgumentException> {
            app.importKBFromZip(Files.readAllBytes(zipFile))
        }.message shouldBe "Invalid zip for KB import."
    }

    @Test
    fun `handle empty zip`() {
        val zipFile = File("src/jvmTest/resources/export/Empty.zip").toPath()
        shouldThrow<IllegalArgumentException> {
            ServerApplication().importKBFromZip(Files.readAllBytes(zipFile))
        }.message shouldBe "Invalid zip for KB import."
    }

    private fun setUpCaseFromFile(id: String, app: ServerApplication) {
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile(id), app.casesDir)
    }

    @Test
    fun startSessionToReplaceConclusion() {
        val id = "Case1"
        setUpCaseFromFile(id, app)
        app.kb.addCase(createCase(id))
        val conclusion1 = Conclusion("Whatever")
        app.startRuleSessionToAddConclusion(id, conclusion1)
        app.commitCurrentRuleSession()
        app.case(id).interpretation.textGivenByRules() shouldBe conclusion1.text
        val conclusion2 = Conclusion("Blah")
        app.startRuleSessionToReplaceConclusion(id, conclusion1, conclusion2)
        app.commitCurrentRuleSession()
        app.case(id).interpretation.textGivenByRules() shouldBe conclusion2.text
    }

    @Test
    fun `when saving an interpretation, an interpretation with diffs should be returned`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)
        val conclusion = Conclusion("Go to Bondi.")
        with(app) {
            kb.addCase(createCase(id))
            startRuleSessionToAddConclusion(id, conclusion)
            commitCurrentRuleSession()
            val case = case(id)
            case.interpretation.latestText() shouldBe conclusion.text

            val verifiedInterpretation = case.interpretation.apply {
                verifiedText = "Go to Bondi. Bring 2 sets of flippers. And bring sunscreen."
            }
            val interpReturned = app.saveInterpretation(verifiedInterpretation)
            interpReturned.diffList shouldBe DiffList(
                listOf(
                    Unchanged("Go to Bondi."),
                    Addition("Bring 2 sets of flippers."),
                    Addition("And bring sunscreen."),
                )
            )
        }
    }

    private fun createCase(caseName: String) = CaseTestUtils.createCase(caseName)
}