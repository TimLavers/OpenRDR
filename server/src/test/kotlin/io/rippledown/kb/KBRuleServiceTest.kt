package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Is
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.utils.shouldBeSameAs
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBRuleServiceTest: KBTestBase(){
    private lateinit var ruleService: RuleService

    @BeforeTest
    override fun setup() {
        super.setup()
        ruleService = kb.ruleService
    }

    @Test
    fun `should return the condition for a user expression`() = runTest {
        //Given
        val x = kb.attributeManager.getOrCreate("x")
        val value = "42"
        val viewableCase = createCase("Case", attribute = x, value = value)
        val userExpression = "X equates to $value"

        //When
        val conditionParsingResult = ruleService.conditionForExpression(viewableCase.case, userExpression)

        //Then
        val expectedCondition = EpisodicCondition(
            null,
            x,
            Is(value),
            Current,
            userExpression
        )
        conditionParsingResult.isFailure shouldBe false
        conditionParsingResult.condition shouldBeSameAs expectedCondition
    }
}