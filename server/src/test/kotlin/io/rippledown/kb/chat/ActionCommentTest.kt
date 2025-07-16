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
                "action": "USER_ACTION",
                "message": "This is a test message",
                "debug": "Debug info",
                "comment": "New comment text",
                "reasons": ["reason1", "reason2"]
            }
        """

        // When
        val actionComment = json.fromJsonString<ActionComment>()

        // Then
        with(actionComment) {
            action shouldBe "USER_ACTION"
            message shouldBe "This is a test message"
            debug shouldBe "Debug info"
            comment shouldBe "New comment text"
            reasons?.size shouldBe 2
            reasons?.get(0) shouldBe "reason1"
            reasons?.get(1) shouldBe "reason2"
        }
    }
}