package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.rippledown.model.Attribute
import io.rippledown.model.CommentFactory
import io.rippledown.model.ConditionFactory
import io.rippledown.model.DummyConditionFactory
import io.rippledown.model.condition.isHigh
import io.rippledown.model.condition.isNormal
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.persistence.PersistentRule
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class RuleExporterTest: ExporterTestBase() {
    private lateinit var commentFactory: CommentFactory
    private lateinit var conditionFactory: ConditionFactory
    private lateinit var rule: Rule

    @BeforeEach
    override fun init() {
        super.init()
        commentFactory = CommentFactory()
        conditionFactory = DummyConditionFactory()
        val glucose = Attribute(9, "Glucose")
        val tsh = Attribute(10, "TSH")
        val glucoseHigh = conditionFactory.getOrCreate(isHigh(null, glucose, ""))
        val tshNormal = conditionFactory.getOrCreate(isNormal(null, tsh))
        val parent = Rule(0, null, emptySet())
        rule = Rule(
            8,
            parent,
            setOf(glucoseHigh, tshNormal),
            mutableSetOf(),
            commentFactory.comment("More coffee needed.")
        )
    }

    @Suppress("JSON_FORMAT_REDUNDANT")
    @Test
    fun exportToString() {
        val serialized = RuleExporter().exportToString(rule)
        val deserialized = Json { allowStructuredMapKeys = true }.decodeFromString<PersistentRule>(serialized)
        deserialized shouldBe PersistentRule(rule)
    }

    @Suppress("JSON_FORMAT_REDUNDANT")
    @Test
    fun importFromString() {
        val serialized = RuleExporter().exportToString(rule)
        val deserialized = RuleExporter().importFromString(serialized)
        deserialized shouldBe PersistentRule(rule)
    }
}
class RuleSourceTest: ExporterTestBase() {
    private lateinit var tree: RuleTree
    private lateinit var commentFactory: CommentFactory
    private lateinit var conditionFactory: ConditionFactory

    @BeforeEach
    override fun init() {
        super.init()
        commentFactory = CommentFactory()
        conditionFactory = DummyConditionFactory()
    }

    @Test
    fun all() {
        tree = ruleTree(commentFactory) {
            child {
                id = 34
                comment { "ConclusionA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = 134
                    comment { "ConclusionA" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        id = 111
                        comment { "ConclusionB" }
                        condition(conditionFactory) {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    id = 12
                    comment { "ConclusionD" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "d"
                    }
                }
            }
        }.build()
        val ruleSource = RuleSource(tree)
        ruleSource.all() shouldBe tree.rules()
    }

    @Test
    fun exportType() {
        tree = RuleTree()
        RuleSource(tree).exportType() shouldBe "Rule"
    }

    @Test
    fun exporter() {
        tree = RuleTree()
        RuleSource(tree).exporter().shouldBeInstanceOf<RuleExporter>()
    }

    @Test
    fun idFor() {
        tree = RuleTree()
        val rule = Rule(99, tree.root, emptySet())
        RuleSource(tree).idFor(rule) shouldBe rule.id
    }
}