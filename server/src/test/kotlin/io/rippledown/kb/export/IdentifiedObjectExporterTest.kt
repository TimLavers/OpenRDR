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
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test

class IdentifiedObjectExporterTest: ExporterTestBase() {
    private lateinit var tree: RuleTree
    private lateinit var conclusionFactory: DummyConclusionFactory
    private lateinit var conditionFactory: ConditionFactory

    @BeforeEach
    override fun init() {
        super.init()
        conclusionFactory = DummyConclusionFactory()
        conditionFactory = DummyConditionFactory()
        tempDir.createDirectories()
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
        val directory = tempDir.resolve("exportDir")
        directory.createDirectories()
        writeFileInDirectory(directory)
        shouldThrow<IllegalArgumentException>{
            IdentifiedObjectExporter(directory, RuleSource(tree))
        }.message shouldBe "Rule export directory is not empty."
    }

    @Test
    fun `destination should be exist`() {
        val directory = tempDir.resolve("exportDir")
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
            val file = tempDir.resolve("${it.id}.json")
            val data = Files.readString(file)
            val persistentRule: PersistentRule = Json.decodeFromString(data)
            persistentRule.id shouldBe it.id
            persistentRule.parentId shouldBe it.parent?.id
            persistentRule.conclusionId shouldBe it.conclusion?.id
            persistentRule.conditionIds shouldBe  it.conditions.map { it.id!! }.toSet()
        }
    }
}