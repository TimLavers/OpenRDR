package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.Test

class CaseViewExporterTest : ExporterTestBase() {

    @Test
    fun `destination should be a file`() {
        shouldThrow<IllegalArgumentException>{
            CaseViewExporter(tempDir, emptyList())
        }.message shouldBe "Case view export destination ${tempDir.name} is not a file."
    }

    @Test
    fun `destination should be empty`() {
        val file = writeFileInDirectory(tempDir)
        shouldThrow<IllegalArgumentException>{
            CaseViewExporter(file, emptyList())
        }.message shouldBe "Case view export file ${file.name} is not empty."
    }

    @Test
    fun exportEmpty() {
        val textFile = tempDir.resolve("Attributes.txt")
        CaseViewExporter(textFile, emptyList()).export()
        textFile.exists() shouldBe true
    }

    @Test
    fun exportMultiple() {
        val tsh = Attribute(100, "TSH")
        val ft3 = Attribute(200, "FT3")
        val ft4 = Attribute(300, "FT4")
        val attributeList = listOf(tsh, ft4, ft3)
        val textFile = tempDir.resolve("Attributes.txt")
        CaseViewExporter(textFile, attributeList).export()
        textFile.fileSize() shouldBeGreaterThan 0
    }
}