package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.dsl.ruleTree
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.junit.Before
import java.io.File
import kotlin.test.Test
import kotlin.text.Charsets.UTF_8

class RuleExporterTest: ExporterTestBase() {
    private lateinit var tree: RuleTree

    @Before
    override fun init() {
        super.init()
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
                conclusion { "ConclusionA" }
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = "c11"
                    conclusion { "ConclusionA" }
                    condition {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        id = "c111"
                        conclusion { "ConclusionB" }
                        condition {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    id = "c12"
                    conclusion { "ConclusionD" }
                    condition {
                        attribute = clinicalNotes
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
}