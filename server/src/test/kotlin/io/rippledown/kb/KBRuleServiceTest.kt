package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.*
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Is
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import io.rippledown.utils.shouldBeSameAs
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBRuleServiceTest {
    private lateinit var kb: KB
    private lateinit var ruleService: RuleService

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "123")
        kb = KB(InMemoryKB(kbInfo))
        ruleService = kb.ruleService
    }

    @Test
    fun `should return the condition for a user expression`() = runTest {
        //Given
        val x = kb.attributeManager.getOrCreate("x")
        val value = "42"
        val case = createCase("Case", attribute = x, value = value)
        val userExpression = "X equates to $value"

        //When
        val conditionParsingResult = ruleService.conditionForExpression(case, userExpression)

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

    private fun createCase(
        caseName: String,
        attribute: Attribute,
        value: String,
    ): RDRCase {
        with(RDRCaseBuilder()) {
            val testResult = TestResult(value)
            addResult(attribute, defaultDate, testResult)
            return build(caseName)
        }
    }
}