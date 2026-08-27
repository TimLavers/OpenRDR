package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.CommentFactory
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.isHigh
import io.rippledown.model.rule.Rule
import io.rippledown.persistence.PersistentRule
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.test.Test
import kotlin.text.Charsets.UTF_8

class ExportedRuleTest: ExporterTestBase() {
    private val id123 = 10123
    private val id0 = 100
    private val commentFactory = CommentFactory()
    private val comment1 = commentFactory.comment("A trip to the beach is advised.")
    private val ft3 = Attribute(300, "FT3")
    private val tshHigh = isHigh(100, tsh, "")
    private val ft3GT2 = greaterThanOrEqualTo(200, ft3, 2.0)

    @Test
    fun constructor1() {
        val er = ExportedRule(PersistentRule())
        er.persistentRule.id shouldBe null
        er.persistentRule.parentId shouldBe null
        er.persistentRule.assignment shouldBe null
        er.persistentRule.conditionIds shouldBe emptySet()
    }

    @Test
    fun constructor2() {
        val pr = PersistentRule(2, 1, setOf(4, 5), comment1)
        val er = ExportedRule(pr)
        er.persistentRule.id shouldBe 2
        er.persistentRule.parentId shouldBe 1
        er.persistentRule.assignment shouldBe comment1
        er.persistentRule.conditionIds shouldBe setOf(4, 5)
    }

    @Test
    fun exportTest() {
        val root = Rule(id0, null, setOf())
        val rule = Rule(id123, root, setOf(tshHigh, ft3GT2), mutableSetOf(), comment1)
        val er = ExportedRule(rule)
        val file = File(tempDir, "rule.json")
        er.export(file)

        val stored = FileUtils.readFileToString(file, UTF_8)
        val restored: PersistentRule = Json.decodeFromString(stored)
        restored shouldBe er.persistentRule
    }
}