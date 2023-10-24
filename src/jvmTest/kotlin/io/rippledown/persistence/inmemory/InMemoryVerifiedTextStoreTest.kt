package io.rippledown.persistence.inmemory

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class InMemoryVerifiedTextStoreTest {
    @Test
    fun `should store the verified text corresponding to an id`() {
        with(InMemoryVerifiedTextStore()) {
            // given
            get(0) shouldBe null

            // when
            put(0, "text")

            // then
            get(0) shouldBe "text"
        }
    }

}