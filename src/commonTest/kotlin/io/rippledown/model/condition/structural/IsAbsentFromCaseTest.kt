package io.rippledown.model.condition.structural

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class IsAbsentFromCaseTest: ConditionTestBase() {

    private val isAbsent = IsAbsentFromCase(tsh)

    @Test
    fun evaluate() {
        isAbsent.evaluate(clinicalNotesCase("blah")) shouldBe true
        isAbsent.evaluate(twoEpisodeCaseWithFirstTSHLowSecondNormal()) shouldBe false
    }

    @Test
    fun alignAttributes() {
        val newTSH = tsh.copy(name = "Renamed")
        val attributeMap = mapOf(tsh.id to newTSH, tsh.id + 1200 to tsh, clinicalNotes.id to clinicalNotes)
        val aligned = isAbsent.alignAttributes{id -> attributeMap[id]!! }
        aligned.attribute.name shouldBe newTSH.name
    }

    @Test
    fun description() {
        isAbsent.description() shouldBe "${tsh.name} is not in case"
    }

    @Test
    fun serialization() {
        val sd: IsAbsentFromCase = Json.decodeFromString(Json.encodeToString(isAbsent))
        sd shouldBe isAbsent
    }
}