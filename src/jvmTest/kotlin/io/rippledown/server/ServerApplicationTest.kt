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
    fun waitingCasesInfo() {
        val app = ServerApplication()
        FileUtils.cleanDirectory(app.casesDir)
        assertEquals(app.waitingCasesInfo().resourcePath, File("temp/cases").absolutePath)
        assertEquals(app.waitingCasesInfo().count, 0)

        // Move some cases into the directory.
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case3"), app.casesDir)
        assertEquals(app.waitingCasesInfo().count, 1)

        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile("Case2"), app.casesDir)
        assertEquals(app.waitingCasesInfo().count, 2)
    }
}