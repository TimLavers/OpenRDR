package io.rippledown.server.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.constants.chat.CHAT_BOT_DONE_MESSAGE
import io.rippledown.server.chat.ModelResponder
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class CommitRuleTest {
    private lateinit var ruleService: KbEditInterface

    @BeforeTest
    fun setup() {
        ruleService = mockk()
    }

    @Test
    fun `should commit a rule session`() = runTest {
        //Given
        val action = CommitRule()

        //When
        val response = action.doIt(ruleService, mockk<ViewableCase>(), mockk<ModelResponder>())

        //Then
        coVerify { ruleService.commitCurrentRuleSession() }
        response shouldBe CHAT_BOT_DONE_MESSAGE
    }

    @Test
    fun `should send a rule session completed message after committing a rule session`() = runTest {
        //Given
        val action = CommitRule()

        //When
        action.doIt(ruleService, mockk<ViewableCase>(), mockk<ModelResponder>())

        //Then
        coVerify { ruleService.sendRuleSessionCompleted() }
    }
}