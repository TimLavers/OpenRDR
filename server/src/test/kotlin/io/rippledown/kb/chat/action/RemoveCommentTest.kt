package io.rippledown.kb.chat.action

import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RemoveCommentTest : ActionTestBase() {
    @Test
    fun `build rule`() {
        val conditions = listOf(condition1, condition2)
        runTest {
            RemoveComment(commentToRemove,emptyList()).buildRule(ruleService, currentCase, conditions)
            coVerify { ruleService.buildRuleToRemoveComment( currentCase, commentToRemove, conditions) }
        }
    }
}