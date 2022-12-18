package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import java.io.File
import kotlin.test.Test

class AttributeExporterTest : ExporterTestBase() {

    @Test
    fun `destination should be a file`() {
        shouldThrow<IllegalArgumentException>{
            AttributeExporter(tempDir, emptyList())
        }.message shouldBe "Attribute export destination ${tempDir.name} is not a file."
    }

    @Test
    fun `destination should be empty`() {
        val file = writeFileInDirectory(tempDir)
        shouldThrow<IllegalArgumentException>{
            AttributeExporter(file, emptyList())
        }.message shouldBe "Attribute export file ${file.name} is not empty."
    }

    @Test
    fun exportEmpty() {
        val textFile = File(tempDir,"Attributes.txt")
        AttributeExporter(textFile, emptyList()).export()
        textFile.exists() shouldBe true
    }

    @Test
    fun exportMultiple() {
        val tsh = Attribute("TSH")
        val ft3 = Attribute("FT3")
        val ft4 = Attribute("FT4")
        val attributeList = listOf(tsh, ft4, ft3)
        val textFile = File(tempDir,"Attributes.txt")
        AttributeExporter(textFile, attributeList).export()
        textFile.length() shouldBeGreaterThan 0
    }
}