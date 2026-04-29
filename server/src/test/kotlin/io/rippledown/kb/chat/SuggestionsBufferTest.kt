package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SuggestionsBufferTest {

    @Test
    fun `suggestions are null by default`() {
        val buffer = SuggestionsBuffer()
        buffer.suggestions shouldBe null
    }

    @Test
    fun `consume returns null when nothing has been buffered`() {
        val buffer = SuggestionsBuffer()
        buffer.consume() shouldBe null
    }

    @Test
    fun `consume returns the buffered suggestions and clears them`() {
        val buffer = SuggestionsBuffer()
        val suggestions = listOf("Sun is \"hot\"", "Waves \u2265 1.5 [editable]")
        buffer.suggestions = suggestions

        buffer.consume() shouldBe suggestions
        buffer.suggestions shouldBe null
    }

    @Test
    fun `consume returns an empty list distinctly from null`() {
        val buffer = SuggestionsBuffer()
        buffer.suggestions = emptyList()

        buffer.consume() shouldBe emptyList()
        buffer.suggestions shouldBe null
    }

    @Test
    fun `a second consume after a single set returns null`() {
        val buffer = SuggestionsBuffer()
        buffer.suggestions = listOf("one")

        buffer.consume()
        buffer.consume() shouldBe null
    }

    @Test
    fun `setting suggestions again after consume buffers the new list`() {
        val buffer = SuggestionsBuffer()
        buffer.suggestions = listOf("first round")
        buffer.consume()

        val second = listOf("second round a", "second round b")
        buffer.suggestions = second

        buffer.consume() shouldBe second
    }
}
