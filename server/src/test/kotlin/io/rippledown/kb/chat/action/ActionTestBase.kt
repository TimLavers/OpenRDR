package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

open class ActionTestBase {

    lateinit var ruleService: RuleService
    lateinit var currentCase: ViewableCase

    @BeforeTest
    fun setUp() {
        ruleService = mockk()
        currentCase = mockk()
    }
}