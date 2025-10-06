package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.RuleService
import kotlin.test.BeforeTest
import kotlin.test.Test

class MoveAttributeTest {

    lateinit var ruleService: RuleService
    @BeforeTest
    fun setUp() {
        ruleService = mockk()
    }

    @Test
    fun `moves attribute`() {
        MoveAttribute("Glucose", "Lipids").doIt(ruleService)
        coVerify { ruleService.moveAttributeTo("Glucose",  "Lipids") }
    }
}