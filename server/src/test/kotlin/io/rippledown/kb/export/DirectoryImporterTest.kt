package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.CommentFactory
import io.rippledown.model.DummyConditionFactory
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.persistence.PersistentRule
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class DirectoryImporterTest: ExporterTestBase() {
    private lateinit var commentFactory: CommentFactory
    private lateinit var conditionFactory: DummyConditionFactory
    private lateinit var tree: RuleTree

    @BeforeEach
    override fun init() {
        super.init()
        commentFactory = CommentFactory()
        conditionFactory = DummyConditionFactory()
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
        tree = ruleTree(commentFactory) {
            child {
                id = 51
                comment { "ConclusionA" }
                condition(conditionFactory) {
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
        tree = ruleTree(commentFactory) {
            child {
                id = 51
                comment { "ConclusionA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = 456
                    comment { "ConclusionA" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        id = 457
                        comment { "ConclusionB" }
                        condition(conditionFactory) {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    id = 458
                    comment { "ConclusionD" }
                    condition(conditionFactory) {
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
            DirectoryImporter(textFile, RuleExporter())
        }.message shouldBe "$textFile is not an existing directory."
    }

    @Test
    fun `empty directory if that is allowed`() {
        shouldNotThrow<IllegalArgumentException>{
            DirectoryImporter(tempDir, RuleExporter(), true)
        }
    }

    @Test
    fun `empty directory if that is not allowed`() {
        shouldThrow<IllegalArgumentException>{
            DirectoryImporter(tempDir, RuleExporter())
        }.message shouldBe "$tempDir is empty."
    }

    private fun exportImport(): Set<PersistentRule> {
        IdentifiedObjectExporter(tempDir, RuleSource(tree)).export()
        return DirectoryImporter(tempDir, RuleExporter()).import()
    }
}