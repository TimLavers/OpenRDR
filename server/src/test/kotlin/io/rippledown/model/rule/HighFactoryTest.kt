package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.rr
import io.rippledown.model.condition.tr
import kotlin.test.Test

class HighFactoryTest {
    @Test
    fun createFor() {
        HighFactory(null).createFor() shouldBe null
        HighFactory(tr("whatever")).createFor() shouldBe null
        HighFactory(tr ("1.0")).createFor() shouldBe null
        HighFactory(tr("1.0", "k/L")).createFor() shouldBe null
        HighFactory(tr("1.0", rr("0.5", null), "k/L")).createFor() shouldBe High
        HighFactory(tr("1.0", rr("0.5", null))).createFor() shouldBe High
        HighFactory(tr("1.0", rr("0.5", "0.9"))).createFor() shouldBe High
    }
}