package io.rippledown.model.condition.episodic.predicate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.startWith
import kotlin.test.Test

const val VALID_PERCENTAGE_MESSAGE = "Value should be an integer in the range [0, 100]"

class ToolsTest: Base() {
    @Test
    fun isOrAre(){
        isOrAre(false) shouldBe "is"
        isOrAre(true) shouldBe "are"
    }

    @Test
    fun checkIReasonablePercentageTest(){
        checkExceptionThrownForCutoff(200)
        checkExceptionThrownForCutoff(110)
        checkExceptionThrownForCutoff(101)
        checkExceptionThrownForCutoff(-1)
        checkExceptionThrownForCutoff(-10)
    }

    private fun checkExceptionThrownForCutoff(cutoff: Int) {
        shouldThrow<IllegalArgumentException> {
            checkIsReasonablePercentage(cutoff)
        }.message should startWith(VALID_PERCENTAGE_MESSAGE)
    }
}