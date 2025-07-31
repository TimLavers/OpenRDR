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
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import org.slf4j.Logger
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

class ChatManagerTest {
    lateinit var logger: Logger
    lateinit var conversationService: ConversationService
    lateinit var ruleService: RuleService
    lateinit var case: RDRCase
    lateinit var chatManager: ChatManager

    @BeforeTest
    fun setUp() {
        conversationService = mockk()
        ruleService = mockk()
        case = mockk()
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
        chatManager.startConversation(case)

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
        chatManager.startConversation(case)

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
        val responseToUser = chatManager.startConversation(case)

        // Then
        responseToUser shouldBe message
    }

    @Test
    fun `should return a user message from a response call`() = runTest {
        // Given
        val message = "the answer is 42"
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
    fun `should build a rule to add a comment with conditions from a response from the conversation service`() =
        runTest {
            // Given
            val message = "What do you want to add?"
            val initialResponseFromModel = ActionComment(USER_ACTION, message = message).toJsonString()
            coEvery { conversationService.startConversation() } returns initialResponseFromModel
            chatManager.startConversation(case)

            val comment = "Go to Bondi."
            val expression1 = "If the sun is hot."
            val expression2 = "If the waves are good."
            val condition1 = mockk<Condition>()
            val condition2 = mockk<Condition>()
            val conditionParsingResult1 = ConditionParsingResult(condition1)
            val conditionParsingResult2 = ConditionParsingResult(condition2)
            val responseFromModel = ActionComment(
                action = ADD_ACTION,
                comment = comment,
                reasons = listOf(expression1, expression2)
            ).toJsonString()
            coEvery { conversationService.response(any<String>()) } returns responseFromModel
            coEvery { ruleService.conditionForExpression(case, expression1) } returns conditionParsingResult1
            coEvery { ruleService.conditionForExpression(case, expression2) } returns conditionParsingResult2

            // When
            val responseToUser = chatManager.response("yes!")

            // Then
            coVerify { ruleService.buildRuleToAddComment(case, comment, eq(listOf(condition1, condition2))) }
            responseToUser shouldBe CHAT_BOT_DONE_MESSAGE
        }

    @Test
    fun `should build a rule to remove a comment with conditions from a response from the conversation service`() =
        runTest {
            // Given
            val message = "What do you want to remove?"
            val initialResponseFromModel = ActionComment(USER_ACTION, message = message).toJsonString()
            coEvery { conversationService.startConversation() } returns initialResponseFromModel
            chatManager.startConversation(case)

            val comment = "Go to Bondi."
            val expression1 = "If the sun is hot."
            val condition1 = mockk<Condition>()
            val conditionParsingResult1 = ConditionParsingResult(condition1)
            val responseFromModel = ActionComment(
                action = REMOVE_ACTION,
                comment = comment,
                reasons = listOf(expression1)
            ).toJsonString()
            coEvery { conversationService.response(any<String>()) } returns responseFromModel
            coEvery { ruleService.conditionForExpression(case, expression1) } returns conditionParsingResult1

            // When
            val responseToUser = chatManager.response("yes!")

            // Then
            coVerify { ruleService.buildRuleToRemoveComment(case, comment, eq(listOf(condition1))) }
            responseToUser shouldBe CHAT_BOT_DONE_MESSAGE
        }

    @Test
    @Ignore //TODO NOT YET IMPLEMENTED
    fun `should start another conversation after building a rule`() =
        runTest {
            // Given
            val message = "What do you want to add?"
            val initialResponseFromModel = ActionComment(USER_ACTION, message = message).toJsonString()
            coEvery { conversationService.startConversation() } returns initialResponseFromModel
            chatManager.startConversation(case)
            coVerify(exactly = 1) { conversationService.startConversation() }

            val comment = "Go to Bondi."
            val expression = "If the sun is hot."
            val condition = mockk<Condition>()
            val conditionParsingResult = ConditionParsingResult(condition)
            val responseFromModel = ActionComment(
                action = ADD_ACTION,
                comment = comment,
                reasons = listOf(expression)
            ).toJsonString()
            coEvery { conversationService.response(any<String>()) } returns responseFromModel
            coEvery { ruleService.conditionForExpression(case, expression) } returns conditionParsingResult

            // When
            val responseToUser = chatManager.response("yes!")
            coVerify { ruleService.buildRuleToAddComment(case, comment, eq(listOf(condition))) }
            responseToUser shouldBe CHAT_BOT_DONE_MESSAGE

            // Then
            coVerify(exactly = 2) { conversationService.startConversation() }
        }

    @Test
    fun `should build a rule to add a comment with no conditions from a response from the conversation service`() =
        runTest {
            // Given
            val message = "What do you want to add?"
            val initialResponseFromModel = ActionComment(USER_ACTION, message = message).toJsonString()
            coEvery { conversationService.startConversation() } returns initialResponseFromModel

            chatManager.startConversation(case)
            val comment = "Go to Bondi."
            val responseFromModel = ActionComment(
                action = ADD_ACTION,
                comment = comment
            ).toJsonString()
            coEvery { conversationService.response(any<String>()) } returns responseFromModel

            // When
            chatManager.response("yes!")

            // Then
            coVerify { ruleService.buildRuleToAddComment(case, comment, emptyList()) }
        }


}