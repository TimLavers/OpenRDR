package io.rippledown.kb.chat.action

import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import kotlin.test.BeforeTest

open class ActionTestBase {

    lateinit var ruleService: RuleService
    lateinit var currentCase: ViewableCase
    lateinit var modelResponder: ModelResponder
    val commentToAdd = "Beach today!"
    val commentToRemove = "Bushwalk today!"
    val expression1 = "If the sun is hot."
    val expression2 = "If the waves are good."
    val condition1 = mockk<Condition>()
    val condition2 = mockk<Condition>()
    val conditionParsingResult1 = ConditionParsingResult(condition1)
    val conditionParsingResult2 = ConditionParsingResult(condition2)
    val conditionParsingFailedResult = ConditionParsingResult(null, "Just because")
    val conditionParsingFailedResult2 = ConditionParsingResult(null, "Whatever")

    @BeforeTest
    fun setUp() {
        ruleService = mockk()
        currentCase = mockk()
        modelResponder = mockk()
    }
}