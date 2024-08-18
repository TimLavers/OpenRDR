package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.isAbsent
import io.rippledown.model.condition.isPresent
import kotlin.test.Test

internal class EpisodicSuggestedConditionFactoryTest {
    val a = Attribute(0, "A")
    val b = Attribute(1, "B")

    @Test
    fun stringValueTest() {
        IsFactory(null).stringValue() shouldBe null
        IsFactory(TestResult("blah")).stringValue() shouldBe "blah"
        IsFactory(TestResult("54")).stringValue() shouldBe "54"
        IsFactory(TestResult("-5.4")).stringValue() shouldBe "-5.4"
    }

    @Test
    fun doubleValueTest() {
        IsFactory(null).doubleValue() shouldBe null
        IsFactory(TestResult("blah")).doubleValue() shouldBe null
        IsFactory(TestResult("54")).doubleValue() shouldBe "54".toDouble()
        IsFactory(TestResult("-5.4")).doubleValue() shouldBe "-5.4".toDouble()
    }
}