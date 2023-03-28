package io.rippledown.kb.export

import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import java.io.File
import kotlin.test.Test

class KBInfoExporterTest : ExporterTestBase() {

    @Test
    fun export() {
        val textFile = File(tempDir,"KBInfo.txt")
        val exportFile = ExportFile(textFile, "Whatever")
        val id = "abc 123"
        val name = "Stuff"
        KBInfoExporter(exportFile, KBInfo(id, name)).export()
        textFile.exists() shouldBe true
        textFile.length() shouldBeGreaterThan 0
    }
}