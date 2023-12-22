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
        val recovered = CaseViewImporter(textFile, emptyMap()).import()
        recovered shouldBe emptyList()
    }

    @Test
    fun exportImport() {
        val tsh = Attribute(100, "TSH")
        val ft3 = Attribute(200, "FT3")
        val ft4 = Attribute(300, "FT4")
        val attributeList = listOf(tsh, ft4, ft3)
        val textFile = File(tempDir,"Attributes.txt")
        CaseViewExporter(textFile, attributeList).export()
        val attributeMap = attributeList.map { Pair(it.id, it) }.toMap()
        val recovered = CaseViewImporter(textFile, attributeMap).import()
        recovered shouldBe attributeList
    }
}