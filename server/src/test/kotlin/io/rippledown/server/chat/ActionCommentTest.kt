package io.rippledown.server.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.rippledown.constants.chat.*
import io.rippledown.fromJsonString
import io.rippledown.server.chat.action.AddComment
import io.rippledown.server.chat.action.CommitRule
import io.rippledown.server.chat.action.ExemptCornerstone
import io.rippledown.server.chat.action.ListKnowledgeBases
import io.rippledown.server.chat.action.MoveAttribute
import io.rippledown.server.chat.action.OpenKnowledgeBase
import io.rippledown.server.chat.action.RemoveComment
import io.rippledown.server.chat.action.ReplaceComment
import io.rippledown.server.chat.action.UndoLastRule
import kotlin.test.Test

class ActionCommentTest {

    @Test
    fun `should handle error when action class cannot be instantiated`() {
        // Given
        val invalidAction = "NonExistentAction"
        val actionComment = ActionComment(invalidAction)
        
        // When
        val result = actionComment.createActionInstance()
        
        // Then
        result shouldBe null
    }

    @Test
    fun `should parse UserAction from JSON`() {
        // Given
        val msg = "Please confirm..."
        val json = """
            {
                "action": "$USER_ACTION",
                "message": "$msg",
            }
        """

        // When
        val actionComment = json.fromJsonString<ActionComment>()

        // Then
        with(actionComment) {
            action shouldBe USER_ACTION
            message shouldBe msg
            debug shouldBe null
            comment shouldBe null
            replacementComment shouldBe null
            reason shouldBe null
        }
    }

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
                "reason": "reason1"
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
            reason shouldBe "reason1"
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
            reason shouldBe null
        }
    }

    @Test
    fun `should handle an invalid action string`() {
        // Given
        val json = """
            {
                "action": "unknown action"
            }
        """
        val actionComment = json.fromJsonString<ActionComment>()

        // When
        val action = actionComment.createActionInstance()

        // Then
        action shouldBe null
    }

    @Test
    fun moveAttribute() {
        val actionComment = ActionComment("MoveAttribute", attributeMoved = "Glucose", destination = "Age")
        with(actionComment.createActionInstance() as MoveAttribute) {
            attributeMoved shouldBe "Glucose"
            destination shouldBe "Age"
        }
    }

    @Test
    fun openKnowledgeBase() {
        val actionComment = ActionComment("OpenKnowledgeBase", kbName = "Glucose")
        with(actionComment.createActionInstance() as OpenKnowledgeBase) {
            kbName shouldBe "Glucose"
        }
    }

    @Test
    fun listKnowledgeBases() {
        val actionComment = ActionComment("ListKnowledgeBases")
        actionComment.createActionInstance()!!::class shouldBe ListKnowledgeBases::class
    }

    @Test
    fun undoLastRule() {
        val actionComment = ActionComment("UndoLastRule")
        actionComment.createActionInstance()!!.javaClass shouldBe UndoLastRule().javaClass
    }

    @Test
    fun addComment() {
        val commentToAdd = "Beach time!"
        val actionComment = ActionComment(ADD_COMMENT, comment = commentToAdd)
        with(actionComment.createActionInstance() as AddComment) {
            comment shouldBe commentToAdd
        }
    }

    @Test
    fun removeComment() {
        val commentToRemove = "Beach time!"
        val actionComment = ActionComment(REMOVE_COMMENT, comment = commentToRemove)
        with(actionComment.createActionInstance() as RemoveComment) {
            comment shouldBe commentToRemove
        }
    }

    @Test
    fun replaceComment() {
        val commentToRemove = "Beach time!"
        val commentToAdd = "Surf time!"
        val actionComment = ActionComment(REPLACE_COMMENT, comment = commentToRemove, replacementComment = commentToAdd)
        with(actionComment.createActionInstance() as ReplaceComment) {
            comment shouldBe commentToRemove
            replacementComment shouldBe commentToAdd
        }
    }

    @Test
    fun commitRule() {
        val actionComment = ActionComment(action = COMMIT_RULE)
        actionComment.createActionInstance().shouldBeInstanceOf<CommitRule>()
    }

    @Test
    fun exemptCornerstone() {
        val actionComment = ActionComment(action = EXEMPT_CORNERSTONE)
        actionComment.createActionInstance().shouldBeInstanceOf<ExemptCornerstone>()
    }

    @Test
    fun `should parse action comment for CommitRule from JSON`() {
        //Given
        val json = """
            {
                "action": "$COMMIT_RULE",
                "message": null,
                "debug": null,
                "comment": null,
                "replacementComment": null,
                "reason": null,
                "attributeMoved": null,
                "destination": null
            }
        """.trimIndent()
        val actionComment = json.fromJsonString<ActionComment>()

        //When
        val instance = actionComment.createActionInstance()

        //Then
        instance.shouldBeInstanceOf<CommitRule>()
    }

    @Test
    fun handleUnknownAction() {
        val actionComment = ActionComment("SwapAttributes", attributeMoved = "Glucose", destination = "Age")
        actionComment.createActionInstance() shouldBe null
    }
}