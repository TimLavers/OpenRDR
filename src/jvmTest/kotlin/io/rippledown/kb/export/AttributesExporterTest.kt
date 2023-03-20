package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import java.io.File
import kotlin.test.Test

class AttributesExporterTest : ExporterTestBase() {

    @Test
    fun `destination should be a file`() {
        shouldThrow<IllegalArgumentException>{
            AttributesExporter(tempDir, emptySet())
        }.message shouldBe "Attributes export destination ${tempDir.name} is not a file."
    }

    @Test
    fun `destination should be empty`() {
        val file = writeFileInDirectory(tempDir)
        shouldThrow<IllegalArgumentException>{
            AttributesExporter(file, emptySet())
        }.message shouldBe "Attributes export file ${file.name} is not empty."
    }

    @Test
    fun exportEmpty() {
        val textFile = File(tempDir,"Attributes.txt")
        AttributesExporter(textFile, emptySet()).export()
        textFile.exists() shouldBe true
    }

    @Test
    fun exportMultiple() {
        val tsh = Attribute("TSH", 100)
        val ft3 = Attribute("FT3", 200)
        val ft4 = Attribute("FT4", 300)
        val attributes = setOf(tsh, ft4, ft3)
        val textFile = File(tempDir,"Attributes.txt")
        AttributesExporter(textFile, attributes).export()
        textFile.length() shouldBeGreaterThan 0
    }
}