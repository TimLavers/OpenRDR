package io.rippledown.server

import io.rippledown.CaseTestUtils
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ServerApplicationTest {

    @Test
    fun casesDir() {
        val app = ServerApplication()
        assertEquals(app.casesDir, File("temp/cases"))
        assertTrue(app.casesDir.exists())
    }

    @Test
    fun case() {
        val app = ServerApplication()
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case1"), app.casesDir)
        val retrieved = app.case("Case1")
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.caseData["TSH"], "0.667")
        assertEquals(retrieved.caseData["ABC"], "6.7")
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