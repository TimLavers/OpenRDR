package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.rr
import io.rippledown.model.condition.tr
import kotlin.test.Test

open class ConditionFactoryTestBase {
    val tsh = Attribute(9457, "TSH")
}