package io.rippledown.persistence.inmemory

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import kotlin.test.Test

class InMemoryKBTest {

    @Test
    fun `rename updates the stored KB name without changing its id`() {
        // given
        val original = KBInfo("thyroids_1", "Thyroids")
        val persistence = InMemoryKB(original)

        // when
        persistence.rename("Thyroid Function")

        // then
        persistence.kbInfo().id shouldBe original.id
        persistence.kbInfo().name shouldBe "Thyroid Function"
    }
}
