package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import java.io.File
import kotlin.test.Test

class CaseViewImporterTest : ExporterTestBase() {

    @Test
    fun exportEmpty() {
        val textFile = File(tempDir,"Attributes.txt")
        CaseViewExporter(textFile, emptyList()).export()
        val recovered = CaseViewImporter(textFile).import()
        recovered shouldBe emptyList()
    }

    @Test
    fun exportImport() {
        val tsh = Attribute("TSH")
        val ft3 = Attribute("FT3")
        val ft4 = Attribute("FT4")
        val attributeList = listOf(tsh, ft4, ft3)
        val textFile = File(tempDir,"Attributes.txt")
        CaseViewExporter(textFile, attributeList).export()
        val recovered = CaseViewImporter(textFile).import()
        recovered shouldBe attributeList
    }
}