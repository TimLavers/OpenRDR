package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.beBlank
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.ConversationService
import io.rippledown.constants.chat.*
import io.rippledown.kb.chat.ChatManager.Companion.LOG_PREFIX_FOR_CONVERSATION_RESPONSE
import io.rippledown.kb.chat.ChatManager.Companion.LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import org.slf4j.Logger
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChatManagerTest {
    lateinit var logger: Logger
    lateinit var conversationService: ConversationService
    lateinit var ruleService: RuleService
    lateinit var case: RDRCase
    lateinit var viewableCase: ViewableCase
    lateinit var chatManager: ChatManager

    @BeforeTest
    fun setUp() {
        conversationService = mockk()
        ruleService = mockk()
        viewableCase = mockk()
        case = mockk()
        every { viewableCase.case } returns case
        chatManager = ChatManager(conversationService, ruleService)
        setupLogger()
    }

    private fun setupLogger() {
        logger = mockk()
        val loggerField = ChatManager::class.java.getDeclaredField("logger")
        loggerField.isAccessible = true
        loggerField.set(chatManager, logger)
        every { logger.isInfoEnabled } returns true
    }

    @Test
    fun `should process an action comment from a start conversation response`() = runTest {
        // Given
        val actionComment = ActionComment(action = USER_ACTION, message = "test response")

        // When
        val responseToUser = chatManager.processActionComment(actionComment)

        // Then
        responseToUser shouldBe actionComment.message
    }

    @Test
    fun `should process an action comment from a response after the conversation has been started`() = runTest {
        // Given
        val actionComment = ActionComment(action = USER_ACTION, message = "test response")

        // When
        val responseToUser = chatManager.processActionComment(actionComment)

        // Then
        responseToUser shouldBe actionComment.message
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
        responseToUser shouldBe message
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
        responseToUser shouldBe message
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
        responseToUser shouldBe beBlank()
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
            coVerify { ruleService.startRuleSessionToAddComment(viewableCase, comment) }
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
        responseToUser shouldBe "Please confirm you want to add the comment '$message'"
    }

    @Test
    fun `should process multiple actions in sequence`() = runTest {
        // Given
        val response = """
            {"action": "ExemptCornerstone"}
            {"action": "CommitRule"}
        """.trimIndent()
        val message = "test message"

        coEvery { conversationService.response(message) } returns response
        coEvery { ruleService.exemptCornerstoneCase() } returns CornerstoneStatus()
        coEvery { ruleService.commitCurrentRuleSession() } returns Unit

        // When
        chatManager.response(message)

        // Then
        coVerify { ruleService.exemptCornerstoneCase() }
        coVerify { ruleService.commitCurrentRuleSession() }
    }



}