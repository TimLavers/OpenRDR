package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.rr
import io.rippledown.model.condition.tr
import kotlin.test.Test

class NormalFactoryTest {
    @Test
    fun createFor() {
//        NormalFactory(null).createFor() shouldBe null
//        NormalFactory(tr("whatever")).createFor() shouldBe null
//        NormalFactory(tr("1.0")).createFor() shouldBe null
//        NormalFactory(tr("1.0",  "k/L")).createFor() shouldBe null
//        NormalFactory(tr("1.0", rr("0.5", null), "k/L")).createFor() shouldBe Normal
//        NormalFactory(tr("1.0", rr("0.5", null))).createFor() shouldBe Normal
//        NormalFactory(tr("1.0", rr("0.5", "0.9"))).createFor() shouldBe Normal
    }
}