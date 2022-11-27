package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.RuleTestBase
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.dsl.ruleTree
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.junit.Before
import java.io.File
import java.nio.charset.Charset
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.text.Charsets.UTF_8

class RuleExporterTest: RuleTestBase() {
    private var tempDir: File = createTempDirectory().toFile()
    private lateinit var tree: RuleTree

    @Before
    fun init() {
        tempDir.mkdirs()
        tree = RuleTree()
    }

    @Test
    fun `destination should be a directory`() {
        val textFile = writeFileInDirectory(tempDir)
        shouldThrow<IllegalArgumentException>{
            RuleExporter(textFile, tree)
        }.message shouldBe "Rule export destination is not a directory."
    }

    @Test
    fun `destination should be empty`() {
        val directory = File(tempDir, "exportDir")
        directory.mkdirs()
        writeFileInDirectory(directory)
        shouldThrow<IllegalArgumentException>{
            RuleExporter(directory, tree)
        }.message shouldBe "Rule export directory is not empty."
    }

    @Test
    fun `destination should be exist`() {
        val directory = File(tempDir, "exportDir")
        shouldThrow<IllegalArgumentException>{
            RuleExporter(directory, tree)
        }.message shouldBe "Rule export destination is not an existing directory."
    }

    @Test
    fun `each rule is in its own file`() {
        tree = ruleTree {
            child {
                id = "c1"
                conclusion { "ConcA" }
                condition {
                    attributeName = clinicalNotes.name
                    constant = "a"
                }
                child {
                    id = "c11"
                    conclusion { "ConcA" }
                    condition {
                        attributeName = clinicalNotes.name
                        constant = "b"
                    }
                    child {
                        id = "c111"
                        conclusion { "ConcB" }
                        condition {
                            attributeName = clinicalNotes.name
                            constant = "c"
                        }
                    }
                }
                child {
                    id = "c12"
                    conclusion { "ConcD" }
                    condition {
                        attributeName = clinicalNotes.name
                        constant = "d"
                    }
                }
            }
        }.build()
        tree.rules().size shouldBe 5
        RuleExporter(tempDir, tree).export()
        tree.rules().forEach {
            val file = File(tempDir, "${it.id}.json")
            val data = FileUtils.readFileToString(file, UTF_8)
            val exportedRule: ExportedRule = Json.decodeFromString(data)
            exportedRule.id shouldBe it.id
            exportedRule.parentId shouldBe it.parent?.id
            exportedRule.conclusion shouldBe it.conclusion
            exportedRule.conditions shouldBe  it.conditions
        }
    }

    private fun writeFileInDirectory(file: File): File {
        val textFile = File(file, "blah.txt")
        FileUtils.writeStringToFile(textFile, "Whatever", Charset.defaultCharset())
        return textFile
    }
}