package io.rippledown.kb

import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.rippledown.kb.chat.ChatContext
import io.rippledown.kb.chat.ChatCoordinator
import io.rippledown.kb.chat.ChatManagerFactory
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.server.KBEndpoint
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBModelIntegrationTest : KBTestBase() {
    private lateinit var coordinator: ChatCoordinator
    private lateinit var endpoint: KBEndpoint

    @BeforeTest
    override fun setup() {
        super.setup()
        val kbService = mockk<KnowledgeBaseService>()
        every { kbService.knowledgeBases() } returns listOf(kb.kbInfo)
        coordinator = ChatCoordinator(ChatManagerFactory(kbService), kbService)
        endpoint = KBEndpoint(session)
    }

    private suspend fun startConversation(case: ViewableCase) =
        coordinator.startConversation(ChatContext.CaseInKnowledgeBase(endpoint, case))

    @Test
    fun `should delegate starting a conversation to the ChatManager using Gemini`() = runTest {
        //Given
        val case = createCase("Case")

        //When
        val response = startConversation(case)

        //Then
        response.text shouldContain "Would you like to add a comment"//todo use some known constant
    }

    @Test
    fun `should delegate user message to the ChatManager using Gemini`() = runTest {
        //Given
        val case = createCase("Case")
        startConversation(case)
        val userExpression = "Please add the comment \"Go to Bondi.\"."

        //When
        val response = coordinator.responseToUserMessage(userExpression)

        //Then
        // The model asks for a reason for the comment, sometimes offering
        // suggestions and sometimes not, so only the request itself is asserted.
        response.text.lowercase() shouldContain "reason"
    }
}