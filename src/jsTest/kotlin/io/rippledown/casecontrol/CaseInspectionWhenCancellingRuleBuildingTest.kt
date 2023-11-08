package io.rippledown.casecontrol

import io.kotest.matchers.shouldBe
import io.rippledown.interpretation.clickCancelButton
import io.rippledown.interpretation.requireBadgeCount
import io.rippledown.interpretation.startToBuildRuleForRow
import io.rippledown.model.createCase
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import main.Api
import mocks.config
import mocks.mock
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CaseInspectionWhenCancellingRuleBuildingTest {

    @Test
    fun RuleSessionShouldNotBeInProgressIfTheRuleIsCancelled(): TestResult {
        val bondiComment = "Go to Bondi now!"
        val diffList = DiffList(
            listOf(
                Addition(bondiComment),
            )
        )
        val interp = ViewableInterpretation().apply { this.diffList = diffList }
        val caseA = createCase(
            id = 1L,
            name = "Bondi",
        ).apply { viewableInterpretation = interp }

        val config = config {
            returnInterpretationAfterSavingInterpretation = interp
            expectedSessionStartRequest = SessionStartRequest(caseA.id!!, Addition(bondiComment))
        }

        var inProgress = false
        val fc = FC {
            CaseInspection {
                case = caseA
                scope = MainScope()
                api = Api(mock(config))
                ruleSessionInProgress = { it ->
                    inProgress = it
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given a rule building session is in progress
                requireBadgeCount(1)
                startToBuildRuleForRow(0)
                inProgress shouldBe true

                //When the rule building session is cancelled
                clickCancelButton()

                //Then
                inProgress shouldBe false
            }
        }
    }
}

