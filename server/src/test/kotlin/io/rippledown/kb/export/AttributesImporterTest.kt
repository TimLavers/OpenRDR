package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlin.test.Test

class AttributesImporterTest : ExporterTestBase() {

    @Test
    fun exportEmpty() {
        val textFile = tempDir.resolve("Attributes.txt")
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
        val textFile = tempDir.resolve("Attributes.txt")
        AttributesExporter(textFile, attributeSet).export()
        val recovered = AttributesImporter(textFile).import()
        recovered.size shouldBe 3
        recovered[tsh.id] shouldBe tsh
        recovered[ft3.id] shouldBe ft3
        recovered[ft4.id] shouldBe ft4
    }

    @Test
    fun `handle spaces in attribute names`() {
        val tsh = Attribute(100, "Thyroid Stimulating Hormone")
        val ft3 = Attribute(200, "Free T3")
        val ft4 = Attribute(300, "Free T4")
        val attributeSet = setOf(tsh, ft4, ft3)
        val textFile = tempDir.resolve("Attributes.txt")
        AttributesExporter(textFile, attributeSet).export()
        val recovered = AttributesImporter(textFile).import()
        recovered.size shouldBe 3
        recovered[tsh.id] shouldBe tsh
        recovered[tsh.id]!!.name shouldBe tsh.name
        recovered[ft3.id] shouldBe ft3
        recovered[ft3.id]!!.name shouldBe ft3.name
        recovered[ft4.id] shouldBe ft4
        recovered[ft4.id]!!.name shouldBe ft4.name
    }
}