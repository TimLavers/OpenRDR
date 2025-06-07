package io.rippledown.chat.conversation

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.string.shouldContain
import io.rippledown.constants.chat.*
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.model.rule.Rule
import io.rippledown.utils.shouldContainAll
import io.rippledown.utils.shouldContainIgnoringMultipleWhitespace
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class ConversationForAddingACommentTest {
    lateinit var conversation: ConversationService

    @BeforeEach
    fun setUp() {
        conversation = Conversation()
    }

    @Test
    fun `for a case with no comments, the model should start a conversation with a debug message and a question whether a comment should be added`() =
        runTest {
            // Given
            val case = caseWithNoComments()

            // When
            val response = conversation.startConversation(case)

            // Then
            response shouldContainAll listOf(
                WOULD_YOU_LIKE,
                ADD_A_COMMENT,
                DEBUG_ACTION,
                NO_COMMENTS
            )
        }

    @Test
    fun `for a case with at least one comment, the model start a conversation with a debug message and a question whether a comment should be added, removed or deleted`() =
        runTest {
            // Given
            val case = caseWithComments()

            // When
            val response = conversation.startConversation(case)

            // Then
            response shouldContainAll listOf(
                WOULD_YOU_LIKE,
                ADD,
                REMOVE,
                REPLACE,
                DEBUG_ACTION,
                EXISTING_COMMENTS
            )
        }

    @Test
    fun `when the comment to be added and the condition is confirmed, the model should output a json object representing the rule action`() =
        runTest {
            // Given
            val bondiComment = "Go to Bondi."
            val condition = "The sun is hot."
            val case = caseWithNoComments()

            //Ask if the user wants to add a comment
            val bot0 = conversation.startConversation(case)
            bot0 shouldContainAll listOf(
                WOULD_YOU_LIKE,
                ADD_A_COMMENT,
                DEBUG_ACTION,
                NO_COMMENTS
            )
            val bot1 = conversation.response("yes")

            //Confirm that the user wants to add the bondi comment
            bot1 shouldContain WHAT_COMMENT
            val bot2 = conversation.response("Add the comment '$bondiComment'")
            bot2 shouldContainAll listOf(
                PLEASE_CONFIRM,
                bondiComment
            )
            val bot3 = conversation.response("yes")

            // When
            // Ask if the user wants to add a condition
            bot3 shouldContain ANY_CONDITIONS
            val bot4 = conversation.response("Add the condition '$condition'")

            // Then
            bot4 shouldContainIgnoringMultipleWhitespace """
                {
                    "action": "$ADD_ACTION",
                    "new_comment": "$bondiComment",
                    "conditions": [ "$condition" ]
                }
            """.trimIndent()
        }
}

fun caseWithComments(): RDRCase {
    var id = 0
    val tsh = Attribute(1, "TSH")
    val ft4 = Attribute(1, "FT4")
    val case = with(RDRCaseBuilder()) {
        addValue(tsh, 1_000, "0.667")
        addValue(ft4, 1_000, "0.8")
        build("Case1234", 0)
    }

    val conclusion1 = Conclusion(++id, "TSH is within the normal range.")
    val conclusion2 = Conclusion(++id, "FT4 is within the normal range.")
    val root = Rule(++id, null, null, emptySet(), mutableSetOf())
    val condition1 = lessThanOrEqualTo(++id, tsh, 4.0)
    val condition2 = lessThanOrEqualTo(++id, ft4, 1.9)
    val rule1 = Rule(++id, root, conclusion1, setOf(condition1), mutableSetOf())
    val rule2 = Rule(++id, root, conclusion2, setOf(condition2), mutableSetOf())
    case.interpretation.add(rule1)
    case.interpretation.add(rule2)
    case.interpretation.conclusions() shouldContainExactlyInAnyOrder setOf(conclusion1, conclusion2)
    return case
}

fun caseWithNoComments(): RDRCase {
    val tsh = Attribute(1, "TSH")
    val ft4 = Attribute(1, "FT4")
    val case = with(RDRCaseBuilder()) {
        addValue(tsh, 1_000, "0.667")
        addValue(ft4, 1_000, "0.8")
        build("Case1234", 0)
    }
    return case
}
