package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.KBInfo
import java.io.File
import kotlin.test.Test

class KBInfoImporterTest : ExporterTestBase() {

    @Test
    fun exportImport() {
        val id = "abc123"
        val name = "Stuff"
        val kbInfo = KBInfo(id, name)
        // Export and import.
        val textFile = File(tempDir,"Stuff.txt")
        KBInfoExporter(ExportFile(textFile, "KBInfo"), kbInfo).export()
        val rebuilt = KBInfoImporter(textFile).import()
        rebuilt.id shouldNotBe kbInfo.id
        rebuilt.name shouldBe kbInfo.name
    }

    @Test
    fun exportImportWithBlankId() {
        val name = "Stuff"
        val kbInfo = KBInfo(name)
        // Export and import.
        val textFile = File(tempDir,"Stuff.txt")
        KBInfoExporter(ExportFile(textFile, "KBInfo"), kbInfo).export()
        val rebuilt = KBInfoImporter(textFile).import()
        rebuilt.id shouldNotBe kbInfo.id
        rebuilt.name shouldBe kbInfo.name
    }
}