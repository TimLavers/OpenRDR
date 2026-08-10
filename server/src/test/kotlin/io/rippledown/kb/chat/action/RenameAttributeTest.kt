package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RenameAttributeTest {
    private lateinit var ruleService: RuleService
    private lateinit var currentCase: ViewableCase
    private lateinit var modelResponder: ModelResponder

    @BeforeTest
    fun setup() {
        ruleService = mockk()
        currentCase = mockk()
        modelResponder = mockk()
    }

    @Test
    fun `should rename the attribute and report the outcome`() = runTest {
        //Given
        every { ruleService.renameAttribute("C1", "Diabetes advice") } returns
                "Renamed \"C1\" to \"Diabetes advice\"."

        //When
        val response = RenameAttribute("C1", "Diabetes advice").doIt(ruleService, currentCase, modelResponder)

        //Then
        response shouldBe ChatResponse("Renamed \"C1\" to \"Diabetes advice\".")
    }

    @Test
    fun `should rename without involving the model or a rule session`() = runTest {
        //Given
        every { ruleService.renameAttribute(any(), any()) } returns "Renamed \"C1\" to \"Diabetes advice\"."

        //When
        RenameAttribute("C1", "Diabetes advice").doIt(ruleService, currentCase, modelResponder)

        //Then - renaming is not rule building, so no session is started, committed or cancelled
        coVerify(exactly = 0) { modelResponder.response(any<String>()) }
        coVerify(exactly = 0) { ruleService.commitCurrentRuleSession() }
        coVerify(exactly = 0) { ruleService.cancelCurrentRuleSession() }
    }

    @Test
    fun `should report why the attribute cannot be renamed`() = runTest {
        //Given a rename that the KB refuses
        val message = "\"Glucose\" is not a comment or a derived attribute, so it cannot be renamed."
        every { ruleService.renameAttribute("Glucose", "Blood glucose") } throws IllegalStateException(message)

        //When
        val response = RenameAttribute("Glucose", "Blood glucose").doIt(ruleService, currentCase, modelResponder)

        //Then the explanation is passed on to the user
        response shouldBe ChatResponse(message)
    }

    @Test
    fun `should report an invalid new name`() = runTest {
        //Given a new name that the KB refuses
        val message = "An attribute name cannot be blank."
        every { ruleService.renameAttribute("C1", " ") } throws IllegalArgumentException(message)

        //When
        val response = RenameAttribute("C1", " ").doIt(ruleService, currentCase, modelResponder)

        //Then the explanation is passed on to the user
        response shouldBe ChatResponse(message)
    }
}
