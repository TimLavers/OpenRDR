package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MoveAttributeTest : ActionTestBase() {
    @Test
    fun `moves attribute`() {
        runTest {
            MoveAttribute("Glucose", "Lipids").doIt(ruleService, currentCase)
            coVerify { ruleService.moveAttributeTo("Glucose", "Lipids") }
        }
    }
}