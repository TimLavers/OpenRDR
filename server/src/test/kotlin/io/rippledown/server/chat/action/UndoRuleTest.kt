package io.rippledown.server.chat.action

import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UndoRuleTest: ActionTestBase() {
    @Test
    fun `undoes last rule`() = runTest{
        UndoLastRule().doIt(ruleService, currentCase, modelResponder)
        coVerify { ruleService.undoLastRuleSession() }
    }
}