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
                "new_comment": "New comment text",
                "existing_comment": "Existing comment text",
                "conditions": ["condition1", "condition2"]
            }
        """

        // When
        val actionComment = json.fromJsonString<ActionComment>()

        // Then
        with(actionComment) {
            action shouldBe "USER_ACTION"
            message shouldBe "This is a test message"
            debug shouldBe "Debug info"
            new_comment shouldBe "New comment text"
            existing_comment shouldBe "Existing comment text"
            conditions?.size shouldBe 2
            conditions?.get(0) shouldBe "condition1"
            conditions?.get(1) shouldBe "condition2"
        }
    }
}