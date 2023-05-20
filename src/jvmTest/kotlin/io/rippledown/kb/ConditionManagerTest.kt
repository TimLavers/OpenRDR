package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.model.condition.HasCurrentValue
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test

class ConditionManagerTest {

    val a1 = Attribute("A1")
    val a2 = Attribute("A2")
    lateinit var manager: ConditionManager

    @BeforeTest
    fun setup() {
        manager = ConditionManager()
    }

    @Test
    fun `should return IsAvailable for every attribute that is available in the case`() {
        val case1Attributes = listOf(a1)
        val viewableCase = createCase(case1Attributes)
        val conditionHints = manager.conditionHintsForCase(viewableCase)
        conditionHints.conditionList.size shouldBe 3
        conditionHints.conditionList[0] shouldBe HasCurrentValue(a1)
    }


    private fun createCase(attributes: List<Attribute>): RDRCase {
        val date = Instant.now()
        val builder = RDRCaseBuilder()
        attributes.forEach {
            builder.addResult(it, date.toEpochMilli(), TestResult(it.name + " value"))
        }
        return builder.build("")
    }

}