package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.*
import io.rippledown.model.condition.ConditionTestBase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class RuleSummaryTest: ConditionTestBase() {
    private val conclusion = Conclusion( 1, "Capricious behaviour normal at that age.")
    private val empty = RuleSummary(8,null, emptySet())
    private val rs2: RuleSummary
    private val rs3: RuleSummary

    init {
        val conditions2 = mutableSetOf<Condition>()
        conditions2.add(SlightlyLow(2000, glucose, 10))
        conditions2.add(IsNormal(2001, tsh))
        rs2 = RuleSummary(12,null, conditions2)

        val conditions3 = mutableSetOf<Condition>()
        conditions3.add(IsNormal(3000, glucose))
        conditions3.add(IsNormal(3001, tsh))
        conditions3.add(ContainsText(3002, clinicalNotes, "goats"))
        rs3 = RuleSummary(13,conclusion, conditions3)
    }

    @Test
    fun noConclusion() {
        empty.conclusion shouldBe null
        rs2.conclusion shouldBe null
    }

    @Test
    fun conditions() {
        empty.conditions.size shouldBe 0

        rs3.conditions shouldContain IsNormal(3000, glucose)
        rs3.conditions shouldContain IsNormal(3001, tsh)
        rs3.conditions shouldContain ContainsText(3002, clinicalNotes, "goats")
        rs3.conditions.size shouldBe 3
    }

    @Test
    fun conclusion() {
        rs3.conclusion shouldBe conclusion
    }

    @Test
    fun serialization() {
        serializeDeserialize(empty) shouldBe empty
        serializeDeserialize(rs2) shouldBe rs2
        val sd3 = serializeDeserialize(rs3)
        sd3 shouldBe rs3
        sd3.conditions shouldBe rs3.conditions
        sd3.conclusion shouldBe conclusion
    }

    fun serializeDeserialize(ruleSummary: RuleSummary): RuleSummary {
        val serialized = Json.encodeToString(ruleSummary)
        return Json.decodeFromString(serialized)
    }
}
