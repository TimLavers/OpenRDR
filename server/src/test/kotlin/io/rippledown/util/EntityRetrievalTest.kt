package io.rippledown.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EntityRetrievalTest {

    @Test
    fun success() {
        val er = EntityRetrieval.Success("Great!")
        er.ok shouldBe true
        er.entity shouldBe "Great!"
    }

    @Test
    fun failure() {
        val er = EntityRetrieval.Failure<Long>("Sorry!")
        er.ok shouldBe false
        er.errorMessage shouldBe "Sorry!"
    }
}