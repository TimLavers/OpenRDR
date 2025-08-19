package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.rippledown.fromJsonString
import kotlin.test.Test

class ActionCommentTest {
    @Test
    fun `should parse ActionComment from JSON`() {
        // Given
        val json = """
            {
                "action": "REPLACE_COMMENT",
                "message": "This is a test message",
                "debug": "Debug info",
                "comment": "Old comment text",
                "replacementComment": "New comment text",
                "reasons": ["reason1", "reason2"]
            }
        """

        // When
        val actionComment = json.fromJsonString<ActionComment>()

        // Then
        with(actionComment) {
            action shouldBe "REPLACE_COMMENT"
            message shouldBe "This is a test message"
            debug shouldBe "Debug info"
            comment shouldBe "Old comment text"
            replacementComment shouldBe "New comment text"
            reasons?.size shouldBe 2
            reasons?.get(0) shouldBe "reason1"
            reasons?.get(1) shouldBe "reason2"
        }
    }

    @Test
    fun `should parse ActionComment with empty fields`() {
        // Given
        val json = """
            {
                "action": "USER_ACTION",
                "comment": "Let's surf"
            }
        """

        // When
        val actionComment = json.fromJsonString<ActionComment>()

        // Then
        with(actionComment) {
            action shouldBe "USER_ACTION"
            message shouldBe null
            debug shouldBe null
            comment shouldBe "Let's surf"
            reasons shouldBe null
        }
    }
}