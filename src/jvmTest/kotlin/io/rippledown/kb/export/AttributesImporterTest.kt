package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import java.io.File
import kotlin.test.Test

class AttributesImporterTest : ExporterTestBase() {

    @Test
    fun exportEmpty() {
        val textFile = File(tempDir,"Attributes.txt")
        AttributesExporter(textFile, emptySet()).export()
        val recovered = AttributesImporter(textFile).import()
        recovered shouldBe emptyMap()
    }

    @Test
    fun exportImport() {
        val tsh = Attribute(100, "TSH")
        val ft3 = Attribute(200, "FT3")
        val ft4 = Attribute(300, "FT4")
        val attributeSet = setOf(tsh, ft4, ft3)
        val textFile = File(tempDir,"Attributes.txt")
        AttributesExporter(textFile, attributeSet).export()
        val recovered = AttributesImporter(textFile).import()
        recovered.size shouldBe 3
        recovered[tsh.id] shouldBe tsh
        recovered[ft3.id] shouldBe ft3
        recovered[ft4.id] shouldBe ft4
    }
}