package io.rippledown.casecontrol

import Api
import io.rippledown.interpretation.*
import io.rippledown.model.createCase
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.MainScope
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CaseInspectionUpdateTest {

    @Test
    fun shouldUpdateDiffViewAfterARuleIsBuilt() {
        val bondiComment = "Go to Bondi now!"
        val diffList = DiffList(
            listOf(
                Addition(bondiComment)
            )
        )

        val interpBeforeRule = ViewableInterpretation().apply { this.diffList = diffList }
        val interpAfterRule = ViewableInterpretation()

        val caseA = createCase(
            id = 1L,
            name = "Manly",
        ).apply { viewableInterpretation = interpBeforeRule }

        val config = config {
            returnInterpretationAfterSavingInterpretation = interpBeforeRule
            returnInterpretationAfterBuildingRule = interpAfterRule
            expectedSessionStartRequest = SessionStartRequest(caseA.id!!, Addition(bondiComment))
        }

        val fc = FC {
            CaseInspection {
                case = caseA
                scope = MainScope()
                api = Api(mock(config))
                ruleSessionInProgress = { _ -> }
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                //Given
                requireBadgeCount(1)
                selectChangesTab()
                requireNumberOfRows(1)
                clickBuildIconForRow(0)

                //When a rule is built
                clickDoneButton()
                waitForEvents()

                //Then
                requireNoBadge()
                waitForEvents()
                requireNoBadge()
            }
        }
    }
}

