package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.beBlank
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.chat.conversation.ConversationService
import io.rippledown.constants.chat.*
import io.rippledown.model.RDRCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChatManagerTest {
    lateinit var conversationService: ConversationService
    lateinit var ruleService: RuleService
    lateinit var case: RDRCase
    lateinit var chatManager: ChatManager

    @BeforeTest
    fun setUp() {
        conversationService = mockk(relaxed = true)
        ruleService = mockk(relaxed = true)
        case = mockk(relaxed = true)
        chatManager = ChatManager(conversationService, ruleService)
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
        coEvery { conversationService.startConversation(case) } returns responseFromModel

        // When
        chatManager.startConversation(case)

        // Then
        coVerify { conversationService.startConversation(case) }
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
        coEvery { conversationService.startConversation(case) } returns responseFromModel

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
    fun `should build a rule to add a comment from a response from the conversation service`() = runTest {
        // Given
        val startConversationResponse = """
            {
                "action": "$USER_ACTION",
                "new_comment": "Do you want to add a comment?",
            }
        """.trimIndent()
        coEvery { conversationService.startConversation(case) } returns startConversationResponse

        val comment = "the answer is 42"
        val responseFromModel = """
            {
                "action": "$ADD_ACTION",
                "new_comment": "$comment",
            }
        """.trimIndent()
        val expectedResponseToUser = CHAT_BOT_ADD_COMMENT_USER_MESSAGE.replace(ADD_COMMENT_PLACEHOLDER, comment)
        coEvery { conversationService.response(any<String>()) } returns responseFromModel

        // When
        chatManager.startConversation(case)
        val responseToUser = chatManager.response("meaning of life?")

        // Then
        coVerify { ruleService.buildRuleToAddComment(case, comment) }
        responseToUser shouldBe expectedResponseToUser
    }


}