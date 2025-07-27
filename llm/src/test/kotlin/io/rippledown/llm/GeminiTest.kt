package io.rippledown.llm

import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class GeminiTest {
    @Test
    fun `should call Gemini`() {
        runBlocking {
            generativeModel().generateContent("Highest mountain in the world?").text
        } shouldContain "Everest"
    }
}