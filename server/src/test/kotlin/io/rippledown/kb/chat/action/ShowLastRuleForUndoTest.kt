package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.rippledown.kb.chat.action.ShowLastRuleForUndo.Companion.confirmRemovalMessage
import io.rippledown.model.rule.UndoRuleDescription
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ShowLastRuleForUndoTest : ActionTestBase() {

    @Test
    fun `shows the last rule description and asks for confirmation when a rule can be removed`() = runTest {
        //Given there is a removable last rule
        val ruleDescription = "Add comment 'Abnormal haemoglobin' if Hb is high"
        every { ruleService.descriptionOfMostRecentRule() } returns
                UndoRuleDescription(ruleDescription, true)

        //When the action runs
        val response = ShowLastRuleForUndo().doIt(ruleService, currentCase, modelResponder)

        //Then the user sees the rule description together with the confirmation prompt
        response.text shouldBe confirmRemovalMessage(ruleDescription)
    }

    @Test
    fun `does not call undo when only previewing`() = runTest {
        //Given a removable last rule
        every { ruleService.descriptionOfMostRecentRule() } returns
                UndoRuleDescription("anything", true)

        //When the action runs
        ShowLastRuleForUndo().doIt(ruleService, currentCase, modelResponder)

        //Then no undo is performed
        coVerify(exactly = 0) { ruleService.undoLastRuleSession() }
    }

    @Test
    fun `tells the user there is nothing to undo when there is no removable rule`() = runTest {
        //Given there is no rule available to undo
        every { ruleService.descriptionOfMostRecentRule() } returns
                UndoRuleDescription("There are no rules to undo.", false)

        //When the action runs
        val response = ShowLastRuleForUndo().doIt(ruleService, currentCase, modelResponder)

        //Then the user is informed and no confirmation prompt is shown
        response.text shouldBe "There are no rules to undo."
    }
}
