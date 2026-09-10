package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.constants.rule.CONDITION_IS_NOT_TRUE
import io.rippledown.constants.rule.NOT_A_VALID_VALUE
import io.rippledown.model.Attribute
import io.rippledown.model.condition.edit.EditableGreaterThanEqualsCondition
import io.rippledown.model.condition.edit.EditableLessThanEqualsCondition
import io.rippledown.model.condition.edit.EditableValue
import io.rippledown.model.condition.edit.Type
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.server.websocket.WebSocketManager
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The value of an editable suggestion is substituted by the server, so that
 * neither the model nor the user has to transcribe the condition's text.
 */
class RuleSessionManagerEditedSuggestionTest : KBTestBase() {
    private lateinit var rsm: RuleSessionManager
    private lateinit var waves: Attribute

    @BeforeTest
    override fun setup() {
        super.setup()
        rsm = RuleSessionManager(kb, mockk<WebSocketManager>())
        waves = kb.attributeManager.getOrCreate("Waves")
    }

    @Test
    fun `should substitute the value the user gave into the suggestion`() {
        // Given a case whose Waves value is 1.5, and the editable suggestion "Waves ≤ 1.5"
        val case = createCase("Bondi", attribute = waves, value = "1.5")
        val suggestion = EditableLessThanEqualsCondition(waves, EditableValue("1.5", Type.Real), Current)

        // When the user replaces the value with 1.7
        val result = rsm.conditionForEditedSuggestion(case.case, suggestion, "1.7")

        // Then the condition is the suggestion with that value, and it is usable
        result.isFailure shouldBe false
        result.condition?.asText() shouldBe "Waves ≤ 1.7"
    }

    @Test
    fun `should refuse an edited suggestion that is not true for the case`() {
        // Given a case whose Waves value is 1.5, and the editable suggestion "Waves ≥ 1.5"
        val case = createCase("Bondi", attribute = waves, value = "1.5")
        val suggestion = EditableGreaterThanEqualsCondition(waves, EditableValue("1.5", Type.Real), Current)

        // When the user replaces the value with 1.7, which the case does not satisfy
        val result = rsm.conditionForEditedSuggestion(case.case, suggestion, "1.7")

        // Then the condition is refused, in terms of the case rather than of the user's wording
        result.condition shouldBe null
        result.errorMessage shouldBe CONDITION_IS_NOT_TRUE
    }

    @Test
    fun `should refuse a value that the suggestion cannot take`() {
        // Given a case whose Waves value is 1.5, and the editable suggestion "Waves ≤ 1.5"
        val case = createCase("Bondi", attribute = waves, value = "1.5")
        val suggestion = EditableLessThanEqualsCondition(waves, EditableValue("1.5", Type.Real), Current)

        // When the user gives a value that is not a number
        val result = rsm.conditionForEditedSuggestion(case.case, suggestion, "high")

        // Then the value is refused, naming the suggestion as it stands
        result.condition shouldBe null
        result.errorMessage shouldBe NOT_A_VALID_VALUE.format("high", "Waves ≤ 1.5")
    }
}
