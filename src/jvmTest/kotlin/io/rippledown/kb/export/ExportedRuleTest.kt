package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.GreaterThanOrEqualTo
import io.rippledown.model.condition.IsHigh
import io.rippledown.model.rule.Rule
import io.rippledown.persistence.PersistentRule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.test.Test
import kotlin.text.Charsets.UTF_8

class ExportedRuleTest: ExporterTestBase() {
    private val id123 = 10123
    private val id0 = 100
    private val conclusion1 = Conclusion(1, "A trip to the beach is advised.")
    private val ft3 = Attribute(300, "FT3")
    private val tshHigh = IsHigh(100, tsh)
    private val ft3GT2 = GreaterThanOrEqualTo(200, ft3, 2.0)

    @Test
    fun constructor1() {
        val er = ExportedRule(PersistentRule())
        er.persistentRule.id shouldBe null
        er.persistentRule.parentId shouldBe null
        er.persistentRule.conclusionId shouldBe null
        er.persistentRule.conditionIds shouldBe emptySet()
    }

    @Test
    fun constructor2() {
        val pr = PersistentRule(2, 1, 3, setOf(4,5))
        val er = ExportedRule(pr)
        er.persistentRule.id shouldBe 2
        er.persistentRule.parentId shouldBe 1
        er.persistentRule.conclusionId shouldBe 3
        er.persistentRule.conditionIds shouldBe setOf(4, 5)
    }

    @Test
    fun exportTest() {
        val root = Rule(id0, null, null, setOf())
        val rule = Rule(id123, root, conclusion1, setOf(tshHigh, ft3GT2))
        val er = ExportedRule(rule)
        val file = File(tempDir, "rule.json")
        er.export(file)

        val stored = FileUtils.readFileToString(file, UTF_8)
        val restored: PersistentRule = Json.decodeFromString(stored)
        restored shouldBe er.persistentRule
    }
}