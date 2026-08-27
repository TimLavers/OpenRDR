package io.rippledown.model

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CommentTest {

    @Test
    fun `a short comment is not truncated`() {
        "Normal results.".truncatedComment() shouldBe "Normal results."
    }

    @Test
    fun `a comment of the maximum shown length is not truncated`() {
        val text = "12345678901234567890"
        text.length shouldBe 20
        text.truncatedComment() shouldBe text
    }

    @Test
    fun `a long comment is truncated`() {
        "This is a long comment.".truncatedComment() shouldBe "This is a long comme..."
    }

    @Test
    fun `a variable that straddles the truncation point is kept whole`() {
        //Given a comment whose variable name starts before, and ends after, the truncation point
        val text = "The wave is {Quality} today."

        //Then the whole variable is shown
        text.truncatedComment() shouldBe "The wave is {Quality}..."
    }

    @Test
    fun `a variable that ends before the truncation point is not extended`() {
        "The {Wave} is good today.".truncatedComment() shouldBe "The {Wave} is good t..."
    }
}
