package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.CaseTestUtils
import io.rippledown.model.RDRCase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.Charset
import kotlin.test.Test

class CaseExporterTest : ExporterTestBase() {

    @Test
    fun `destination should be a directory`() {
        val textFile = writeFileInDirectory(tempDir)
        shouldThrow<IllegalArgumentException>{
            CaseExporter(textFile, emptyList())
        }.message shouldBe "Case export destination is not a directory."
    }

    @Test
    fun `destination should be empty`() {
        val directory = File(tempDir, "exportDir")
        directory.mkdirs()
        writeFileInDirectory(directory)
        shouldThrow<IllegalArgumentException>{
            CaseExporter(directory, emptyList())
        }.message shouldBe "Case export directory is not empty."
    }

    @Test
    fun `destination should exist`() {
        val directory = File(tempDir, "exportDir")
        shouldThrow<IllegalArgumentException>{
            CaseExporter(directory, emptyList())
        }.message shouldBe "Case export destination is not an existing directory."
    }

    @Test
    fun `each case is in its own file`() {
        val case1 = CaseTestUtils.createCase("Case1")
        val case2 = CaseTestUtils.createCase("Case2")
        val case3 = CaseTestUtils.createCase("Case3")
        CaseExporter(tempDir, listOf(case1, case2, case3)).export()
        checkNamedFileContainsDataForCase("Case1", case1)
        checkNamedFileContainsDataForCase("Case2", case2)
        checkNamedFileContainsDataForCase("Case3", case3)
    }

    @Test
    fun `cases with names that would be illegal as file names can be exported`() {
        val case1 = CaseTestUtils.createCase(">>Cat<<")
        val case2 = CaseTestUtils.createCase(">>CAT<<")
        CaseExporter(tempDir, listOf(case1, case2)).export()
        val filesInDir = tempDir.listFiles()!!
        val fileCat = if (filesInDir[0].name.startsWith("__Cat")) filesInDir[0] else filesInDir[1]
        val fileCAT = if (filesInDir[0].name.startsWith("__CAT")) filesInDir[0] else filesInDir[1]
        checkNamedFileContainsDataForCase(fileCat.name.split(".")[0], case1)
        checkNamedFileContainsDataForCase(fileCAT.name.split(".")[0], case2)
    }

    private fun checkNamedFileContainsDataForCase(filename: String, case: RDRCase) {
        val file = File(tempDir, "$filename.json")
        val data = FileUtils.readFileToString(file, Charset.defaultCharset())
        val format = Json { allowStructuredMapKeys = true }
        val deserialized = format.decodeFromString<RDRCase>(data)
        case.data shouldBe deserialized.data
        case.name shouldBe deserialized.name
    }
}