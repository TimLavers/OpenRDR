package io.rippledown.server

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
import kotlin.test.*

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