package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.DummyConclusionFactory
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.persistence.PersistentRule
import org.junit.Before
import java.io.File
import kotlin.test.Test

class RuleImporterTest: ExporterTestBase() {
    private lateinit var conclusionFactory: DummyConclusionFactory
    private lateinit var tree: RuleTree

    @Before
    override fun init() {
        super.init()
        conclusionFactory = DummyConclusionFactory()
        tempDir.mkdirs()
        tree = RuleTree()
    }

    @Test
    fun `import size 1 tree`() {
        val rebuilt = exportImport()
        rebuilt.size shouldBe 1
        rebuilt.single() shouldBe  PersistentRule(tree.root)
    }

    @Test
    fun `import size 2 tree`() {
        tree = ruleTree(conclusionFactory) {
            child {
                id = 51
                conclusion { "ConclusionA" }
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
        }.build()
        val rebuilt = exportImport()
        rebuilt.size shouldBe 2
        val expected = tree.rules().map { PersistentRule(it) }.toSet()
        rebuilt shouldBe expected
    }

    @Test
    fun `import complex tree`() {
        tree = ruleTree(conclusionFactory) {
            child {
                id = 51
                conclusion { "ConclusionA" }
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = 456
                    conclusion { "ConclusionA" }
                    condition {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        id = 457
                        conclusion { "ConclusionB" }
                        condition {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    id = 458
                    conclusion { "ConclusionD" }
                    condition {
                        attribute = clinicalNotes
                        constant = "d"
                    }
                }
            }
        }.build()
        val rebuilt = exportImport()
        rebuilt.size shouldBe 5
        val expected = tree.rules().map {PersistentRule(it)}.toSet()
        rebuilt shouldBe expected
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

    private fun exportImport(): Set<PersistentRule> {
        RuleExporter(tempDir, tree).export()
        return RuleImporter(tempDir).import()
    }
}