package io.rippledown.kb

import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBModelIntegrationTest: KBTestBase() {

    @BeforeTest
    override fun setup() {
        super.setup()
    }

    @Test
    fun `should delegate starting a conversation to the ChatManager using Gemini`() = runTest {
        //Given
        val case = createCase("Case")

        //When
        val response = session.startConversation(case)

        //Then
        response.text shouldContain "Would you like to add a comment"//todo use some known constant
    }

    @Test
    fun `should delegate user message to the ChatManager using Gemini`() = runTest {
        //Given
        val case = createCase("Case")
        session.startConversation(case)
        val userExpression = "Please add the comment 'Go to Bondi.'."

        //When
        val response = session.responseToUserMessage(userExpression)

        //Then
        response.text.lowercase() shouldContain "confirm"
    }
}