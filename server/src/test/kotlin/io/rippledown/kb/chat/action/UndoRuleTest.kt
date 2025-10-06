package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.RuleService
import kotlin.test.BeforeTest
import kotlin.test.Test

class UndoRuleTest {

    lateinit var ruleService: RuleService
    @BeforeTest
    fun setUp() {
        ruleService = mockk()
    }

    @Test
    fun `undoes las rule`() {
        UndoLastRule().doIt(ruleService)
        coVerify { ruleService.undoLastRule() }
    }
}