package io.rippledown.chat.conversation

import io.kotest.matchers.string.shouldContain
import io.rippledown.constants.chat.ADD_A_COMMENT
import io.rippledown.constants.chat.ANY_CONDITIONS
import io.rippledown.constants.chat.PLEASE_CONFIRM
import io.rippledown.constants.chat.WHAT_COMMENT
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

private const val I_AM_VALID = "I am valid"

class ConversationForCheckingUserExpressionTest {
    lateinit var conversation: ConversationService

    @BeforeEach
    fun setUp() {
        val expressionCheck = { expression: String ->
            expression.contains(I_AM_VALID)
        }
        conversation = Conversation(expressionCheck)
    }


    @Test
    fun `the bot should accept the entered expression if it is valid`() = runTest {
        // Given
        val bondiComment = "Go to Bondi."
        val expression = "The waves are perfect. $I_AM_VALID"
        val case = caseWithNoComments()

        val bot0 = conversation.startConversation(case)
        bot0 shouldContain (ADD_A_COMMENT)

        val bot1 = conversation.response("yes")
        bot1 shouldContain WHAT_COMMENT

        val bot2 = conversation.response("Add the comment '$bondiComment'")
        bot2 shouldContain (PLEASE_CONFIRM)

        val bot3 = conversation.response("yes")
        bot3 shouldContain ANY_CONDITIONS

        val bot4 = conversation.response("yes")
        bot4 shouldContain "Please provide the first condition."

        // When
        val bot5 = conversation.response("add the condition '$expression'")

        // Then
        bot5 shouldContain "Condition accepted."
    }

    @Test
    fun `the bot should not accept the entered expression if it is invalid`() = runTest {
        // Given
        val bondiComment = "Go to Bondi."
        val expression = "The waves are perfect. I am not valid"
        val case = caseWithNoComments()

        val bot0 = conversation.startConversation(case)
        bot0 shouldContain (ADD_A_COMMENT)

        val bot1 = conversation.response("yes")
        bot1 shouldContain WHAT_COMMENT

        val bot2 = conversation.response("Add the comment '$bondiComment'")
        bot2 shouldContain (PLEASE_CONFIRM)

        val bot3 = conversation.response("yes")
        bot3 shouldContain ANY_CONDITIONS

        val bot4 = conversation.response("yes")
        bot4 shouldContain "Please provide the first condition."

        // When
        val bot5 = conversation.response("add the condition '$expression'")

        // Then
        bot5 shouldContain "Please rephrase the condition"
    }
}