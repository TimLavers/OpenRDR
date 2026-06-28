package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.beBlank
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.ConversationService
import io.rippledown.constants.chat.*
import io.rippledown.kb.chat.ChatManager.Companion.AI_UNAVAILABLE_MESSAGE
import io.rippledown.kb.chat.ChatManager.Companion.CURRENT_CORNERSTONE_STATUS_PREFIX
import io.rippledown.kb.chat.ChatManager.Companion.LOG_PREFIX_FOR_CONVERSATION_RESPONSE
import io.rippledown.kb.chat.ChatManager.Companion.LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE
import io.rippledown.kb.chat.ChatManager.Companion.commentVariableTip
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import org.slf4j.Logger
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * @author Cascade AI
 */
class ChatManagerTest {
    lateinit var logger: Logger
    lateinit var conversationService: ConversationService
    lateinit var ruleService: RuleService
    lateinit var case: RDRCase
    lateinit var viewableCase: ViewableCase
    lateinit var suggestionsBuffer: SuggestionsBuffer
    lateinit var chatManager: ChatManager

    @BeforeTest
    fun setUp() {
        conversationService = mockk()
        ruleService = mockk()
        viewableCase = mockk()
        case = mockk()
        suggestionsBuffer = SuggestionsBuffer()
        every { viewableCase.case } returns case
        every { viewableCase.attributes() } returns listOf(Attribute(1, "Glucose"), Attribute(2, "TSH"))
        every { ruleService.isRuleSessionActive() } returns false
        chatManager = ChatManager(conversationService, ruleService, suggestionsBuffer)
        setupLogger()
    }

    private fun setupLogger() {
        logger = mockk()
        val loggerField = ChatManager::class.java.getDeclaredField("logger")
        loggerField.isAccessible = true
        loggerField.set(chatManager, logger)
        every { logger.isInfoEnabled } returns true
        every { logger.isErrorEnabled } returns true
    }

    @Test
    fun `should process an action comment from a start conversation response`() = runTest {
        // Given
        val actionComment = ActionComment(action = USER_ACTION, message = "test response")

        // When
        val responseToUser = chatManager.processActionComment(actionComment)

        // Then
        responseToUser shouldBe ChatResponse(actionComment.message!!)
    }

    @Test
    fun `should process an action comment from a response after the conversation has been started`() = runTest {
        // Given
        val actionComment = ActionComment(action = USER_ACTION, message = "test response")

        // When
        val responseToUser = chatManager.processActionComment(actionComment)

        // Then
        responseToUser shouldBe ChatResponse(actionComment.message!!)
    }

    @Test
    fun `should delegate a start conversation request to the conversation service`() = runTest {
        // Given
        val message = "Do you know the meaning of life?"
        val responseFromModel = """
            {
                "action": "$USER_ACTION",
                "message": "$message"
            }
        """.trimIndent()
        coEvery { conversationService.startConversation() } returns responseFromModel

        // When
        chatManager.startConversation(viewableCase)

        // Then
        coVerify { conversationService.startConversation() }
    }

    @Test
    fun `should log a start conversation response`() = runTest {
        // Given
        val actionComment = ActionComment(action = USER_ACTION, message = "test response")
        val responseFromModel = actionComment.toJsonString()
        coEvery { conversationService.startConversation() } returns responseFromModel

        // When
        chatManager.startConversation(viewableCase)

        // Then
        coVerify(exactly = 1) { logger.info("$LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE '$responseFromModel'") }
    }

    @Test
    fun `should log a conversation response`() = runTest {
        // Given
        val actionComment = ActionComment(action = USER_ACTION, message = "test response")
        val responseFromModel = actionComment.toJsonString()
        coEvery { conversationService.response(any<String>()) } returns responseFromModel

        // When
        chatManager.response("hi there")

        // Then
        coVerify(exactly = 1) { logger.info("$LOG_PREFIX_FOR_CONVERSATION_RESPONSE $responseFromModel") }
    }

    @Test
    fun `should return a user message from a startConversation call`() = runTest {
        // Given
        val message = "Do you know the meaning of life?"
        val responseFromModel = """
            {
                "action": "$USER_ACTION",
                "message": "$message"
            }
        """.trimIndent()
        coEvery { conversationService.startConversation() } returns responseFromModel

        // When
        val responseToUser = chatManager.startConversation(viewableCase)

        // Then
        responseToUser shouldBe ChatResponse(message)
    }

    @Test
    fun `should return suggestions from ActionComment in ChatResponse`() = runTest {
        // Given
        val message = "Here are some suggested conditions."
        val suggestions = listOf("wave height is \"2\"", "case is for a single date")
        val responseFromModel = ActionComment(
            action = USER_ACTION,
            message = message,
            suggestions = suggestions
        ).toJsonString()
        coEvery { conversationService.startConversation() } returns responseFromModel

        // When
        val responseToUser = chatManager.startConversation(viewableCase)

        // Then
        responseToUser shouldBe ChatResponse(message, suggestions)
    }

    @Test
    fun `buffered suggestions take precedence over ActionComment suggestions`() = runTest {
        // Given
        val message = "Here are some suggestions."
        val bufferedSuggestions = listOf("buffered one", "buffered two [editable]")
        suggestionsBuffer.suggestions = bufferedSuggestions
        val responseFromModel = ActionComment(
            action = USER_ACTION,
            message = message,
            suggestions = listOf("model echoed should be ignored")
        ).toJsonString()
        coEvery { conversationService.startConversation() } returns responseFromModel

        // When
        val responseToUser = chatManager.startConversation(viewableCase)

        // Then
        responseToUser shouldBe ChatResponse(message, bufferedSuggestions)
        // Buffer is consumed
        suggestionsBuffer.suggestions shouldBe null
    }

    @Test
    fun `startConversation should surface a prose response as a plain bot message rather than 500`() = runTest {
        // Given: the model ignored the JSON protocol and returned prose,
        // as it does when the case already has comments. Previously this
        // path threw a JsonDecodingException out of the KBEndpoint; the
        // client would see an HTTP 500 and wedge. The manager must now
        // echo the prose back as a plain bot message so the chat panel
        // can render it and the user (and the cucumber suite) can
        // continue the scenario.
        val prose = """
            This case has the following comments:
            "Comment 1.",
            "Comment 2.",
            "Comment 3.".
            Would you like to add another one, or replace or remove one of them?
        """.trimIndent()
        coEvery { conversationService.startConversation() } returns prose

        // When
        val responseToUser = chatManager.startConversation(viewableCase)

        // Then
        responseToUser shouldBe ChatResponse(prose)
    }

    @Test
    fun `startConversation should surface malformed JSON-looking text rather than 500`() = runTest {
        // Given: the model emitted something that started with '{' but
        // isn't a valid ActionComment (e.g. truncated output). The old
        // implementation propagated the JsonDecodingException out of the
        // HTTP handler. Now we must fall back to delivering the raw text.
        val garbage = "{ this is not valid JSON"
        coEvery { conversationService.startConversation() } returns garbage

        // When
        val responseToUser = chatManager.startConversation(viewableCase)

        // Then
        responseToUser shouldBe ChatResponse(garbage)
    }

    @Test
    fun `response should surface a prose response as a plain bot message rather than empty`() = runTest {
        // Given: the model replied to a user message in prose (no JSON
        // fragments) — for example because it went off-script and asked
        // a clarifying question instead of emitting an ActionComment.
        // Previously the manager returned ChatResponse("") here, leaving
        // the chat panel silent and causing cucumber scenarios such as
        // "The comments given for a case are returned by the
        // interpretation service" to hang for 60s waiting for
        // suggestions that never arrived. The manager must now echo the
        // prose so the user (and the cucumber suite) can see what the
        // model actually said.
        val prose = "I'm not sure what you mean. Could you rephrase that?"
        coEvery { conversationService.response(any()) } returns prose

        // When
        val responseToUser = chatManager.response("hello")

        // Then
        responseToUser shouldBe ChatResponse(prose)
    }

    @Test
    fun `should return empty suggestions when ActionComment has no suggestions`() = runTest {
        // Given
        val message = "No suggestions here."
        val responseFromModel = ActionComment(
            action = USER_ACTION,
            message = message,
        ).toJsonString()
        coEvery { conversationService.startConversation() } returns responseFromModel

        // When
        val responseToUser = chatManager.startConversation(viewableCase)

        // Then
        responseToUser shouldBe ChatResponse(message)
    }

    @Test
    fun `should return a user message from a response call`() = runTest {
        // Given
        val message = "the answer's 42"
        val responseFromModel = """
            {
                "action": "$USER_ACTION",
                "message": "$message"
            }
        """.trimIndent()
        coEvery { conversationService.response(any()) } returns responseFromModel


        // When
        val responseToUser = chatManager.response("meaning of life?")

        // Then
        responseToUser shouldBe ChatResponse(message)
    }

    @Test
    fun `should return a blank string to a debug message from the conversation service`() = runTest {
        // Given
        val message = "the answer is 42"
        val responseFromModel = """
            {
                "action": "$DEBUG_ACTION",
                "message": "$message"
            }
        """.trimIndent()
        coEvery { conversationService.response(any()) } returns responseFromModel

        // When
        val responseToUser = chatManager.response("meaning of life?")

        // Then
        responseToUser.text shouldBe beBlank()
    }

    @Test
    fun `should commit a rule session`() =
        runTest {
            // Given
            val message = "What do you want to replace?"
            val initialResponseFromModel = ActionComment(USER_ACTION, message = message).toJsonString()
            coEvery { conversationService.startConversation() } returns initialResponseFromModel
            chatManager.startConversation(viewableCase)

            val responseFromModel = ActionComment(
                action = COMMIT_RULE,
            ).toJsonString()
            coEvery { conversationService.response(any<String>()) } returns responseFromModel

            // When
            chatManager.response("please commit the rule")

            // Then
            coVerify {
                ruleService.commitCurrentRuleSession()
            }
        }

    @Test
    fun `should start a rule session for adding a comment`() =
        runTest {
            // Given
            val initialResponseFromModel =
                ActionComment(USER_ACTION, message = "What do you want to add?").toJsonString()
            coEvery { conversationService.startConversation() } returns initialResponseFromModel
            chatManager.startConversation(viewableCase) //to set the current case

            val comment = "Go to Bondi."
            val responseFromModelToAddComment = ActionComment(
                action = ADD_COMMENT,
                comment = comment,
            ).toJsonString()
            coEvery { conversationService.response(any<String>()) } answers {
                responseFromModelToAddComment
            } andThenAnswer {
                "anything else?"
            }

            // When
            chatManager.response("")

            // Then
            coVerify { ruleService.startRuleSessionToAddComment(viewableCase, comment, emptyList()) }
        }

    @Test
    fun `should append the comment variable tip the first time a comment is added in a session`() = runTest {
        // Given
        val initialResponseFromModel =
            ActionComment(USER_ACTION, message = "What do you want to add?").toJsonString()
        coEvery { conversationService.startConversation() } returns initialResponseFromModel
        chatManager.startConversation(viewableCase) //to set the current case

        val addComment = ActionComment(action = ADD_COMMENT, comment = "Go to Bondi.").toJsonString()
        coEvery { conversationService.response(any<String>()) } returnsMany listOf(addComment, "anything else?")

        // When
        val responseToUser = chatManager.response("add a comment")

        // Then - the tip is delivered on its own channel, with the example using the
        // first attribute of the displayed case
        responseToUser.tip shouldBe commentVariableTip("Glucose")
    }

    @Test
    fun `should not repeat the comment variable tip for a second comment in the same session`() = runTest {
        // Given
        val initialResponseFromModel =
            ActionComment(USER_ACTION, message = "What do you want to add?").toJsonString()
        coEvery { conversationService.startConversation() } returns initialResponseFromModel
        chatManager.startConversation(viewableCase) //to set the current case

        val firstAdd = ActionComment(action = ADD_COMMENT, comment = "Go to Bondi.").toJsonString()
        val secondAdd = ActionComment(action = ADD_COMMENT, comment = "Go to Manly.").toJsonString()
        coEvery { conversationService.response(any<String>()) } returnsMany
                listOf(firstAdd, "anything else?", secondAdd, "anything else?")

        // When
        val firstResponse = chatManager.response("add a comment")
        val secondResponse = chatManager.response("add another comment")

        // Then
        firstResponse.tip shouldBe commentVariableTip("Glucose")
        secondResponse.tip shouldBe null
    }

    @Test
    fun `should not show the comment variable tip when the first comment already uses a variable`() = runTest {
        // Given
        val initialResponseFromModel =
            ActionComment(USER_ACTION, message = "What do you want to add?").toJsonString()
        coEvery { conversationService.startConversation() } returns initialResponseFromModel
        chatManager.startConversation(viewableCase) //to set the current case

        val addComment = ActionComment(action = ADD_COMMENT, comment = "The TSH is {TSH}.").toJsonString()
        coEvery { conversationService.response(any<String>()) } returnsMany listOf(addComment, "anything else?")

        // When
        val responseToUser = chatManager.response("add a comment")

        // Then
        responseToUser.tip shouldBe null
    }

    @Test
    fun `should not show the comment variable tip for a later plain comment once a variable has been used`() = runTest {
        // Given
        val initialResponseFromModel =
            ActionComment(USER_ACTION, message = "What do you want to add?").toJsonString()
        coEvery { conversationService.startConversation() } returns initialResponseFromModel
        chatManager.startConversation(viewableCase) //to set the current case

        val addWithVariable = ActionComment(action = ADD_COMMENT, comment = "The TSH is {TSH}.").toJsonString()
        val addPlain = ActionComment(action = ADD_COMMENT, comment = "Go to Bondi.").toJsonString()
        coEvery { conversationService.response(any<String>()) } returnsMany
                listOf(addWithVariable, "anything else?", addPlain, "anything else?")

        // When - the user adds a comment using a variable, then a plain comment
        val firstResponse = chatManager.response("add a comment with a variable")
        val secondResponse = chatManager.response("add a plain comment")

        // Then - having used the facility, the user is never shown the tip this session
        firstResponse.tip shouldBe null
        secondResponse.tip shouldBe null
    }

    @Test
    fun `should start a rule session for removing a comment`() =
        runTest {
            // Given
            val initialResponseFromModel =
                ActionComment(USER_ACTION, message = "What do you want to remove?").toJsonString()
            coEvery { conversationService.startConversation() } returns initialResponseFromModel
            chatManager.startConversation(viewableCase) //to set the current case

            val comment = "Go to Bondi."
            val responseFromModelToRemoveComment = ActionComment(
                action = REMOVE_COMMENT,
                comment = comment,
            ).toJsonString()

            coEvery { conversationService.response(any<String>()) } answers {
                responseFromModelToRemoveComment
            } andThenAnswer {
                "anything else?"
            }

            // When
            chatManager.response("")

            // Then
            coVerify { ruleService.startRuleSessionToRemoveComment(viewableCase, comment) }
        }

    @Test
    fun `should start a rule session for replacing a comment`() =
        runTest {
            // Given
            val initialResponseFromModel =
                ActionComment(action = USER_ACTION, message = "What do you want to replace?").toJsonString()
            coEvery { conversationService.startConversation() } returns initialResponseFromModel
            chatManager.startConversation(viewableCase) //to set the current case

            val comment = "Go to Bondi."
            val replacementComment = "Go to Manly."
            val responseFromModelToReplaceComment = ActionComment(
                action = REPLACE_COMMENT,
                comment = comment,
                replacementComment = replacementComment,
            ).toJsonString()

            coEvery { conversationService.response(any<String>()) } answers {
                responseFromModelToReplaceComment
            } andThenAnswer {
                "anything else?"
            }

            // When
            chatManager.response("")

            // Then
            coVerify {
                ruleService.startRuleSessionToReplaceComment(
                    viewableCase,
                    comment,
                    replacementComment
                )
            }
        }

    @Test
    fun `should log an unknown action from the model`() =
        runTest {
            // Given
            val initialResponseFromModel =
                ActionComment(USER_ACTION, message = "What do you want to replace?").toJsonString()
            coEvery { conversationService.startConversation() } returns initialResponseFromModel
            chatManager.startConversation(viewableCase) //to set the current case

            val comment = "Go to Bondi."
            val replacementComment = "Go to Manly."
            val responseFromModelToReviewCornerstones = """
                {
                    "action": "unknown",
                    "comment": "x",
                }
            """.trimIndent()

            coEvery { conversationService.response(any<String>()) } answers {
                responseFromModelToReviewCornerstones
            }

            // When
            chatManager.response("")

            // Then
            coVerify(exactly = 0) {
                ruleService.startRuleSessionToReplaceComment(
                    viewableCase,
                    comment,
                    replacementComment
                )
            }
        }

    @Test
    fun `should handle undo rule action`() = runTest {
        val responseFromModel = ActionComment(UNDO_LAST_RULE).toJsonString()
        coEvery { conversationService.response(any<String>()) } answers {
            responseFromModel
        }

        chatManager.response("blah")

        coVerify { ruleService.undoLastRuleSession() }
    }

    @Test
    fun `should handle attribute reorder action`() = runTest {
        val responseFromModel =
            ActionComment(action = MOVE_ATTRIBUTE, attributeMoved = "Glucose", destination = "Lipids").toJsonString()
        coEvery { conversationService.response(any<String>()) } answers {
            responseFromModel
        }

        chatManager.response("blah")

        coVerify { ruleService.moveAttributeTo("Glucose",  "Lipids") }
    }

    @Test
    fun `should prepend the current cornerstone status to user messages while a rule session is active`() = runTest {
        // Given
        every { ruleService.isRuleSessionActive() } returns true
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus()
        val responseFromModel = ActionComment(USER_ACTION, message = "ok").toJsonString()
        coEvery { conversationService.response(any<String>()) } returns responseFromModel

        // When
        chatManager.response("no")

        // Then - the model receives the current cornerstone status alongside the user's message,
        // so it can never act on a stale Total from earlier in the conversation.
        coVerify {
            conversationService.response(match<String> {
                it.startsWith(CURRENT_CORNERSTONE_STATUS_PREFIX) && it.endsWith("\nno")
            })
        }
    }

    @Test
    fun `should not prepend cornerstone status when no rule session is active`() = runTest {
        // Given
        every { ruleService.isRuleSessionActive() } returns false
        val responseFromModel = ActionComment(USER_ACTION, message = "hi").toJsonString()
        coEvery { conversationService.response(any<String>()) } returns responseFromModel

        // When
        chatManager.response("hello")

        // Then
        coVerify { conversationService.response("hello") }
    }

    @Test
    fun `should handle a user message with apostrophe`() = runTest {
        // Given
        val message = "Let's surf"

        val responseFromModel = """
            {
                "action": "$USER_ACTION",
                "message": "Please confirm you want to add the comment '$message'"
            }
        """.trimIndent()
        coEvery { conversationService.response(any()) } returns responseFromModel


        // When
        val responseToUser = chatManager.response(message)

        // Then
        responseToUser shouldBe ChatResponse("Please confirm you want to add the comment '$message'")
    }

    @Test
    fun `should only process the first json fragment when multiple are returned`() = runTest {
        // Given
        val response = """
            {"action": "CommitRule"}
            {"action": "CancelRule"}
        """.trimIndent()
        val message = "test message"

        coEvery { conversationService.response(message) } returns response
        coEvery { ruleService.commitCurrentRuleSession() } returns Unit
        coEvery { ruleService.sendRuleSessionCompleted() } returns Unit

        // When
        chatManager.response(message)

        // Then
        coVerify { ruleService.commitCurrentRuleSession() }
        coVerify(exactly = 0) { ruleService.cancelCurrentRuleSession() }
    }


    @Test
    fun `should sanitize escaped apostrophe in response`() {
        // Given
        val input = """
            {
                "action": "UserAction",
                "message": "Please confirm that you want to add the comment: 'Let\'s surf.'"
            }
        """.trimIndent()

        // When
        val result = input.sanitizeLlmJson()

        // Then
        result shouldBe """
            {
                "action": "UserAction",
                "message": "Please confirm that you want to add the comment: 'Let's surf.'"
            }
        """.trimIndent()
    }

    @Test
    fun `should sanitize multiple escaped apostrophes`() {
        // Given
        val input = """
            {
                "action": "UserAction",
                "message": "It\'s the patient\'s result that\'s important."
            }
        """.trimIndent()

        // When
        val result = input.sanitizeLlmJson()

        // Then
        result shouldBe """
            {
                "action": "UserAction",
                "message": "It's the patient's result that's important."
            }
        """.trimIndent()
    }

    @Test
    fun `should not alter json without escaped apostrophes`() {
        // Given
        val input = """
            {
                "action": "UserAction",
                "message": "No apostrophes here."
            }
        """.trimIndent()

        // When
        val result = input.sanitizeLlmJson()

        // Then
        result shouldBe input
    }

    @Test
    fun `should return unavailable message when startConversation throws`() = runTest {
        // Given
        val exception = RuntimeException("429 Resource exhausted")
        coEvery { conversationService.startConversation() } throws exception

        // When
        val response = chatManager.startConversation(viewableCase)

        // Then
        response shouldBe ChatResponse(AI_UNAVAILABLE_MESSAGE)
        coVerify { logger.error("Failed to start conversation", exception) }
    }

    @Test
    fun `should return unavailable message when response throws`() = runTest {
        // Given
        val exception = RuntimeException("429 Resource exhausted")
        coEvery { conversationService.response(any()) } throws exception

        // When
        val response = chatManager.response("hello")

        // Then
        response shouldBe ChatResponse(AI_UNAVAILABLE_MESSAGE)
        coVerify { logger.error("Failed to send message: hello", exception) }
    }

    @Test
    fun `should not alter valid json backslash sequences`() {
        // Given
        val input = """
            {
                "action": "UserAction",
                "message": "line1\nline2"
            }
        """.trimIndent()

        // When
        val result = input.sanitizeLlmJson()

        // Then
        result shouldBe input
    }

    @Test
    fun `should convert double-escaped newline to proper json newline`() {
        // Given - LLM outputs \\n (literal backslash-n) instead of \n (JSON newline)
        val input = """
            {
                "action": "UserAction",
                "message": "1. Go to Bondi.\\n"
            }
        """.trimIndent()

        // When
        val result = input.sanitizeLlmJson()

        // Then
        result shouldBe """
            {
                "action": "UserAction",
                "message": "1. Go to Bondi.\n"
            }
        """.trimIndent()
    }

}
