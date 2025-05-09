package io.rippledown.chat

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.model.rule.Rule
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class GeminiChatTest {

    @Test
    fun `model should respond by identifying the comments in a json string corresponding to a case`() = runTest {
        //Given
        val json = jsonForCaseWithComments()

        //When
        val response = responseFor("list the comments in this json", json)

        println("Response: $response")
    }

    fun jsonForCaseWithComments(): String {
        var id = 0
        val tsh = Attribute(1, "TSH")
        val ft4 = Attribute(1, "FT4")
        val case = with(RDRCaseBuilder()) {
            addValue(tsh, 1_000, "0.667")
            addValue(ft4, 1_000, "0.8")
            build("Case1234", 0)
        }


        val conclusion1 = Conclusion(++id, "TSH is within the normal range.")
        val conclusion2 = Conclusion(++id, "FT4 is within the normal range.")
        val root = Rule(++id, null, null, emptySet(), mutableSetOf())
        val condition1 = lessThanOrEqualTo(++id, tsh, 4.0)
        val condition2 = lessThanOrEqualTo(++id, ft4, 1.9)
        val rule1 = Rule(++id, root, conclusion1, setOf(condition1), mutableSetOf())
        val rule2 = Rule(++id, root, conclusion2, setOf(condition2), mutableSetOf())
        case.interpretation.add(rule1)
        case.interpretation.add(rule2)
        case.interpretation.conclusions() shouldContainExactlyInAnyOrder setOf(conclusion1, conclusion2)

        val s = case.toJsonString()
        println("s = ${s}")
        return s
    }

}

