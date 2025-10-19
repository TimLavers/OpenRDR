package io.rippledown.kb.chat.action

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.rippledown.constants.chat.CHAT_BOT_DONE_MESSAGE
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AddCommentTest : ActionTestBase() {
    @Test
    fun `build rule`() {
        val conditions = listOf<Condition>(condition1, condition2)
        runTest {
            AddComment(commentToAdd,emptyList()).buildRule(ruleService, currentCase, conditions)
            coVerify { ruleService.buildRuleToAddComment( currentCase, commentToAdd, conditions) }
        }
    }

    @Test
    fun `no conditions`() {
        runTest {
            AddComment(commentToAdd, emptyList()).doIt(ruleService, currentCase)
            coVerify { ruleService.buildRuleToAddComment( currentCase, commentToAdd, emptyList()) }
        }
    }

    @Test
    fun `null conditions`() {
        runTest {
            AddComment(commentToAdd, null).doIt(ruleService, currentCase)
            coVerify { ruleService.buildRuleToAddComment( currentCase, commentToAdd, emptyList()) }
        }
    }

    @Test
    fun `conditions are parsed`() = runTest {
        coEvery { ruleService.conditionForExpression(currentCase.case, expression1) } returns conditionParsingResult1
        coEvery { ruleService.conditionForExpression(currentCase.case, expression2) } returns conditionParsingResult2
        val returnMessage = AddComment(commentToAdd, listOf(expression1, expression2)).doIt(ruleService, currentCase)

        coVerify { ruleService.buildRuleToAddComment(currentCase, commentToAdd, eq(listOf(condition1, condition2))) }
        returnMessage shouldBe CHAT_BOT_DONE_MESSAGE
    }

    @Test
    fun `first condition cannot be parsed`() = runTest {
        coEvery { ruleService.conditionForExpression(currentCase.case, expression1) } returns conditionParsingFailedResult
        coEvery { ruleService.conditionForExpression(currentCase.case, expression2) } returns conditionParsingResult2
        val returnMessage = AddComment(commentToAdd, listOf(expression1, expression2)).doIt(ruleService, currentCase)

        coVerify(inverse = true) { ruleService.buildRuleToAddComment(any(), any(), any()) }
        returnMessage shouldBe "Failed to parse condition: ${conditionParsingFailedResult.errorMessage}"
    }

    @Test
    fun `subsequent condition cannot be parsed`() = runTest {
        coEvery { ruleService.conditionForExpression(currentCase.case, expression1) } returns conditionParsingResult1
        coEvery { ruleService.conditionForExpression(currentCase.case, expression2) } returns conditionParsingFailedResult
        val returnMessage = AddComment(commentToAdd, listOf(expression1, expression2)).doIt(ruleService, currentCase)

        coVerify(inverse = true) { ruleService.buildRuleToAddComment(any(), any(), any()) }
        returnMessage shouldBe "Failed to parse condition: ${conditionParsingFailedResult.errorMessage}"
    }

    @Test
    fun `no condition can be parsed`() = runTest {
        coEvery { ruleService.conditionForExpression(currentCase.case, expression1) } returns conditionParsingFailedResult
        coEvery { ruleService.conditionForExpression(currentCase.case, expression2) } returns conditionParsingFailedResult2
        val returnMessage = AddComment(commentToAdd, listOf(expression1, expression2)).doIt(ruleService, currentCase)

        coVerify(inverse = true) { ruleService.buildRuleToAddComment(any(), any(), any()) }
        returnMessage shouldBe "Failed to parse condition: ${conditionParsingFailedResult.errorMessage}"
    }

    @Test
    fun `handle inconsistent condition parsing result`() = runTest {
        val inconsistentParsingResult = ConditionParsingResult(null, null)

        coEvery { ruleService.conditionForExpression(currentCase.case, expression1) } returns inconsistentParsingResult

        shouldThrow<IllegalStateException> {
            AddComment(commentToAdd, listOf(expression1)).doIt(ruleService, currentCase)
        }.message shouldBe "Condition should not be null for a successful parsing result"
    }
}