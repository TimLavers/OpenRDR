package io.rippledown.kb.chat.action

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AddCommentTest : ActionTestBase() {
    val commentToAdd = "Beach today!"
    val expression1 = "If the sun is hot."
    val expression2 = "If the waves are good."
    val condition1 = mockk<Condition>()
    val condition2 = mockk<Condition>()
    val conditionParsingResult1 = ConditionParsingResult(condition1)
    val conditionParsingResult2 = ConditionParsingResult(condition2)

    @Test
    fun `no conditions`() {
        runTest {
            AddComment(commentToAdd, emptyList()).doIt(ruleService, currentCase)
            coVerify { ruleService.buildRuleToAddComment( currentCase, commentToAdd, emptyList()) }
        }
    }

    @Test
    fun `conditions are parsed`() = runTest {
        coEvery { ruleService.conditionForExpression(currentCase.case, expression1) } returns conditionParsingResult1
        coEvery { ruleService.conditionForExpression(currentCase.case, expression2) } returns conditionParsingResult2
        AddComment(commentToAdd, listOf(expression1, expression2)).doIt(ruleService, currentCase)

        coVerify { ruleService.buildRuleToAddComment(currentCase, commentToAdd, eq(listOf(condition1, condition2))) }
    }
}