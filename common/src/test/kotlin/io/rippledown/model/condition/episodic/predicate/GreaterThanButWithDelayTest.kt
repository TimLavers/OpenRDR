package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.longs.shouldBeAtLeast
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Result
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class GreaterThanButWithDelayTest {

    @Test
    fun `evaluation is the same as greater than`() {
        // Given results above, below and at the comparison value, and one that is not numeric
        val above = Result("6.7")
        val below = Result("2.1")
        val at = Result("4.0")
        val text = Result("blah")
        val predicate = GreaterThanButWithDelay(4.0, 10)

        // When each is evaluated
        // Then the outcome matches the plain greater-than comparison
        predicate.evaluate(above) shouldBe true
        predicate.evaluate(below) shouldBe false
        predicate.evaluate(at) shouldBe false
        predicate.evaluate(text) shouldBe false
    }

    @Test
    fun `evaluation is delayed by the given number of milliseconds`() {
        // Given a predicate with a 100 millisecond delay
        val predicate = GreaterThanButWithDelay(4.0, 100)

        // When a result is evaluated
        val start = System.nanoTime()
        predicate.evaluate(Result("6.7"))
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000

        // Then at least the delay has passed
        elapsedMillis shouldBeAtLeast 100
    }

    @Test
    fun `the default delay is half a second`() {
        // Given a predicate constructed without an explicit delay
        val predicate = GreaterThanButWithDelay(4.0)

        // Then its delay is the default
        predicate.delayMillis shouldBe GreaterThanButWithDelay.DEFAULT_DELAY_MILLIS
        GreaterThanButWithDelay.DEFAULT_DELAY_MILLIS shouldBe 500L
    }

    @Test
    fun `the description names the delayed comparison`() {
        // Given a predicate
        val predicate = GreaterThanButWithDelay(4.0, 10)

        // When it is described
        // Then the dumb name appears, so no real condition can be mistaken for it
        predicate.description(plural = true) shouldBe "is greater than but with delay 4.0"
        predicate.description(plural = false) shouldBe "is greater than but with delay 4.0"
    }

    @Test
    fun `serialisation round trip`() {
        // Given a condition using the delayed predicate
        val attribute = Attribute(1, "TSH")
        val condition = EpisodicCondition(null, attribute, GreaterThanButWithDelay(4.0, 10), Current)

        // When it is serialised and deserialised
        val restored = serializeDeserialize(condition)

        // Then the delayed predicate is intact
        restored.predicate shouldBe GreaterThanButWithDelay(4.0, 10)
    }
}
