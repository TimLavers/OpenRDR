package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.*
import io.rippledown.chat.ReasonTransformation.Companion.TRANSFORMATION_MESSAGE
import io.rippledown.kb.chat.RuleConversation.Companion.MORE_REASONS_QUESTION
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ReasonsAtSessionStartTest {
    private val case = RDRCase(CaseId(1L, "Bondi"))
    private lateinit var ruleService: RuleService
    private lateinit var viewableCase: ViewableCase
    private lateinit var modelResponder: ModelResponder
    private val glucoseHigh = greaterThanOrEqualTo(7, Attribute(1, "Glucose"), 11.0)
    private val ageOver50 = greaterThanOrEqualTo(8, Attribute(2, "Age"), 50.0)

    @BeforeTest
    fun setup() {
        ruleService = mockk()
        viewableCase = mockk()
        modelResponder = mockk()
        every { viewableCase.case } returns case
    }

    @Test
    fun `two good reasons are both added and the status is sent once`() {
        // Given
        every { ruleService.conditionForExpression(case, "glucose high") } returns
                ConditionParsingResult(glucoseHigh, expression = "glucose high")
        every { ruleService.conditionForExpression(case, "age over 50") } returns
                ConditionParsingResult(ageOver50, expression = "age over 50")
        every { ruleService.addConditionToCurrentRuleSession(any()) } returns Unit
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When
        val applied = applyReasons(ruleService, case, listOf("glucose high", "age over 50"))

        // Then
        verify(exactly = 1) { ruleService.addConditionToCurrentRuleSession(glucoseHigh) }
        verify(exactly = 1) { ruleService.addConditionToCurrentRuleSession(ageOver50) }
        verify(exactly = 1) { ruleService.sendCornerstoneStatus() }
        applied.acknowledgementForUser() shouldBe
                "${TRANSFORMATION_MESSAGE.format(glucoseHigh.asText())}\n${TRANSFORMATION_MESSAGE.format(ageOver50.asText())}"
    }

    @Test
    fun `a reason that cannot be parsed is reported and does not stop the others`() {
        // Given
        every { ruleService.conditionForExpression(case, "gibberish") } returns
                ConditionParsingResult(null, "I could not understand 'gibberish'.", "gibberish")
        every { ruleService.conditionForExpression(case, "age over 50") } returns
                ConditionParsingResult(ageOver50, expression = "age over 50")
        every { ruleService.addConditionToCurrentRuleSession(ageOver50) } returns Unit
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When
        val applied = applyReasons(ruleService, case, listOf("gibberish", "age over 50"))

        // Then
        verify(exactly = 1) { ruleService.addConditionToCurrentRuleSession(any()) }
        verify(exactly = 1) { ruleService.sendCornerstoneStatus() }
        applied.acknowledgementForUser() shouldBe
                "I could not understand 'gibberish'.\n${TRANSFORMATION_MESSAGE.format(ageOver50.asText())}"
        applied.summaryForModel() shouldContain "added: ${ageOver50.asText()}"
        applied.summaryForModel() shouldContain "not understood: \"gibberish\" - I could not understand 'gibberish'."
    }

    @Test
    fun `when nothing is added the status is not sent`() {
        // Given
        every { ruleService.conditionForExpression(case, "gibberish") } returns
                ConditionParsingResult(null, "No.", "gibberish")

        // When
        val applied = applyReasons(ruleService, case, listOf("gibberish"))

        // Then
        verify(exactly = 0) { ruleService.addConditionToCurrentRuleSession(any()) }
        verify(exactly = 0) { ruleService.sendCornerstoneStatus() }
        applied.summaryForModel() shouldNotContain "added:"
    }

    @Test
    fun `an empty list touches nothing`() {
        // When
        val applied = applyReasons(ruleService, case, emptyList())

        // Then
        applied.isEmpty() shouldBe true
        applied.acknowledgementForUser() shouldBe ""
        applied.summaryForModel() shouldBe ""
    }

    @Test
    fun `a condition the session refuses is reported as a failure`() {
        // Given a cycle refusal from the session
        every { ruleService.conditionForExpression(case, "Beta is in case") } returns
                ConditionParsingResult(ageOver50, expression = "Beta is in case")
        every { ruleService.addConditionToCurrentRuleSession(ageOver50) } throws
                IllegalArgumentException("This condition would create a cycle.")

        // When
        val applied = applyReasons(ruleService, case, listOf("Beta is in case"))

        // Then
        verify(exactly = 0) { ruleService.sendCornerstoneStatus() }
        applied.acknowledgementForUser() shouldBe "This condition would create a cycle."
    }

    @Test
    fun `a reason typed exactly as the formal condition is still acknowledged as added`() {
        // Given
        val exact = glucoseHigh.copy(userExpression = glucoseHigh.asText())
        every { ruleService.conditionForExpression(case, exact.asText()) } returns
                ConditionParsingResult(exact, expression = exact.asText())
        every { ruleService.addConditionToCurrentRuleSession(exact) } returns Unit
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When
        val applied = applyReasons(ruleService, case, listOf(exact.asText()))

        // Then
        applied.acknowledgementForUser() shouldBe TRANSFORMATION_MESSAGE.format(exact.asText())
    }

    @Test
    fun `without reasons the model is sent the status and its reply stands`() = runTest {
        // Given
        val status = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 2)
        every { ruleService.sendCornerstoneStatus() } returns Unit
        val fromModel = ChatResponse("Why should this comment be given?")
        coEvery { modelResponder.response(status.summary()) } returns fromModel

        // When
        val response = respondAfterSessionStart(ruleService, viewableCase, status, emptyList(), modelResponder)

        // Then
        response shouldBe fromModel
        verify(exactly = 1) { ruleService.sendCornerstoneStatus() }
        verify(exactly = 0) { ruleService.conditionForExpression(any(), any()) }
    }

    @Test
    fun `with reasons the model is told the outcome and the user gets the server's acknowledgement`() = runTest {
        // Given
        val statusAtStart = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 2)
        val statusAfter = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 1)
        every { ruleService.sendCornerstoneStatus() } returns Unit
        every { ruleService.conditionForExpression(case, "glucose high") } returns
                ConditionParsingResult(glucoseHigh, expression = "glucose high")
        every { ruleService.addConditionToCurrentRuleSession(glucoseHigh) } returns Unit
        every { ruleService.cornerstoneStatus() } returns statusAfter
        val fromModel = ChatResponse("Do you want any more reasons?", suggestions = listOf("Age ≥ 50"))
        coEvery { modelResponder.response(any<String>()) } returns fromModel

        // When
        val response =
            respondAfterSessionStart(ruleService, viewableCase, statusAtStart, listOf("glucose high"), modelResponder)

        // Then the model saw the status as it stands after the condition, and the outcome
        coVerify {
            modelResponder.response(match {
                it.startsWith(statusAfter.summary()) && it.contains("added: ${glucoseHigh.asText()}")
            })
        }
        response.text shouldBe "${TRANSFORMATION_MESSAGE.format(glucoseHigh.asText())}\n\n$MORE_REASONS_QUESTION"
        response.suggestions shouldBe listOf("Age ≥ 50")
    }
}
