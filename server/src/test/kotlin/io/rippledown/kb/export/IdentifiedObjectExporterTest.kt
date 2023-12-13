package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.ConditionFactory
import io.rippledown.model.DummyConclusionFactory
import io.rippledown.model.DummyConditionFactory
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.persistence.PersistentRule
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.BeforeEach

import java.io.File
import kotlin.test.Test
import kotlin.text.Charsets.UTF_8

class IdentifiedObjectExporterTest: ExporterTestBase() {
    private lateinit var tree: RuleTree
    private lateinit var conclusionFactory: DummyConclusionFactory
    private lateinit var conditionFactory: ConditionFactory

    @BeforeEach
    override fun init() {
        super.init()
        conclusionFactory = DummyConclusionFactory()
        conditionFactory = DummyConditionFactory()
        tempDir.mkdirs()
        tree = RuleTree()
    }

    @Test
    fun `destination should be a directory`() {
        val textFile = writeFileInDirectory(tempDir)
        shouldThrow<IllegalArgumentException>{
            IdentifiedObjectExporter(textFile, RuleSource(tree))
        }.message shouldBe "Rule export destination is not a directory."
    }

    @Test
    fun `destination should be empty`() {
        val directory = File(tempDir, "exportDir")
        directory.mkdirs()
        writeFileInDirectory(directory)
        shouldThrow<IllegalArgumentException>{
            IdentifiedObjectExporter(directory, RuleSource(tree))
        }.message shouldBe "Rule export directory is not empty."
    }

    @Test
    fun `destination should be exist`() {
        val directory = File(tempDir, "exportDir")
        shouldThrow<IllegalArgumentException>{
            IdentifiedObjectExporter(directory, RuleSource(tree))
        }.message shouldBe "Rule export destination is not an existing directory."
    }

    @Test
    fun `each rule is in its own file`() {
        tree = ruleTree(conclusionFactory) {
            child {
                id = 34
                conclusion { "ConclusionA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = 134
                    conclusion { "ConclusionA" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        id = 111
                        conclusion { "ConclusionB" }
                        condition(conditionFactory) {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    id = 12
                    conclusion { "ConclusionD" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "d"
                    }
                }
            }
        }.build()
        tree.rules().size shouldBe 5
        IdentifiedObjectExporter(tempDir, RuleSource(tree)).export()
        tree.rules().forEach { it ->
            val file = File(tempDir, "${it.id}.json")
            val data = FileUtils.readFileToString(file, UTF_8)
            val persistentRule: PersistentRule = Json.decodeFromString(data)
            persistentRule.id shouldBe it.id
            persistentRule.parentId shouldBe it.parent?.id
            persistentRule.conclusionId shouldBe it.conclusion?.id
            persistentRule.conditionIds shouldBe  it.conditions.map { it.id!! }.toSet()
        }
    }
}