package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.diff.Addition
import io.rippledown.model.rule.BuildRuleRequest
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import kotlin.test.Test

class RepeatInferencingBackdoorTest {

    private val kb = KB(InMemoryKB(KBInfo("id", "TestKB")))
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)
    private val rsm = RuleSessionManager(kb, webSocketManager)

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun addProcessedCase(name: String, glucoseValue: String = "12.0"): String {
        val builder = RDRCaseBuilder()
        builder.addValue(glucose(), defaultDate, glucoseValue)
        val kase = builder.build(name)
        kb.addProcessedCase(kase)
        return name
    }

    @Test
    fun `a comment rule can be conditioned on a derived value assigned by another rule`() {
        val caseName = addProcessedCase("Fermi")

        rsm.buildRule(
            BuildRuleRequest(
                caseName,
                Addition(""),
                listOf("Glucose \u2265 11.0"),
                "Diabetes status",
                "\"diabetic\""
            )
        )

        rsm.buildRule(
            BuildRuleRequest(
                caseName,
                Addition("Diabetic diet advice given."),
                listOf("Diabetes status is \"diabetic\""),
            )
        )

        val processed = kb.getProcessedCaseByName(caseName)
        val viewable = kb.viewableCase(processed)
        viewable.viewableInterpretation.latestText() shouldBe "Diabetic diet advice given."

        val diabetesStatus = kb.attributeManager.byName("Diabetes status")!!
        viewable.case.latestValue(diabetesStatus) shouldBe "diabetic"
    }
}
