package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.RuleService
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class UndoRuleTest: ActionTestBase() {
    @Test
    fun `undoes last rule`() = runTest{
        UndoLastRule().doIt(ruleService,currentCase)
        coVerify { ruleService.undoLastRule() }
    }
}