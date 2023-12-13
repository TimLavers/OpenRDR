package io.rippledown.model.condition.structural

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class IsPresentInCaseTest: ConditionTestBase() {

    private val isPresent = IsPresentInCase(tsh)

    @Test
    fun evaluate() {
        isPresent.evaluate(clinicalNotesCase("blah")) shouldBe false
        isPresent.evaluate(twoEpisodeCaseWithFirstTSHLowSecondNormal()) shouldBe true
    }

    @Test
    fun alignAttributes() {
        val newTSH = tsh.copy(name = "Renamed")
        val attributeMap = mapOf(tsh.id to newTSH, tsh.id + 1200 to tsh, clinicalNotes.id to clinicalNotes)
        val aligned = isPresent.alignAttributes{ id -> attributeMap[id]!! }
        aligned.attribute.name shouldBe newTSH.name
    }

    @Test
    fun description() {
        isPresent.description() shouldBe "${tsh.name} is in case"
    }

    @Test
    fun serialization() {
        val sd: IsPresentInCase = Json.decodeFromString(Json.encodeToString(isPresent))
        sd shouldBe isPresent
    }
}