package io.rippledown.textdiff

import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.test.BeforeTest

class TextToAlphabetMapperTest {
    lateinit var mapper: TextToAlphabetMapper

    @Test
    fun `should map the first text to A`() {
        mapper.toAlpha("This is a string") shouldBe "A"
    }

    @Test
    fun `should map consecutive texts to A B C etc`() {
        mapper.toAlpha("This is string 1") shouldBe "A"
        mapper.toAlpha("This is string 2") shouldBe "B"
        mapper.toAlpha("This is string 3") shouldBe "C"
    }

    @Test
    fun `should map identical texts to the same char`() {
        mapper.toAlpha("This is string 1") shouldBe "A"
        mapper.toAlpha("This is string 2") shouldBe "B"
        mapper.toAlpha("This is string 3") shouldBe "C"
        mapper.toAlpha("This is string 1") shouldBe "A"
    }

    @Test
    fun `should be case insensitive`() {
        mapper.toAlpha("This is string a") shouldBe "A"
        mapper.toAlpha("This is string A") shouldBe "B"
    }

    @Test
    fun `should map a char to its text`() {
        val text = "This is a string"
        val a = mapper.toAlpha(text)
        mapper.toText(a) shouldBe text
    }

    @Test
    fun `should map several chars to their corresponding text`() {
        val a = mapper.toAlpha("This is string 1")
        val b = mapper.toAlpha("This is string 2")
        val c = mapper.toAlpha("This is string 3")
        val a1 = mapper.toAlpha("This is string 1")

        mapper.toText(a) shouldBe "This is string 1"
        mapper.toText(b) shouldBe "This is string 2"
        mapper.toText(c) shouldBe "This is string 3"
        mapper.toText(a1) shouldBe "This is string 1"
    }

    @BeforeTest
    fun init() {
        mapper = TextToAlphabetMapper()
    }
}