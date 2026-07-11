package io.rippledown.kb.export

import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.test.Test

class KBInfoExporterTest : ExporterTestBase() {

    @Test
    fun export() {
        val textFile = tempDir.resolve("KBInfo.txt")
        val exportFile = ExportFile(textFile, "Whatever")
        val id = "abc123"
        val name = "Stuff"
        KBInfoExporter(exportFile, KBInfo(id, name)).export()
        textFile.exists() shouldBe true
        textFile.fileSize() shouldBeGreaterThan 0
    }
}