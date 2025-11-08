package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UserActionTest : ActionTestBase() {
    @Test
    fun `should return the user message`() = runTest {
        //Given
        val message = "The answer is 42"

        //When
        val action = UserAction(message)

        //Then
        action.doIt(ruleService, currentCase, modelResponder) shouldBe message
    }
}