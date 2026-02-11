package io.rippledown.kb

import io.kotest.matchers.shouldBe
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
TODO() // move this
        //When
//        val response = kb.startConversation(case)

        //Then
//        response shouldBe "Would you like to add a comment to the report?"//todo use some known constant
    }

    @Test
    fun `should delegate user message to the ChatManager using Gemini`() = runTest {
        //Given
        val case = createCase("Case")
        TODO() // move this
//        kb.startConversation(case)
        val userExpression = "Please add the comment 'Go to Bondi.'."

        //When
//        val response = kb.responseToUserMessage(userExpression)

        //Then
//        response shouldBe "Please confirm that you want to add the comment: 'Go to Bondi.'"
    }
}