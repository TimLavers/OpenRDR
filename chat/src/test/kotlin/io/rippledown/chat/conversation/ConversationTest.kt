package io.rippledown.chat.conversation

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.string.shouldContain
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.model.rule.Rule
import io.rippledown.shouldContainIgnoringMultipleWhitespace
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class ConversationTest {
    lateinit var conversation: ConversationService

    @BeforeEach
    fun setUp() {
        conversation = Conversation()
    }

    @Test
    fun `for a case with no comments, the model should start a conversation by asking if a comment should be added`() =
        runTest {
            // Given
            val case = caseWithNoComments()

            // When
            val response = conversation.startConversation(case)

            // Then
            response shouldContain QUESTION_IF_THERE_ARE_NO_EXISTING_COMMENTS
        }

    @Test
    fun `for a case with at least one comment, the model should start a conversation by asking what change is required to the report`() =
        runTest {
            // Given
            val case = caseWithComments()

            // When
            val response = conversation.startConversation(case)

            // Then
            response shouldContain QUESTION_IF_THERE_ARE_EXISTING_COMMENTS
        }

    @Test
    fun `when the comment to be added is described, the model should request confirmation`() = runTest {
        // Given
        val case = caseWithNoComments()
        val initialResponse = conversation.startConversation(case)
        println("initialResponse = ${initialResponse}")

        // When
        val response = conversation.response("add the comment 'Go to Bondi'")

        // Then
        println("response = ${response}")
        response shouldContain CONFIRMATION_START
    }

    @Test
    fun `when the comment to be added is confirmed, the model output a json object representing the rule action`() =
        runTest {
            // Given
            val case = caseWithNoComments()
            conversation.startConversation(case)
            val bondiComment = "Go to Bondi."
            conversation.response("Add the comment '$bondiComment'")

            // When
            val confirmationResponse = conversation.response("Yes")

            // Then
            confirmationResponse shouldContainIgnoringMultipleWhitespace """
                {
                    "action": "add",
                    "comment": "$bondiComment"
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
