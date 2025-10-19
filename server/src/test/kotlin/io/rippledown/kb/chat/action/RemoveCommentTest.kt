package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.rippledown.model.condition.Condition
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RemoveCommentTest : ActionTestBase() {
    @Test
    fun `build rule`() {
        val conditions = listOf<Condition>(condition1, condition2)
        runTest {
            RemoveComment(commentToRemove,emptyList()).buildRule(ruleService, currentCase, conditions)
            coVerify { ruleService.buildRuleToRemoveComment( currentCase, commentToRemove, conditions) }
        }
    }
}