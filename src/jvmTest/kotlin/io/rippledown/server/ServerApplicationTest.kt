package io.rippledown.server

import io.rippledown.CaseTestUtils
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import org.apache.commons.io.FileUtils
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
        assertEquals(app.casesDir, File("temp/cases"))
        assertTrue(app.casesDir.exists())
    }

    @Test
    fun interpretationsDir() {
        val app = ServerApplication()
        assertEquals(app.interpretationsDir, File("temp/interpretations"))
        assertTrue(app.interpretationsDir.exists())
    }

    @Test
    fun saveInterpretationDeletesCase() {
        val app = ServerApplication()
        val caseId = CaseId("Case1", "Case1")
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case1"), app.casesDir)
        val case1File = File(app.casesDir, "Case1.json")
        assertTrue(case1File.exists())
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        app.saveInterpretation(interpretation)
        assertFalse(case1File.exists())
    }
    @Test
    fun saveInterpretation() {
        val app = ServerApplication()
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case1"), app.casesDir)
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
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case1"), app.casesDir)
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
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case1"), app.casesDir)
        val retrieved = app.case("Case1")
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.latestEpisode()["TSH"]!!.value.text, "0.667")
        assertEquals(retrieved.latestEpisode()["ABC"]!!.value.text, "6.7")
        assertEquals(retrieved.caseData.size, 2)
    }

    @Test
    fun waitingCasesInfo() {
        val app = ServerApplication()
        FileUtils.cleanDirectory(app.casesDir)
        assertEquals(app.waitingCasesInfo().resourcePath, File("temp/cases").absolutePath)
        assertEquals(app.waitingCasesInfo().count, 0)

        // Move some cases into the directory.
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case3"), app.casesDir)
        val ci1 = app.waitingCasesInfo()
        assertEquals(ci1.count, 1)
        assertEquals(ci1.caseIds[0].name, "Case3")

        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case2"), app.casesDir)
        val ci2 = app.waitingCasesInfo()
        assertEquals(ci2.count, 2)
        assertEquals(ci2.caseIds[0].name, "Case2")
        assertEquals(ci2.caseIds[1].name, "Case3")
    }
}