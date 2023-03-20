package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.rippledown.CaseTestUtils
import io.rippledown.model.*
import io.rippledown.model.condition.GreaterThanOrEqualTo
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import kotlin.test.*


fun RDRCase.getAttribute(attributeName: String): Attribute {
    return attributes.first { attribute -> attribute.name == attributeName }
}
fun RDRCase.getLatest(attributeName: String): TestResult? {
    return getLatest(getAttribute(attributeName))
}
internal class ServerApplicationTest {

    @BeforeTest
    fun setup() {
        val app = ServerApplication()
        FileUtils.cleanDirectory(app.casesDir)
        FileUtils.cleanDirectory(app.interpretationsDir)
    }

    @Test
    fun casesDir() {
        val app = ServerApplication()
        assertEquals(app.casesDir, File("cases"))
        assertTrue(app.casesDir.exists())
    }

    @Test
    fun interpretationsDir() {
        val app = ServerApplication()
        assertEquals(app.interpretationsDir, File("interpretations"))
        assertTrue(app.interpretationsDir.exists())
    }

    @Test
    fun saveInterpretationDeletesCase() {
        val app = ServerApplication()
        val caseId = CaseId("Case1", "Case1")
        setUpCaseFromFile("Case1", app)
        val case1File = File(app.casesDir, "Case1.json")
        assertTrue(case1File.exists())
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        app.saveInterpretation(interpretation)
        assertFalse(case1File.exists())
    }

    @Test
    fun saveInterpretation() {
        val app = ServerApplication()
        setUpCaseFromFile("Case1", app)
        val caseId = CaseId("Case1", "Case 1")
        val interpretation = Interpretation(caseId, "Whatever, blah.")

        assertEquals(app.interpretationsDir.listFiles()!!.size, 0)

        app.saveInterpretation(interpretation)
        assertEquals(app.interpretationsDir.listFiles()!!.size, 1)
        val interpretationFile = File(app.interpretationsDir, "Case1.interpretation.json")
        val data = FileUtils.readFileToString(interpretationFile, UTF_8)
        val deserialized = Json.decodeFromString<Interpretation>(data)
        assertEquals(deserialized.text, "Whatever, blah.")
        assertEquals(deserialized.caseId, caseId)

        // Save it again, with a different comment.
        setUpCaseFromFile("Case1", app)
        val interpretation2 = Interpretation(caseId, "Sure.")
        app.saveInterpretation(interpretation2)
        assertEquals(app.interpretationsDir.listFiles()!!.size, 1)
        val interpretationFile2 = File(app.interpretationsDir, "Case1.interpretation.json")
        val data2 = FileUtils.readFileToString(interpretationFile2, UTF_8)
        val deserialized2 = Json.decodeFromString<Interpretation>(data2)
        assertEquals(deserialized2.text, "Sure.")
        assertEquals(deserialized2.caseId, caseId)
    }

    @Test
    fun case() {
        val app = ServerApplication()
        setUpCaseFromFile("Case1", app)
        val retrieved = app.case("Case1")
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.getLatest("TSH")!!.value.text, "0.667")
        assertEquals(retrieved.getLatest("ABC")!!.value.text, "6.7")
        assertEquals(2, retrieved.data.size)
        // No rules added.
        retrieved.interpretation.conclusions().size shouldBe 0
        // Add a rule.
        val conclusion = Conclusion("ABC ok.")
        app.kb.startRuleSession(retrieved, ChangeTreeToAddConclusion(conclusion))
        val abc = retrieved.getAttribute("ABC")
        app.kb.addConditionToCurrentRuleSession(GreaterThanOrEqualTo(abc, 5.0))
        app.kb.commitCurrentRuleSession()
        val retrievedAgain = app.case("Case1")
        retrievedAgain.interpretation.conclusions() shouldContainExactly setOf(conclusion)
    }

    @Test
    fun viewableCase() {
        val app = ServerApplication()
        setUpCaseFromFile("Case1", app)
        val retrieved = app.viewableCase("Case1")
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.rdrCase.getLatest("ABC")!!.value.text, "6.7")
        assertEquals(2, retrieved.attributes().size)
        // No rules added.
        retrieved.interpretation.conclusions().size shouldBe 0
        // Add a rule.
        val conclusion = Conclusion("ABC ok.")
        app.kb.startRuleSession(retrieved.rdrCase, ChangeTreeToAddConclusion(conclusion))
        val abc = retrieved.rdrCase.getAttribute("ABC")
        app.kb.addConditionToCurrentRuleSession(GreaterThanOrEqualTo(abc, 5.0))
        app.kb.commitCurrentRuleSession()
        val retrievedAgain = app.viewableCase("Case1")
        retrievedAgain.interpretation.conclusions() shouldContainExactly setOf(conclusion)
    }

    @Test
    fun getOrCreateAttribute() {
        val app = ServerApplication()
        app.kb.attributeManager.all() shouldBe emptySet()
        val attribute = app.getOrCreateAttribute("stuff")
        app.kb.attributeManager.all() shouldBe setOf(attribute)
    }

    @Test
    fun moveAttributeJustBelow() {
        val app = ServerApplication()
        setUpCaseFromFile("Case1", app)
        val retrieved = app.viewableCase("Case1")
        val attributesBefore = retrieved.attributes()
        attributesBefore.size shouldBe 2 // sanity
        app.moveAttributeJustBelow(attributesBefore[0].id, attributesBefore[1].id)
        // Get the case again and check that the order has been applied.
        val retrievedAfter = app.viewableCase("Case1")
        retrievedAfter.attributes()[0] shouldBe attributesBefore[1]
        retrievedAfter.attributes()[1] shouldBe attributesBefore[0]
    }

    @Test
    fun waitingCasesInfo() {
        val app = ServerApplication()
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
        val app = ServerApplication()
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
        ServerApplication().kbName() shouldBe KBInfo("Thyroids")
    }

    @Test
    fun startRuleSessionToAddConclusion() {
        val app = ServerApplication()
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
        val app = ServerApplication()
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
        val app = ServerApplication()
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
        val app = ServerApplication()
        shouldThrow<IllegalArgumentException>{
            app.importKBFromZip(Files.readAllBytes(zipFile))
        }.message shouldBe "Invalid zip for KB import."
    }

    @Test
    fun `handle empty zip`() {
        val zipFile = File("src/jvmTest/resources/export/Empty.zip").toPath()
        shouldThrow<IllegalArgumentException>{
            ServerApplication().importKBFromZip(Files.readAllBytes(zipFile))
        }.message shouldBe "Invalid zip for KB import."
    }

    private fun setUpCaseFromFile(id: String, app: ServerApplication) {
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile(id), app.casesDir)
    }

    @Test
    fun startSessionToReplaceConclusion() {
        val app = ServerApplication()
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

    private fun createCase(caseName: String) = CaseTestUtils.createCase(caseName)
}