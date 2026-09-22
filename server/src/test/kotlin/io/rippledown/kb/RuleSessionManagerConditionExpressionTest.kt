package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.toExpressionTransformation
import io.rippledown.constants.rule.CONDITION_IS_NOT_TRUE
import io.rippledown.constants.rule.DOES_NOT_CORRESPOND_TO_A_CONDITION
import io.rippledown.constants.rule.INTERPRETED_CONDITION_IS_NOT_TRUE
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.server.websocket.WebSocketManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RuleSessionManagerConditionExpressionTest : KBTestBase() {
    private lateinit var rsm: RuleSessionManager
    private val parser = mockk<ConditionParser>()

    @BeforeEach
    override fun setup() {
        super.setup()
        rsm = RuleSessionManager(kb, mockk<WebSocketManager>())
        rsm.setConditionParser(parser)
    }

    @Test
    fun `retains incoming wording when an existing condition has another phrase`() {
        // Given an existing condition and a second phrase for the same predicate
        val case = createCase("Case", value = "12.0").case
        val stored = kb.conditionManager.getOrCreate(
            greaterThanOrEqualTo(null, glucose(), 11.0).copy(userExpression = "elevated glucose")
        )
        every { parser.parse("raised glucose", any()) } returns
                greaterThanOrEqualTo(null, glucose(), 11.0).copy(userExpression = "raised glucose")

        // When
        val result = rsm.conditionForExpression(case, "raised glucose")

        // Then deduplication preserves the condition while the incoming wording drives the note
        result.condition shouldBeSameInstanceAs stored
        result.expression shouldBe "raised glucose"
        result.condition?.userExpression() shouldBe "elevated glucose"
        result.toExpressionTransformation().message shouldBe
                "Added your reason 'Glucose ≥ 11.0' (you previously called this 'elevated glucose')."
    }

    @Test
    fun `retains incoming wording for a new condition`() {
        // Given
        val case = createCase("Case", value = "12.0").case
        val expression = "raised glucose"
        every { parser.parse(expression, any()) } returns
                greaterThanOrEqualTo(null, glucose(), 11.0).copy(userExpression = expression)

        // When
        val result = rsm.conditionForExpression(case, expression)

        // Then
        result.isFailure shouldBe false
        result.expression shouldBe expression
        result.condition?.userExpression() shouldBe expression
        result.toExpressionTransformation().message shouldBe "Added your reason 'Glucose ≥ 11.0'."
    }

    @Test
    fun `retains incoming wording when parsing fails`() {
        // Given
        val case = createCase("Case").case
        every { parser.parse("unrecognised reason", any()) } returns null

        // When
        val result = rsm.conditionForExpression(case, "unrecognised reason")

        // Then
        result.expression shouldBe "unrecognised reason"
        result.condition shouldBe null
        result.errorMessage shouldBe DOES_NOT_CORRESPOND_TO_A_CONDITION
    }

    @Test
    fun `retains incoming wording when the attribute is absent`() {
        // Given
        val case = createCase("Case").case
        val weight = kb.attributeManager.getOrCreate("Weight")
        every { parser.parse("heavy", any()) } returns greaterThanOrEqualTo(null, weight, 100.0)

        // When
        val result = rsm.conditionForExpression(case, "heavy")

        // Then
        result.expression shouldBe "heavy"
        result.condition shouldBe null
        result.errorMessage shouldBe DOES_NOT_CORRESPOND_TO_A_CONDITION
    }

    @Test
    fun `retains the formal expression when the condition is false`() {
        // Given
        val case = createCase("Case", value = "1.0").case
        val condition = greaterThanOrEqualTo(null, glucose(), 11.0)
        every { parser.parse(condition.asText(), any()) } returns condition

        // When
        val result = rsm.conditionForExpression(case, condition.asText())

        // Then
        result.expression shouldBe condition.asText()
        result.condition shouldBe null
        result.errorMessage shouldBe CONDITION_IS_NOT_TRUE
    }

    @Test
    fun `a false condition reports the incoming phrase rather than the stored phrase`() {
        // Given
        val case = createCase("Case", value = "1.0").case
        val stored = kb.conditionManager.getOrCreate(
            greaterThanOrEqualTo(null, glucose(), 11.0).copy(userExpression = "elevated glucose")
        )
        every { parser.parse("raised glucose", any()) } returns stored

        // When
        val result = rsm.conditionForExpression(case, "raised glucose")

        // Then the validation error is retained without a previous-phrase note
        val expected = INTERPRETED_CONDITION_IS_NOT_TRUE.format("raised glucose", stored.asText())
        result.expression shouldBe "raised glucose"
        result.condition shouldBe null
        result.errorMessage shouldBe expected
        result.toExpressionTransformation().message shouldBe expected
    }
}
