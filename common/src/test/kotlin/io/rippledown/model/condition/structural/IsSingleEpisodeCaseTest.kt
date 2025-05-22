package io.rippledown.model.condition.structural

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class IsSingleEpisodeCaseTest: ConditionTestBase() {

    private val predicate = IsSingleEpisodeCase

    @Test
    fun evaluate() {
        predicate.evaluate(clinicalNotesCase("blah")) shouldBe true
        predicate.evaluate(twoEpisodeCaseWithFirstTSHLowSecondNormal()) shouldBe false
    }

    @Test
    fun alignAttributes() {
        val newTSH = tsh.copy(name = "Renamed")
        val attributeMap = mapOf(tsh.id to newTSH, tsh.id + 1200 to tsh, clinicalNotes.id to clinicalNotes)
        val aligned = predicate.alignAttributes{ id -> attributeMap[id]!! }
        aligned shouldBeSameInstanceAs predicate
    }

    @Test
    fun description() {
        predicate.description() shouldBe "case is for a single date"
    }

    @Test
    fun serialization() {
        serializeDeserialize(predicate) shouldBe predicate
    }
}