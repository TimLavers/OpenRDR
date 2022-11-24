package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.CaseTestUtils
import io.rippledown.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.junit.Before
import java.io.File
import java.nio.charset.Charset
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

class CaseExporterTest {
    private var tempDir: File = createTempDirectory().toFile()

    @Before
    fun init() {
        tempDir.mkdirs()
    }

    @Test
    fun `destination should be a directory`() {
        val textFile = writeFileInDirectory(tempDir)
        shouldThrow<IllegalArgumentException>{
            CaseExporter(textFile, emptySet())
        }.message shouldBe "Case export destination is not a directory"
    }

    @Test
    fun `destination should be empty`() {
        val directory = File(tempDir, "exportDir")
        directory.mkdirs()
        writeFileInDirectory(directory)
        shouldThrow<IllegalArgumentException>{
            CaseExporter(directory, emptySet())
        }.message shouldBe "Case export directory is not empty"
    }

    @Test
    fun `destination should be exist`() {
        val directory = File(tempDir, "exportDir")
        writeFileInDirectory(directory)
        shouldThrow<IllegalArgumentException>{
            CaseExporter(directory, emptySet())
        }.message shouldBe "Case export directory does not exist"
    }

    @Test
    fun `each case is in its own file`() {
        val case1 = CaseTestUtils.createCase("Case1")
        val case2 = CaseTestUtils.createCase("Case2")
        val case3 = CaseTestUtils.createCase("Case3")
        CaseExporter(tempDir, setOf(case1, case2, case3)).export()
        checkNamedFileContainsDataForCase("Case1", case1)
        checkNamedFileContainsDataForCase("Case2", case2)
        checkNamedFileContainsDataForCase("Case3", case3)
    }

    @Test
    fun `cases with names that would be illegal as file names can be exported`() {
        val case1 = CaseTestUtils.createCase(">>Cat<<")
        val case2 = CaseTestUtils.createCase(">>CAT<<")
        CaseExporter(tempDir, setOf(case1, case2)).export()
        checkNamedFileContainsDataForCase("__Cat__", case1)
        checkNamedFileContainsDataForCase("__CAT__2", case2)
    }

    private fun checkNamedFileContainsDataForCase(filename: String, case: RDRCase) {
        val file = File(tempDir, "$filename.json")
        val data = FileUtils.readFileToString(file, Charset.defaultCharset())
        val format = Json { allowStructuredMapKeys = true }
        val deserialized = format.decodeFromString<RDRCase>(data)
        case.data shouldBe deserialized.data
        case.name shouldBe deserialized.name
    }

    private fun writeFileInDirectory(file: File): File {
        val textFile = File(file, "blah.txt")
        FileUtils.writeStringToFile(textFile, "Whatever", Charset.defaultCharset())
        return textFile
    }
}