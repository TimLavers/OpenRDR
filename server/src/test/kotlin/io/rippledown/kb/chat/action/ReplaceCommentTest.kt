package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.rippledown.model.condition.Condition
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ReplaceCommentTest : ActionTestBase() {
    @Test
    fun `build rule`() {
        val conditions = listOf<Condition>(condition1, condition2)
        runTest {
            ReplaceComment(commentToRemove, commentToAdd,emptyList()).buildRule(ruleService, currentCase, conditions)
            coVerify { ruleService.buildRuleToReplaceComment( currentCase, commentToRemove, commentToAdd, conditions) }
        }
    }
}