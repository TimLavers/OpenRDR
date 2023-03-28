package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import java.io.File
import kotlin.test.Test

class KBInfoImporterTest : ExporterTestBase() {

    @Test
    fun exportImport() {
        val id = "abc 123"
        val name = "Stuff"
        val kbInfo = KBInfo(id, name)
        // Export and import.
        val textFile = File(tempDir,"Stuff.txt")
        KBInfoExporter(ExportFile(textFile, "KBInfo"), kbInfo).export()
        val rebuilt = KBInfoImporter(textFile).import()
        rebuilt.id shouldBe kbInfo.id
        rebuilt.name shouldBe kbInfo.name
    }

    @Test
    fun exportImportWithBlankId() {
        val id = ""
        val name = "Stuff"
        val kbInfo = KBInfo(id, name)
        // Export and import.
        val textFile = File(tempDir,"Stuff.txt")
        KBInfoExporter(ExportFile(textFile, "KBInfo"), kbInfo).export()
        val rebuilt = KBInfoImporter(textFile).import()
        rebuilt.id shouldBe kbInfo.id
        rebuilt.name shouldBe kbInfo.name
    }
}