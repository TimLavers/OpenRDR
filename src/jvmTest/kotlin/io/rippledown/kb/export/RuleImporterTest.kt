package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.dsl.ruleTree
import org.junit.Before
import java.io.File
import kotlin.test.Test

class RuleImporterTest: ExporterTestBase() {
    private lateinit var tree: RuleTree

    @Before
    override fun init() {
        super.init()
        tempDir.mkdirs()
        tree = RuleTree()
    }

    @Test
    fun `import size 1 tree`() {
        val rebuilt = exportImport()
        rebuilt.size() shouldBe 1
        rebuilt.root.structurallyEqual(tree.root) shouldBe true
    }

    @Test
    fun `import size 2 tree`() {
        tree = ruleTree {
            child {
                id = "c1"
                conclusion { "ConclusionA" }
                condition {
                    attributeName = clinicalNotes.name
                    constant = "a"
                }
            }
        }.build()
        val rebuilt = exportImport()
        rebuilt.size() shouldBe 2
        rebuilt.root.structurallyEqual(tree.root) shouldBe true
    }

    @Test
    fun `import complex tree`() {
        tree = ruleTree {
            child {
                id = "c1"
                conclusion { "ConclusionA" }
                condition {
                    attributeName = clinicalNotes.name
                    constant = "a"
                }
                child {
                    id = "c11"
                    conclusion { "ConclusionA" }
                    condition {
                        attributeName = clinicalNotes.name
                        constant = "b"
                    }
                    child {
                        id = "c111"
                        conclusion { "ConclusionB" }
                        condition {
                            attributeName = clinicalNotes.name
                            constant = "c"
                        }
                    }
                }
                child {
                    id = "c12"
                    conclusion { "ConclusionD" }
                    condition {
                        attributeName = clinicalNotes.name
                        constant = "d"
                    }
                }
            }
        }.build()
        val rebuilt = exportImport()
        rebuilt.size() shouldBe 5
        tree.rules().forEach {
            val rebuiltRule = rebuilt.rulesMatching { x ->
                x.id == it.id
            }.first()
            rebuiltRule.structurallyEqual(it) shouldBe true
        }
    }

    @Test
    fun `source should be an existing directory`() {
        val textFile = writeFileInDirectory(tempDir)
        shouldThrow<IllegalArgumentException>{
            RuleImporter(textFile)
        }.message shouldBe "$textFile is not an existing directory."
    }

    @Test
    fun `empty directory`() {
        shouldThrow<IllegalArgumentException>{
            RuleImporter(tempDir)
        }.message shouldBe "$tempDir is empty."
    }

    @Test
    fun `destination should be exist`() {
        val directory = File(tempDir, "exportDir")
        shouldThrow<IllegalArgumentException>{
            RuleExporter(directory, tree)
        }.message shouldBe "Rule export destination is not an existing directory."
    }

    private fun exportImport(): RuleTree {
        RuleExporter(tempDir, tree).export()
        return RuleImporter(tempDir).import()
    }
}