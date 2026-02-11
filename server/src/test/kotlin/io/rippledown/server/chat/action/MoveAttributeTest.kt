package io.rippledown.server.chat.action

import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MoveAttributeTest : ActionTestBase() {
    @Test
    fun `moves attribute`() {
        runTest {
            MoveAttribute("Glucose", "Lipids").doIt(ruleService, currentCase, modelResponder)
            coVerify { ruleService.moveAttributeTo("Glucose", "Lipids") }
        }
    }
}