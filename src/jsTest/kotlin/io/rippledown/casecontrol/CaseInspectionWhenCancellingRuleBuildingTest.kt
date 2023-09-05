package io.rippledown.casecontrol

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.interpretation.*
import io.rippledown.model.Interpretation
import io.rippledown.model.createCase
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import react.FC
import react.dom.createRootFor
import kotlin.test.Test

class CaseInspectionWhenCancellingRuleBuildingTest {

    @Test
    fun RuleSessionShouldNotBeInProgressIfTheRuleIsCancelled() = runTest {
        val bondiComment = "Go to Bondi now!"
        val diffList = DiffList(
            listOf(
                Addition(bondiComment),
            )
        )
        val caseA = createCase(
            id = 1L,
            name = "Bondi",
        )

        val interp = Interpretation(
            diffList = diffList,
        )
        caseA.interpretation = interp

        val config = config {
            returnInterpretationAfterSavingInterpretation = interp
            expectedSessionStartRequest = SessionStartRequest(caseA.id!!, Addition(bondiComment))
        }

        var inProgress = false
        val fc = FC {
            CaseInspection {
                case = caseA
                scope = this@runTest
                api = Api(mock(config))
                ruleSessionInProgress = { it ->
                    inProgress = it
                }
            }
        }
        with(createRootFor(fc)) {
            //Given a rule building session is in progress
            requireBadgeCount(1)
            selectChangesTab()
            moveMouseOverRow(0)
            clickBuildIconForRow(0)
            inProgress shouldBe true

            //When the rule building session is cancelled
            clickCancelButton()

            //Then
            inProgress shouldBe false
        }
    }
}

