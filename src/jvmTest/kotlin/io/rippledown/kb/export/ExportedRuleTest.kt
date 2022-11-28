package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.GreaterThanOrEqualTo
import io.rippledown.model.condition.IsHigh
import io.rippledown.model.rule.Rule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.test.Test
import kotlin.text.Charsets.UTF_8

class ExportedRuleTest: ExporterTestBase() {
    private val id123 = "r123"
    private val id0 = "r0"
    private val conclusion1 = Conclusion("A trip to the beach is advised.")
    private val ft3 = Attribute("FT3")
    private val tshHigh = IsHigh(tsh)
    private val ft3GT2 = GreaterThanOrEqualTo(ft3, 2.0)

    @Test
    fun constructor1() {
        val er = ExportedRule(id123)
        er.id shouldBe id123
        er.parentId shouldBe null
        er.conclusion shouldBe null
        er.conditions shouldBe emptySet()
        er shouldBe serializeDeserialize(er)
    }

    @Test
    fun constructor2() {
        val er = ExportedRule(id123, id0)
        er.id shouldBe id123
        er.parentId shouldBe id0
        er.conclusion shouldBe null
        er.conditions shouldBe emptySet()
        er shouldBe serializeDeserialize(er)
    }

    @Test
    fun constructor3() {
        val er = ExportedRule(id123, id0, conclusion1)
        er.id shouldBe id123
        er.parentId shouldBe id0
        er.conclusion shouldBe conclusion1
        er.conditions shouldBe emptySet()
        er shouldBe serializeDeserialize(er)
    }

    @Test
    fun constructor4() {
        val conditions = setOf(tshHigh, ft3GT2)
        val er = ExportedRule(id123, id0, conclusion1, conditions)
        er.id shouldBe id123
        er.parentId shouldBe id0
        er.conclusion shouldBe conclusion1
        er.conditions shouldBe conditions
        er shouldBe serializeDeserialize(er)
    }

    @Test
    fun constructFromRule() {
        val rule = Rule(id0, null, null, setOf())
        val er = ExportedRule(rule)
        er.id shouldBe id0
        er.parentId shouldBe null
        er.conclusion shouldBe null
        er.conditions shouldBe emptySet()
        er shouldBe serializeDeserialize(er)
    }

    @Test
    fun constructFromRule2() {
        val root = Rule(id0, null, null, setOf())
        val rule = Rule(id123, root, null)
        val er = ExportedRule(rule)
        er.id shouldBe id123
        er.parentId shouldBe id0
        er.conclusion shouldBe null
        er.conditions shouldBe emptySet()
        er shouldBe serializeDeserialize(er)
    }

    @Test
    fun constructFromRule3() {
        val root = Rule(id0, null, null, setOf())
        val conditions = setOf(tshHigh, ft3GT2)
        val rule = Rule(id123, root, null, conditions)
        val er = ExportedRule(rule)
        er.id shouldBe id123
        er.parentId shouldBe id0
        er.conclusion shouldBe null
        er.conditions shouldBe conditions
        er shouldBe serializeDeserialize(er)
    }

    @Test
    fun constructFromRule4() {
        val root = Rule(id0, null, null, setOf())
        val conditions = setOf(tshHigh, ft3GT2)
        val rule = Rule(id123, root, conclusion1, conditions)
        val er = ExportedRule(rule)
        er.id shouldBe id123
        er.parentId shouldBe id0
        er.conclusion shouldBe conclusion1
        er.conditions shouldBe conditions
        er shouldBe serializeDeserialize(er)
    }

    @Test
    fun constructFromRule5() {
        val root = Rule(id0, null, null, setOf())
        val conditions = setOf(tshHigh, ft3GT2)
        val child1 = Rule("id345", null, conclusion1, setOf(tshHigh))
        val child2 = Rule("id456", null, null, setOf(ft3GT2))
        val childRules = mutableSetOf(child1, child2)
        val rule = Rule(id123, root, conclusion1, conditions, childRules)
        val er = ExportedRule(rule)
        er.id shouldBe id123
        er.parentId shouldBe id0
        er.conclusion shouldBe conclusion1
        er.conditions shouldBe conditions
        er shouldBe serializeDeserialize(er)
    }

    @Test
    fun exportTest() {
        val root = Rule(id0, null, null, setOf())
        val rule = Rule(id123, root, conclusion1, setOf(tshHigh, ft3GT2))
        val er = ExportedRule(rule)
        val file = File(tempDir, "rule.json")
        er.export(file)

        val stored = FileUtils.readFileToString(file, UTF_8)
        val restored: ExportedRule = Json.decodeFromString(stored)
        restored shouldBe er
    }

    private fun serializeDeserialize(er: ExportedRule): ExportedRule {
        val serialized = Json.encodeToString(er)
        return Json.decodeFromString(serialized)
    }
}